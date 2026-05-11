package com.futsite.service;

import com.futsite.dto.request.CreateMatchRequest;
import com.futsite.dto.request.RecordGoalRequest;
import com.futsite.dto.response.GoalResponse;
import com.futsite.dto.response.MatchResponse;
import com.futsite.dto.response.MatchTimerResponse;
import com.futsite.exception.BadRequestException;
import com.futsite.exception.ResourceNotFoundException;
import com.futsite.model.entity.*;
import com.futsite.model.enums.ChampionshipStatus;
import com.futsite.model.enums.MatchStatus;
import com.futsite.repository.postgres.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final GoalRepository goalRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final ChampionshipService championshipService;
    private final RedisService redisService;
    private final TeamService teamService;
    private final StatisticsService statisticsService;

    @Transactional
    public MatchResponse createMatch(Long championshipId, CreateMatchRequest request, String managerUsername) {
        Championship championship = championshipService.getChampionshipEntity(championshipId);

        if (!championship.getManager().getUsername().equals(managerUsername)) {
            throw new BadRequestException("Only the championship manager can create matches");
        }

        if (championship.getStatus() != ChampionshipStatus.STARTED) {
            throw new BadRequestException("Championship must be started before creating matches");
        }

        Team homeTeam = teamRepository.findById(request.getHomeTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Home team not found"));
        Team awayTeam = teamRepository.findById(request.getAwayTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Away team not found"));

        Match match = Match.builder()
                .championship(championship)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .round(request.getRound() != null ? request.getRound() : 1)
                .bracketPosition(request.getBracketPosition())
                .scheduledAt(request.getScheduledAt())
                .status(MatchStatus.SCHEDULED)
                .durationSeconds(request.getDurationSeconds())
                .goalLimit(request.getGoalLimit() != null ? request.getGoalLimit() : 0)
                .homeScore(0)
                .awayScore(0)
                .build();

        match = matchRepository.save(match);
        return toResponse(match);
    }

    @Transactional
    public MatchResponse startMatch(Long matchId, String managerUsername) {
        Match match = getMatchEntity(matchId);
        validateMatchManager(match, managerUsername);

        if (match.getStatus() != MatchStatus.SCHEDULED) {
            throw new BadRequestException("Match can only be started from SCHEDULED status");
        }

        match.setStatus(MatchStatus.LIVE);
        match = matchRepository.save(match);

        // Save start timestamp in Redis (for tracking purposes only)
        redisService.setMatchStartTime(matchId, System.currentTimeMillis());

        return toResponse(match);
    }

    @Transactional
    public MatchResponse pauseMatch(Long matchId, String managerUsername) {
        Match match = getMatchEntity(matchId);
        validateMatchManager(match, managerUsername);

        if (match.getStatus() != MatchStatus.LIVE) {
            throw new BadRequestException("Match is not live");
        }

        match.setStatus(MatchStatus.PAUSED);
        match = matchRepository.save(match);
        
        // Save pause timestamp in Redis for tracking
        redisService.setMatchPauseTime(matchId, System.currentTimeMillis());

        return toResponse(match);
    }

    @Transactional
    public MatchResponse resumeMatch(Long matchId, String managerUsername) {
        Match match = getMatchEntity(matchId);
        validateMatchManager(match, managerUsername);

        if (match.getStatus() != MatchStatus.PAUSED) {
            throw new BadRequestException("Match is not paused");
        }

        match.setStatus(MatchStatus.LIVE);
        match = matchRepository.save(match);
        
        // Clear pause time to resume normal flow
        redisService.clearMatchPauseTime(matchId);

        return toResponse(match);
    }

    // NOTE: Timer adjustments are now handled by the frontend incrementally
    // Backend no longer manages continuous timer, only validates and stores snapshots

    @Transactional
    public MatchResponse finishMatch(Long matchId, String managerUsername) {
        Match match = getMatchEntity(matchId);
        validateMatchManager(match, managerUsername);

        if (match.getStatus() != MatchStatus.LIVE && match.getStatus() != MatchStatus.PAUSED) {
            throw new BadRequestException("Match must be live or paused to finish");
        }

        match.setStatus(MatchStatus.FINISHED);
        match = matchRepository.save(match);

        // Save finish timestamp in Redis
        redisService.setMatchFinishTime(matchId, System.currentTimeMillis());

        // Invalidate standings cache
        redisService.invalidateStandingsCache(match.getChampionship().getId());

        // Generate statistics in MongoDB (non-blocking: don't fail the match finish if Mongo is down)
        try {
            statisticsService.generateMatchStatistics(match);
            statisticsService.updateChampionshipStatistics(match.getChampionship().getId());
        } catch (Exception e) {
            log.error("Failed to generate statistics for match {}: {}", matchId, e.getMessage());
        }

        return toResponse(match);
    }

    @Transactional
    public GoalResponse recordGoal(Long matchId, RecordGoalRequest request, String managerUsername) {
        Match match = getMatchEntity(matchId);
        validateMatchManager(match, managerUsername);

        if (match.getStatus() != MatchStatus.LIVE && match.getStatus() != MatchStatus.PAUSED) {
            throw new BadRequestException("Can only record goals during a live or paused match");
        }

        // Check goal limit before recording
        if (match.getGoalLimit() != null && match.getGoalLimit() > 0) {
            int totalGoals = match.getHomeScore() + match.getAwayScore();
            if (totalGoals >= match.getGoalLimit()) {
                throw new BadRequestException("Goal limit (" + match.getGoalLimit() + ") already reached");
            }
        }

        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        User scorer = null;
        if (!request.getOwnGoal() && request.getScorerId() != null) {
            scorer = userRepository.findById(request.getScorerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Scorer not found"));
        }

        // Calculate elapsed time from system clock (Redis-backed match timestamps)
        long elapsedSeconds = redisService.getMatchElapsedSeconds(matchId);
        int minute = (int) (elapsedSeconds / 60);
        int second = (int) (elapsedSeconds % 60);

        Goal goal = Goal.builder()
                .match(match)
                .team(team)
                .scorer(scorer)
                .ownGoal(request.getOwnGoal())
                .minute(minute)
                .second(second)
                .build();

        goal = goalRepository.save(goal);

        // Update score
        if (request.getOwnGoal()) {
            // Own goal: point goes to the OTHER team
            if (team.getId().equals(match.getHomeTeam().getId())) {
                match.setAwayScore(match.getAwayScore() + 1);
            } else {
                match.setHomeScore(match.getHomeScore() + 1);
            }
        } else {
            if (team.getId().equals(match.getHomeTeam().getId())) {
                match.setHomeScore(match.getHomeScore() + 1);
            } else {
                match.setAwayScore(match.getAwayScore() + 1);
            }
        }
        matchRepository.save(match);

        // Auto-finish if goal limit is reached
        if (match.getGoalLimit() != null && match.getGoalLimit() > 0) {
            int totalGoals = match.getHomeScore() + match.getAwayScore();
            if (totalGoals >= match.getGoalLimit()) {
                match.setStatus(MatchStatus.FINISHED);
                matchRepository.save(match);
                redisService.setMatchFinishTime(matchId, System.currentTimeMillis());
                redisService.invalidateStandingsCache(match.getChampionship().getId());
                try {
                    statisticsService.generateMatchStatistics(match);
                    statisticsService.updateChampionshipStatistics(match.getChampionship().getId());
                } catch (Exception e) {
                    log.error("Failed to generate statistics for match {}: {}", matchId, e.getMessage());
                }
            }
        }

        return GoalResponse.builder()
                .id(goal.getId())
                .teamName(team.getName())
                .teamId(team.getId())
                .scorerName(scorer != null ? scorer.getFullName() : "Gol Contra")
                .scorerId(scorer != null ? scorer.getId() : null)
                .ownGoal(goal.getOwnGoal())
                .minute(goal.getMinute())
                .second(goal.getSecond())
                .build();
    }

    // NOTE: Timer state is no longer fetched from backend - frontend maintains incremental timer
    // This endpoint is kept for potential analytics/debugging purposes but is not used during match play

    public MatchResponse getMatch(Long matchId) {
        return toResponse(getMatchEntity(matchId));
    }

    public List<MatchResponse> getMatchesByChampionship(Long championshipId) {
        return matchRepository.findByChampionshipId(championshipId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<MatchResponse> getMatchesByChampionshipAndRound(Long championshipId, Integer round) {
        return matchRepository.findByChampionshipIdAndRound(championshipId, round).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    // ====== INTERNAL ======

    private Match getMatchEntity(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
    }

    private void validateMatchManager(Match match, String username) {
        if (!match.getChampionship().getManager().getUsername().equals(username)) {
            throw new BadRequestException("Only the championship manager can control matches");
        }
    }

    private MatchResponse toResponse(Match m) {
        List<GoalResponse> goalResponses = m.getGoals() != null
                ? m.getGoals().stream().map(g -> GoalResponse.builder()
                    .id(g.getId())
                    .teamName(g.getTeam().getName())
                    .teamId(g.getTeam().getId())
                    .scorerName(g.getScorer() != null ? g.getScorer().getFullName() : "Gol Contra")
                    .scorerId(g.getScorer() != null ? g.getScorer().getId() : null)
                    .ownGoal(g.getOwnGoal())
                    .minute(g.getMinute())
                    .second(g.getSecond())
                    .build()).collect(Collectors.toList())
                : List.of();

        return MatchResponse.builder()
                .id(m.getId())
                .championshipId(m.getChampionship().getId())
                .championshipName(m.getChampionship().getName())
                .homeTeam(teamService.toTeamResponse(m.getHomeTeam()))
                .awayTeam(teamService.toTeamResponse(m.getAwayTeam()))
                .round(m.getRound())
                .bracketPosition(m.getBracketPosition())
                .scheduledAt(m.getScheduledAt())
                .status(m.getStatus())
                .durationSeconds(m.getDurationSeconds())
                .goalLimit(m.getGoalLimit())
                .homeScore(m.getHomeScore())
                .awayScore(m.getAwayScore())
                .goals(goalResponses)
                .build();
    }
}
