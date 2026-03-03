package com.futsite.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddTeamMemberRequest {
    @NotNull
    private Long athleteId;

    @NotNull
    private Integer jerseyNumber;
}
