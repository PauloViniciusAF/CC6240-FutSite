package com.futsite.controller;

import com.futsite.dto.request.CreateChampionshipRequest;
import com.futsite.dto.request.SetBracketRequest;
import com.futsite.dto.response.ChampionshipResponse;
import com.futsite.service.ChampionshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/championships")
@RequiredArgsConstructor
public class ChampionshipController {

    private final ChampionshipService championshipService;

    @PostMapping
    public ResponseEntity<ChampionshipResponse> create(
            @Valid @RequestBody CreateChampionshipRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(championshipService.createChampionship(request, userDetails.getUsername()));
    }

    @PostMapping("/{id}/teams/{teamId}")
    public ResponseEntity<ChampionshipResponse> addTeam(
            @PathVariable Long id,
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(championshipService.addTeam(id, teamId, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}/teams/{teamId}")
    public ResponseEntity<ChampionshipResponse> removeTeam(
            @PathVariable Long id,
            @PathVariable Long teamId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(championshipService.removeTeam(id, teamId, userDetails.getUsername()));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ChampionshipResponse> start(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(championshipService.startChampionship(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/bracket")
    public ResponseEntity<ChampionshipResponse> setBracket(
            @PathVariable Long id,
            @Valid @RequestBody SetBracketRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(championshipService.setBracket(id, request, userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChampionshipResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(championshipService.getChampionship(id));
    }

    @GetMapping
    public ResponseEntity<List<ChampionshipResponse>> getAll() {
        return ResponseEntity.ok(championshipService.getAllChampionships());
    }

    @GetMapping("/my-championships")
    public ResponseEntity<List<ChampionshipResponse>> getMyChampionships(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(championshipService.getChampionshipsByManager(userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        championshipService.deleteChampionship(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
