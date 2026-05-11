package com.futsite.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecordGoalRequest {
    /** Team that scored */
    @NotNull
    private Long teamId;

    /** Scorer athlete ID - null if own goal */
    private Long scorerId;

    @NotNull
    private Boolean ownGoal;

    /** Note: minute and second are auto-calculated by backend from match timer (not required from client) */
    private Integer minute;

    private Integer second;
}
