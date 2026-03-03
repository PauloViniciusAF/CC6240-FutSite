package com.futsite.service;

import com.futsite.dto.request.CreateChampionshipRequest;
import com.futsite.dto.request.SetBracketRequest;
import com.futsite.dto.response.ChampionshipResponse;
import com.futsite.exception.BadRequestException;
import com.futsite.exception.ResourceNotFoundException;
import com.futsite.model.entity.*;
import com.futsite.model.enums.*;
import com.futsite.repository.postgres.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChampionshipService {

    private final ChampionshipRepository championshipRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final RedisService redisService;

    @Transactional
    public ChampionshipResponse createChampionship(CreateChampionshipRequest request, String managerUsername) {
        User manager = userRepository.findByUsername(managerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (manager.getRole() != UserRole.MANAGER) {
            throw new BadRequestException("Only managers can create championships");
        }

        Championship championship = Championship.builder()
                .name(request.getName())
                .manager(manager)
                .format(request.getFormat())
                .status(ChampionshipStatus.DRAFT)
                .sports(request.getSports() != null ? request.getSports() : new ArrayList<>())
                .teams(new ArrayList<>())
                .matches(new ArrayList<>())
                .build();

        // Round Robin specific
        if (request.getFormat() == ChampionshipFormat.ROUND_ROBIN) {
            championship.setHomeAndAway(request.getHomeAndAway() != null ? request.getHomeAndAway() : false);
            championship.setWinPoints(request.getWinPoints() != null ? request.getWinPoints() : 3);
            championship.setDrawPoints(request.getDrawPoints() != null ? request.getDrawPoints() : 1);
            championship.setLossPoints(request.getLossPoints() != null ? request.getLossPoints() : 0);
        }

        // Knockout specific
        if (request.getFormat() == ChampionshipFormat.KNOCKOUT) {
            championship.setTwoLegged(request.getTwoLegged() != null ? request.getTwoLegged() : false);
            Integer teamCount = request.getKnockoutTeamCount();
            if (teamCount != null && !Set.of(2, 4, 8, 16).contains(teamCount)) {
                throw new BadRequestException("Knockout team count must be 2, 4, 8, or 16");
            }
            championship.setKnockoutTeamCount(teamCount);
        }

        championship = championshipRepository.save(championship);

        // Add teams if provided
        if (request.getTeamIds() != null && !request.getTeamIds().isEmpty()) {
            for (Long teamId : request.getTeamIds()) {
                addTeamInternal(championship, teamId);
            }
            championship = championshipRepository.save(championship);
        }

        return toResponse(championship);
    }

    @Transactional
    public ChampionshipResponse addTeam(Long championshipId, Long teamId, String managerUsername) {
        Championship championship = getChampionshipEntity(championshipId);
        validateManager(championship, managerUsername);

        if (championship.getStatus() != ChampionshipStatus.DRAFT) {
            throw new BadRequestException("Cannot add teams after championship has started");
        }

        addTeamInternal(championship, teamId);
        championship = championshipRepository.save(championship);

        return toResponse(championship);
    }

    @Transactional
    public ChampionshipResponse removeTeam(Long championshipId, Long teamId, String managerUsername) {
        Championship championship = getChampionshipEntity(championshipId);
        validateManager(championship, managerUsername);

        if (championship.getStatus() != ChampionshipStatus.DRAFT) {
            throw new BadRequestException("Cannot remove teams after championship has started");
        }

        championship.getTeams().removeIf(t -> t.getId().equals(teamId));
        championship = championshipRepository.save(championship);
        return toResponse(championship);
    }

    @Transactional
    public ChampionshipResponse startChampionship(Long championshipId, String managerUsername) {
        Championship championship = getChampionshipEntity(championshipId);
        validateManager(championship, managerUsername);

        if (championship.getStatus() != ChampionshipStatus.DRAFT) {
            throw new BadRequestException("Championship already started or finished");
        }

        if (championship.getTeams().size() < 2) {
            throw new BadRequestException("Championship must have at least 2 teams to start");
        }

        if (championship.getFormat() == ChampionshipFormat.ROUND_ROBIN) {
            generateRoundRobinMatches(championship);
        }
        // For knockout: matches are generated via bracket seeding

        championship.setStatus(ChampionshipStatus.STARTED);
        championship = championshipRepository.save(championship);

        // Notify all athletes
        notifyAllAthletes(championship);

        return toResponse(championship);
    }

    @Transactional
    public ChampionshipResponse setBracket(Long championshipId, SetBracketRequest request, String managerUsername) {
        Championship championship = getChampionshipEntity(championshipId);
        validateManager(championship, managerUsername);

        if (championship.getFormat() != ChampionshipFormat.KNOCKOUT) {
            throw new BadRequestException("Bracket is only for knockout format");
        }

        List<Team> teams = championship.getTeams();
        int teamCount = championship.getKnockoutTeamCount() != null
                ? championship.getKnockoutTeamCount() : teams.size();

        if (teams.size() != teamCount) {
            throw new BadRequestException("Need exactly " + teamCount + " teams for bracket, have " + teams.size());
        }

        // Clear existing matches
        matchRepository.deleteAll(matchRepository.findByChampionshipId(championshipId));
        championship.getMatches().clear();

        List<Team> orderedTeams;
        if (request.isRandomDraw()) {
            orderedTeams = new ArrayList<>(teams);
            Collections.shuffle(orderedTeams);
        } else {
            orderedTeams = new ArrayList<>();
            for (int i = 0; i < teamCount; i++) {
                Long tid = request.getBracketSeeding().get(i);
                Team t = teams.stream().filter(team -> team.getId().equals(tid)).findFirst()
                        .orElseThrow(() -> new BadRequestException("Team not found in championship"));
                orderedTeams.add(t);
            }
        }

        // Generate first round matches
        int firstRound = teamCount / 2; // number of matches in first round
        int roundNumber = teamCount; // e.g., 16 for oitavas

        for (int i = 0; i < orderedTeams.size(); i += 2) {
            Match match = Match.builder()
                    .championship(championship)
                    .homeTeam(orderedTeams.get(i))
                    .awayTeam(orderedTeams.get(i + 1))
                    .round(roundNumber)
                    .bracketPosition(i / 2)
                    .status(MatchStatus.SCHEDULED)
                    .durationSeconds(0)
                    .build();
            matchRepository.save(match);
            championship.getMatches().add(match);
        }

        championship = championshipRepository.save(championship);
        return toResponse(championship);
    }

    public ChampionshipResponse getChampionship(Long championshipId) {
        return toResponse(getChampionshipEntity(championshipId));
    }

    public List<ChampionshipResponse> getAllChampionships() {
        return championshipRepository.findAll().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<ChampionshipResponse> getChampionshipsByManager(String username) {
        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return championshipRepository.findByManagerId(manager.getId()).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void deleteChampionship(Long championshipId, String managerUsername) {
        Championship championship = getChampionshipEntity(championshipId);
        validateManager(championship, managerUsername);
        championshipRepository.delete(championship);
    }

    // ====== ROUND ROBIN MATCH GENERATION ======

    private void generateRoundRobinMatches(Championship championship) {
        List<Team> teams = new ArrayList<>(championship.getTeams());
        Collections.shuffle(teams); // random order

        int n = teams.size();
        boolean addBye = n % 2 != 0;
        if (addBye) {
            teams.add(null); // bye team
            n++;
        }

        int totalRounds = n - 1;
        int matchesPerRound = n / 2;

        // Generate turno
        for (int round = 0; round < totalRounds; round++) {
            for (int match = 0; match < matchesPerRound; match++) {
                int home = (round + match) % (n - 1);
                int away = (n - 1 - match + round) % (n - 1);
                if (match == 0) away = n - 1;

                Team homeTeam = teams.get(home);
                Team awayTeam = teams.get(away);

                if (homeTeam == null || awayTeam == null) continue; // bye

                Match m = Match.builder()
                        .championship(championship)
                        .homeTeam(homeTeam)
                        .awayTeam(awayTeam)
                        .round(round + 1)
                        .status(MatchStatus.SCHEDULED)
                        .durationSeconds(0)
                        .build();
                matchRepository.save(m);
                championship.getMatches().add(m);
            }
        }

        // Generate returno if home and away
        if (Boolean.TRUE.equals(championship.getHomeAndAway())) {
            List<Match> turnoMatches = new ArrayList<>(championship.getMatches());
            int returnoRoundOffset = totalRounds;
            for (Match turnoMatch : turnoMatches) {
                Match returno = Match.builder()
                        .championship(championship)
                        .homeTeam(turnoMatch.getAwayTeam()) // swap home/away
                        .awayTeam(turnoMatch.getHomeTeam())
                        .round(turnoMatch.getRound() + returnoRoundOffset)
                        .status(MatchStatus.SCHEDULED)
                        .durationSeconds(0)
                        .build();
                matchRepository.save(returno);
                championship.getMatches().add(returno);
            }
        }
    }

    // ====== INTERNAL ======

    private void addTeamInternal(Championship championship, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        boolean alreadyAdded = championship.getTeams().stream()
                .anyMatch(t -> t.getId().equals(teamId));
        if (alreadyAdded) {
            throw new BadRequestException("Team is already in this championship");
        }

        championship.getTeams().add(team);

        // Notify captain
        emailService.notifyCaptainTeamRegistered(
                team.getCaptain().getEmail(),
                team.getCaptain().getFullName(),
                team.getName(),
                championship.getName());
    }

    private void notifyAllAthletes(Championship championship) {
        for (Team team : championship.getTeams()) {
            for (TeamMember member : team.getMembers()) {
                emailService.notifyChampionshipStarted(
                        member.getAthlete().getEmail(),
                        member.getAthlete().getFullName(),
                        championship.getName());
            }
        }
    }

    Championship getChampionshipEntity(Long id) {
        return championshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Championship not found"));
    }

    private void validateManager(Championship championship, String username) {
        if (!championship.getManager().getUsername().equals(username)) {
            throw new BadRequestException("Only the championship manager can perform this action");
        }
    }

    private ChampionshipResponse toResponse(Championship c) {
        return ChampionshipResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .manager(AuthService.toUserResponse(c.getManager()))
                .format(c.getFormat())
                .status(c.getStatus())
                .sports(c.getSports())
                .teams(c.getTeams().stream().map(TeamService::toTeamResponse).collect(Collectors.toList()))
                .homeAndAway(c.getHomeAndAway())
                .winPoints(c.getWinPoints())
                .drawPoints(c.getDrawPoints())
                .lossPoints(c.getLossPoints())
                .twoLegged(c.getTwoLegged())
                .knockoutTeamCount(c.getKnockoutTeamCount())
                .build();
    }
}
