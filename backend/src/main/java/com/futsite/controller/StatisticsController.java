package com.futsite.controller;

import com.futsite.model.document.ChampionshipStatistics;
import com.futsite.model.document.MatchStatistics;
import com.futsite.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/match/{matchId}")
    public ResponseEntity<MatchStatistics> getMatchStats(@PathVariable Long matchId) {
        MatchStatistics stats = statisticsService.getMatchStatistics(matchId);
        return stats != null ? ResponseEntity.ok(stats) : ResponseEntity.notFound().build();
    }

    @GetMapping("/championship/{champId}")
    public ResponseEntity<ChampionshipStatistics> getChampionshipStats(@PathVariable Long champId) {
        ChampionshipStatistics stats = statisticsService.getChampionshipStatistics(champId);
        return stats != null ? ResponseEntity.ok(stats) : ResponseEntity.notFound().build();
    }

    @GetMapping("/championship/{champId}/matches")
    public ResponseEntity<List<MatchStatistics>> getChampionshipMatchStats(@PathVariable Long champId) {
        return ResponseEntity.ok(statisticsService.getMatchStatisticsByChampionship(champId));
    }
}
