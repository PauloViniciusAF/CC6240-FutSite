package com.futsite.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateMatchRequest {
    @NotNull
    private Long homeTeamId;

    @NotNull
    private Long awayTeamId;

    @NotNull
    private Integer durationSeconds;

    private Integer goalLimit;

    private LocalDateTime scheduledAt;

    /** For knockout: which round (8=oitavas, 4=quartas, 2=semi, 1=final) */
    private Integer round;

    private Integer bracketPosition;
}
