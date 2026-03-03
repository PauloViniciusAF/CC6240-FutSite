package com.futsite.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MatchTimerResponse {
    private Long matchId;
    private String status;
    private Integer elapsedSeconds;
    private Integer totalSeconds;
}
