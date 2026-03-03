package com.futsite.controller;

import com.futsite.dto.request.AddTeamMemberRequest;
import com.futsite.dto.request.CreateTeamRequest;
import com.futsite.dto.response.TeamResponse;
import com.futsite.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(
            @Valid @RequestBody CreateTeamRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(teamService.createTeam(request, userDetails.getUsername()));
    }

    @PostMapping("/{teamId}/members")
    public ResponseEntity<TeamResponse> addMember(
            @PathVariable Long teamId,
            @Valid @RequestBody AddTeamMemberRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(teamService.addMember(teamId, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{teamId}/members/{athleteId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long teamId,
            @PathVariable Long athleteId,
            @AuthenticationPrincipal UserDetails userDetails) {
        teamService.removeMember(teamId, athleteId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponse> getTeam(@PathVariable Long teamId) {
        return ResponseEntity.ok(teamService.getTeam(teamId));
    }

    @GetMapping
    public ResponseEntity<List<TeamResponse>> getAllTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }

    @GetMapping("/my-teams")
    public ResponseEntity<List<TeamResponse>> getMyTeams(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(teamService.getTeamsByCaptain(userDetails.getUsername()));
    }

    @GetMapping("/my-memberships")
    public ResponseEntity<List<TeamResponse>> getMyMemberships(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(teamService.getTeamsByAthlete(userDetails.getUsername()));
    }

    @GetMapping("/sport/{sport}")
    public ResponseEntity<List<TeamResponse>> getTeamsBySport(@PathVariable String sport) {
        return ResponseEntity.ok(teamService.getTeamsBySport(sport));
    }
}
