package com.futsite.controller;

import com.futsite.dto.request.CreateMatchRequest;
import com.futsite.dto.request.RecordGoalRequest;
import com.futsite.dto.response.GoalResponse;
import com.futsite.dto.response.MatchResponse;
import com.futsite.dto.response.MatchTimerResponse;
import com.futsite.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @PostMapping("/championship/{champId}")
    public ResponseEntity<MatchResponse> createMatch(
            @PathVariable Long champId,
            @Valid @RequestBody CreateMatchRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(matchService.createMatch(champId, request, userDetails.getUsername()));
    }

    @PostMapping("/{matchId}/start")
    public ResponseEntity<MatchResponse> start(
            @PathVariable Long matchId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(matchService.startMatch(matchId, userDetails.getUsername()));
    }

    @PostMapping("/{matchId}/pause")
    public ResponseEntity<MatchResponse> pause(
            @PathVariable Long matchId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(matchService.pauseMatch(matchId, userDetails.getUsername()));
    }

    @PostMapping("/{matchId}/resume")
    public ResponseEntity<MatchResponse> resume(
            @PathVariable Long matchId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(matchService.resumeMatch(matchId, userDetails.getUsername()));
    }

    @PostMapping("/{matchId}/finish")
    public ResponseEntity<MatchResponse> finish(
            @PathVariable Long matchId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(matchService.finishMatch(matchId, userDetails.getUsername()));
    }

    @PostMapping("/{matchId}/timer/adjust")
    public ResponseEntity<MatchTimerResponse> adjustTimer(
            @PathVariable Long matchId,
            @RequestParam int deltaSeconds,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(matchService.adjustTimer(matchId, deltaSeconds, userDetails.getUsername()));
    }

    @PostMapping("/{matchId}/goals")
    public ResponseEntity<GoalResponse> recordGoal(
            @PathVariable Long matchId,
            @Valid @RequestBody RecordGoalRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(matchService.recordGoal(matchId, request, userDetails.getUsername()));
    }

    @GetMapping("/{matchId}/timer")
    public ResponseEntity<MatchTimerResponse> getTimer(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchService.getTimer(matchId));
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<MatchResponse> getMatch(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchService.getMatch(matchId));
    }

    @GetMapping("/championship/{champId}")
    public ResponseEntity<List<MatchResponse>> getByChampionship(@PathVariable Long champId) {
        return ResponseEntity.ok(matchService.getMatchesByChampionship(champId));
    }

    @GetMapping("/championship/{champId}/round/{round}")
    public ResponseEntity<List<MatchResponse>> getByRound(
            @PathVariable Long champId, @PathVariable Integer round) {
        return ResponseEntity.ok(matchService.getMatchesByChampionshipAndRound(champId, round));
    }
}
