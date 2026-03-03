package com.futsite.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoalResponse {
    private Long id;
    private String teamName;
    private Long teamId;
    private String scorerName;
    private Long scorerId;
    private Boolean ownGoal;
    private Integer minute;
    private Integer second;
}
