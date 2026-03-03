package com.futsite.dto.response;

import com.futsite.model.enums.MatchStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MatchResponse {
    private Long id;
    private Long championshipId;
    private String championshipName;
    private TeamResponse homeTeam;
    private TeamResponse awayTeam;
    private Integer round;
    private Integer bracketPosition;
    private LocalDateTime scheduledAt;
    private MatchStatus status;
    private Integer durationSeconds;
    private Integer goalLimit;
    private Integer homeScore;
    private Integer awayScore;
    private List<GoalResponse> goals;
}
