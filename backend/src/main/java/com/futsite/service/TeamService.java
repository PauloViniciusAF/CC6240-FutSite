package com.futsite.service;

import com.futsite.dto.request.AddTeamMemberRequest;
import com.futsite.dto.request.CreateTeamRequest;
import com.futsite.dto.response.TeamResponse;
import com.futsite.dto.response.UserResponse;
import com.futsite.exception.BadRequestException;
import com.futsite.exception.ResourceNotFoundException;
import com.futsite.model.entity.Team;
import com.futsite.model.entity.TeamMember;
import com.futsite.model.entity.User;
import com.futsite.model.enums.UserRole;
import com.futsite.repository.postgres.TeamMemberRepository;
import com.futsite.repository.postgres.TeamRepository;
import com.futsite.repository.postgres.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request, String captainUsername) {
        User captain = userRepository.findByUsername(captainUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (captain.getRole() != UserRole.ATHLETE) {
            throw new BadRequestException("Only athletes can create teams");
        }

        Team team = Team.builder()
                .name(request.getName())
                .sport(request.getSport())
                .captain(captain)
                .members(new ArrayList<>())
                .build();

        team = teamRepository.save(team);

        // Add captain as a member (jersey number 0 if not specified)
        TeamMember captainMember = TeamMember.builder()
                .team(team)
                .athlete(captain)
                .jerseyNumber(0)
                .build();
        teamMemberRepository.save(captainMember);
        team.getMembers().add(captainMember);

        // Add additional members if provided
        if (request.getMembers() != null) {
            for (CreateTeamRequest.TeamMemberEntry entry : request.getMembers()) {
                addMemberInternal(team, entry.getAthleteId(), entry.getJerseyNumber());
            }
        }

        return toTeamResponse(team);
    }

    @Transactional
    public TeamResponse addMember(Long teamId, AddTeamMemberRequest request, String username) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        // Only the captain can add members
        if (!team.getCaptain().getUsername().equals(username)) {
            throw new BadRequestException("Only the team captain can add members");
        }

        addMemberInternal(team, request.getAthleteId(), request.getJerseyNumber());

        return toTeamResponse(teamRepository.findById(teamId).get());
    }

    @Transactional
    public void removeMember(Long teamId, Long athleteId, String username) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        if (!team.getCaptain().getUsername().equals(username)) {
            throw new BadRequestException("Only the team captain can remove members");
        }

        if (team.getCaptain().getId().equals(athleteId)) {
            throw new BadRequestException("Cannot remove the captain from the team");
        }

        TeamMember member = teamMemberRepository.findByTeamIdAndAthleteId(teamId, athleteId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in team"));

        teamMemberRepository.delete(member);
    }

    public TeamResponse getTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        return toTeamResponse(team);
    }

    public List<TeamResponse> getTeamsByCaptain(String username) {
        User captain = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return teamRepository.findByCaptainId(captain.getId())
                .stream().map(this::toTeamResponse).collect(Collectors.toList());
    }

    public List<TeamResponse> getTeamsByAthlete(String username) {
        User athlete = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return teamRepository.findByAthleteId(athlete.getId())
                .stream().map(this::toTeamResponse).collect(Collectors.toList());
    }

    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll()
                .stream().map(this::toTeamResponse).collect(Collectors.toList());
    }

    public List<TeamResponse> getTeamsBySport(String sport) {
        return teamRepository.findBySport(sport)
                .stream().map(this::toTeamResponse).collect(Collectors.toList());
    }

    // ====== INTERNAL ======

    private void addMemberInternal(Team team, Long athleteId, Integer jerseyNumber) {
        User athlete = userRepository.findById(athleteId)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete not found"));

        if (athlete.getRole() != UserRole.ATHLETE) {
            throw new BadRequestException("User is not an athlete");
        }

        if (teamMemberRepository.existsByTeamIdAndAthleteId(team.getId(), athleteId)) {
            throw new BadRequestException("Athlete is already in this team");
        }

        if (teamMemberRepository.existsByTeamIdAndJerseyNumber(team.getId(), jerseyNumber)) {
            throw new BadRequestException("Jersey number " + jerseyNumber + " is already taken");
        }

        TeamMember member = TeamMember.builder()
                .team(team)
                .athlete(athlete)
                .jerseyNumber(jerseyNumber)
                .build();
        teamMemberRepository.save(member);

        // Notify athlete by email
        emailService.notifyAthleteAddedToTeam(athlete.getEmail(), athlete.getFullName(), team.getName());
    }

    public static TeamResponse toTeamResponse(Team team) {
        List<TeamResponse.TeamMemberResponse> memberResponses = team.getMembers() != null
                ? team.getMembers().stream().map(m -> TeamResponse.TeamMemberResponse.builder()
                    .id(m.getId())
                    .athlete(AuthService.toUserResponse(m.getAthlete()))
                    .jerseyNumber(m.getJerseyNumber())
                    .build()).collect(Collectors.toList())
                : new ArrayList<>();

        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .sport(team.getSport())
                .captain(AuthService.toUserResponse(team.getCaptain()))
                .members(memberResponses)
                .build();
    }
}
