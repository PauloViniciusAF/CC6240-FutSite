package com.futsite.service;

import com.futsite.model.document.ChampionshipStatistics;
import com.futsite.model.document.ChampionshipStatistics.TeamStanding;
import com.futsite.model.document.ChampionshipStatistics.TopScorer;
import com.futsite.model.document.MatchStatistics;
import com.futsite.model.document.MatchStatistics.GoalDetail;
import com.futsite.model.entity.*;
import com.futsite.model.enums.MatchStatus;
import com.futsite.repository.mongo.ChampionshipStatisticsRepository;
import com.futsite.repository.mongo.MatchStatisticsRepository;
import com.futsite.repository.postgres.ChampionshipRepository;
import com.futsite.repository.postgres.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Statistics service — demonstrates MongoDB (DB1) usage.
 * Stores flexible, denormalized statistics documents that vary per sport/match.
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final MatchStatisticsRepository matchStatsRepo;
    private final ChampionshipStatisticsRepository champStatsRepo;
    private final ChampionshipRepository championshipRepository;
    private final MatchRepository matchRepository;
    private final RedisService redisService;

    public void generateMatchStatistics(Match match) {
        List<GoalDetail> goalDetails = match.getGoals().stream()
                .map(g -> GoalDetail.builder()
                        .scorerName(g.getScorer() != null ? g.getScorer().getFullName() : "Gol Contra")
                        .scorerId(g.getScorer() != null ? g.getScorer().getId() : null)
                        .teamName(g.getTeam().getName())
                        .ownGoal(g.getOwnGoal())
                        .minute(g.getMinute())
                        .second(g.getSecond())
                        .build())
                .collect(Collectors.toList());

        Map<String, Object> homeStats = new HashMap<>();
        homeStats.put("goals", match.getHomeScore());
        homeStats.put("ownGoals", match.getGoals().stream()
                .filter(g -> g.getOwnGoal() && g.getTeam().getId().equals(match.getHomeTeam().getId()))
                .count());

        Map<String, Object> awayStats = new HashMap<>();
        awayStats.put("goals", match.getAwayScore());
        awayStats.put("ownGoals", match.getGoals().stream()
                .filter(g -> g.getOwnGoal() && g.getTeam().getId().equals(match.getAwayTeam().getId()))
                .count());

        MatchStatistics stats = MatchStatistics.builder()
                .matchId(match.getId())
                .championshipId(match.getChampionship().getId())
                .homeTeamName(match.getHomeTeam().getName())
                .awayTeamName(match.getAwayTeam().getName())
                .homeTeamId(match.getHomeTeam().getId())
                .awayTeamId(match.getAwayTeam().getId())
                .homeScore(match.getHomeScore())
                .awayScore(match.getAwayScore())
                .durationSeconds(match.getDurationSeconds())
                .goals(goalDetails)
                .homeStats(homeStats)
                .awayStats(awayStats)
                .playedAt(LocalDateTime.now())
                .build();

        // Upsert
        matchStatsRepo.findByMatchId(match.getId())
                .ifPresent(existing -> stats.setId(existing.getId()));
        matchStatsRepo.save(stats);
    }

    public void updateChampionshipStatistics(Long championshipId) {
        Championship championship = championshipRepository.findById(championshipId)
                .orElseThrow();

        List<Match> finishedMatches = matchRepository
                .findByChampionshipIdAndStatus(championshipId, MatchStatus.FINISHED);

        // Build standings (for round robin)
        Map<Long, TeamStanding> standingsMap = new HashMap<>();
        for (Team team : championship.getTeams()) {
            standingsMap.put(team.getId(), TeamStanding.builder()
                    .teamId(team.getId())
                    .teamName(team.getName())
                    .played(0).wins(0).draws(0).losses(0)
                    .goalsFor(0).goalsAgainst(0).goalDifference(0).points(0)
                    .build());
        }

        int winPts = championship.getWinPoints() != null ? championship.getWinPoints() : 3;
        int drawPts = championship.getDrawPoints() != null ? championship.getDrawPoints() : 1;
        int lossPts = championship.getLossPoints() != null ? championship.getLossPoints() : 0;

        // Top scorers map: playerId -> count
        Map<Long, Integer> scorerMap = new HashMap<>();
        Map<Long, String> scorerNames = new HashMap<>();
        Map<Long, String> scorerTeams = new HashMap<>();

        for (Match match : finishedMatches) {
            Long homeId = match.getHomeTeam().getId();
            Long awayId = match.getAwayTeam().getId();
            int homeScore = match.getHomeScore();
            int awayScore = match.getAwayScore();

            TeamStanding homeSt = standingsMap.get(homeId);
            TeamStanding awaySt = standingsMap.get(awayId);

            if (homeSt != null) {
                homeSt.setPlayed(homeSt.getPlayed() + 1);
                homeSt.setGoalsFor(homeSt.getGoalsFor() + homeScore);
                homeSt.setGoalsAgainst(homeSt.getGoalsAgainst() + awayScore);
            }
            if (awaySt != null) {
                awaySt.setPlayed(awaySt.getPlayed() + 1);
                awaySt.setGoalsFor(awaySt.getGoalsFor() + awayScore);
                awaySt.setGoalsAgainst(awaySt.getGoalsAgainst() + homeScore);
            }

            if (homeScore > awayScore) {
                if (homeSt != null) { homeSt.setWins(homeSt.getWins() + 1); homeSt.setPoints(homeSt.getPoints() + winPts); }
                if (awaySt != null) { awaySt.setLosses(awaySt.getLosses() + 1); awaySt.setPoints(awaySt.getPoints() + lossPts); }
            } else if (awayScore > homeScore) {
                if (awaySt != null) { awaySt.setWins(awaySt.getWins() + 1); awaySt.setPoints(awaySt.getPoints() + winPts); }
                if (homeSt != null) { homeSt.setLosses(homeSt.getLosses() + 1); homeSt.setPoints(homeSt.getPoints() + lossPts); }
            } else {
                if (homeSt != null) { homeSt.setDraws(homeSt.getDraws() + 1); homeSt.setPoints(homeSt.getPoints() + drawPts); }
                if (awaySt != null) { awaySt.setDraws(awaySt.getDraws() + 1); awaySt.setPoints(awaySt.getPoints() + drawPts); }
            }

            // Goals for top scorers
            for (Goal goal : match.getGoals()) {
                if (!goal.getOwnGoal() && goal.getScorer() != null) {
                    scorerMap.merge(goal.getScorer().getId(), 1, Integer::sum);
                    scorerNames.put(goal.getScorer().getId(), goal.getScorer().getFullName());
                    scorerTeams.put(goal.getScorer().getId(), goal.getTeam().getName());
                }
            }
        }

        // Compute goal difference
        standingsMap.values().forEach(s ->
                s.setGoalDifference(s.getGoalsFor() - s.getGoalsAgainst()));

        // Sort standings
        List<TeamStanding> standings = standingsMap.values().stream()
                .sorted(Comparator.comparingInt(TeamStanding::getPoints).reversed()
                        .thenComparingInt(TeamStanding::getGoalDifference).reversed()
                        .thenComparingInt(TeamStanding::getGoalsFor).reversed())
                .collect(Collectors.toList());

        // Top scorers
        List<TopScorer> topScorers = scorerMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(20)
                .map(e -> TopScorer.builder()
                        .playerId(e.getKey())
                        .playerName(scorerNames.get(e.getKey()))
                        .teamName(scorerTeams.get(e.getKey()))
                        .goals(e.getValue())
                        .build())
                .collect(Collectors.toList());

        ChampionshipStatistics champStats = champStatsRepo.findByChampionshipId(championshipId)
                .orElse(ChampionshipStatistics.builder()
                        .championshipId(championshipId)
                        .championshipName(championship.getName())
                        .build());

        champStats.setStandings(standings);
        champStats.setTopScorers(topScorers);
        champStatsRepo.save(champStats);

        // Cache in Redis
        redisService.cacheStandings(championshipId, standings);
    }

    public MatchStatistics getMatchStatistics(Long matchId) {
        return matchStatsRepo.findByMatchId(matchId).orElse(null);
    }

    public ChampionshipStatistics getChampionshipStatistics(Long championshipId) {
        return champStatsRepo.findByChampionshipId(championshipId).orElse(null);
    }

    public List<MatchStatistics> getMatchStatisticsByChampionship(Long championshipId) {
        return matchStatsRepo.findByChampionshipId(championshipId);
    }
}
