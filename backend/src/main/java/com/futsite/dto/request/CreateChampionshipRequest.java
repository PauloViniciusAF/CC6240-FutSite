package com.futsite.dto.request;

import com.futsite.model.enums.ChampionshipFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CreateChampionshipRequest {
    @NotBlank
    private String name;

    @NotNull
    private List<String> sports;

    @NotNull
    private ChampionshipFormat format;

    // Round Robin fields
    private Boolean homeAndAway;
    private Integer winPoints;
    private Integer drawPoints;
    private Integer lossPoints;

    // Knockout fields
    private Boolean twoLegged;
    private Integer knockoutTeamCount;

    /** Optional: add teams at creation */
    private List<Long> teamIds;
}
