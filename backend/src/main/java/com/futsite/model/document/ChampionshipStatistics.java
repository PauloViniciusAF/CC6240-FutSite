package com.futsite.model.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "championship_statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChampionshipStatistics {

    @Id
    private String id;

    private Long championshipId;
    private String championshipName;

    @Builder.Default
    private List<TeamStanding> standings = new ArrayList<>();

    @Builder.Default
    private List<TopScorer> topScorers = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeamStanding {
        private Long teamId;
        private String teamName;
        private Integer played;
        private Integer wins;
        private Integer draws;
        private Integer losses;
        private Integer goalsFor;
        private Integer goalsAgainst;
        private Integer goalDifference;
        private Integer points;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopScorer {
        private Long playerId;
        private String playerName;
        private String teamName;
        private Integer goals;
    }
}
