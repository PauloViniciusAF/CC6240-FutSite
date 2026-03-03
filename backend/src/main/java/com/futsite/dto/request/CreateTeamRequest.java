package com.futsite.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CreateTeamRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String sport;

    /** Optional: add members at creation time */
    private List<TeamMemberEntry> members;

    @Data
    public static class TeamMemberEntry {
        @NotNull
        private Long athleteId;
        @NotNull
        private Integer jerseyNumber;
    }
}
