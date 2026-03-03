package com.futsite.dto.response;

import com.futsite.model.enums.ChampionshipFormat;
import com.futsite.model.enums.ChampionshipStatus;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ChampionshipResponse {
    private Long id;
    private String name;
    private UserResponse manager;
    private ChampionshipFormat format;
    private ChampionshipStatus status;
    private List<String> sports;
    private List<TeamResponse> teams;

    // Round Robin
    private Boolean homeAndAway;
    private Integer winPoints;
    private Integer drawPoints;
    private Integer lossPoints;

    // Knockout
    private Boolean twoLegged;
    private Integer knockoutTeamCount;
}
