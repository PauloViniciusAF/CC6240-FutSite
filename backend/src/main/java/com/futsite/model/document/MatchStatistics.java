package com.futsite.model.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Document(collection = "match_statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchStatistics {

    @Id
    private String id;

    private Long matchId;
    private Long championshipId;

    private String homeTeamName;
    private String awayTeamName;
    private Long homeTeamId;
    private Long awayTeamId;

    private Integer homeScore;
    private Integer awayScore;

    private Integer durationSeconds;

    @Builder.Default
    private List<GoalDetail> goals = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> homeStats = new HashMap<>();

    @Builder.Default
    private Map<String, Object> awayStats = new HashMap<>();

    private LocalDateTime playedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GoalDetail {
        private String scorerName;
        private Long scorerId;
        private String teamName;
        private Boolean ownGoal;
        private Integer minute;
        private Integer second;
    }
}
