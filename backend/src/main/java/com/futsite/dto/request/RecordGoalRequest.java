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

    /** Minute in the match */
    @NotNull
    private Integer minute;

    private Integer second;
}
