package com.futsite.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class TeamResponse {
    private Long id;
    private String name;
    private String sport;
    private UserResponse captain;
    private List<TeamMemberResponse> members;

    @Data
    @Builder
    public static class TeamMemberResponse {
        private Long id;
        private UserResponse athlete;
        private Integer jerseyNumber;
    }
}
