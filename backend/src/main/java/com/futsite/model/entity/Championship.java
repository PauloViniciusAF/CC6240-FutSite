package com.futsite.model.entity;

import com.futsite.model.enums.ChampionshipFormat;
import com.futsite.model.enums.ChampionshipStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "championships")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Championship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChampionshipFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ChampionshipStatus status = ChampionshipStatus.DRAFT;

    // ---- Round Robin specific ----
    /** true = turno e returno, false = somente turno */
    @Column
    private Boolean homeAndAway;

    @Column
    private Integer winPoints;

    @Column
    private Integer drawPoints;

    @Column
    private Integer lossPoints;

    // ---- Knockout specific ----
    /** true = ida e volta, false = jogo único */
    @Column
    private Boolean twoLegged;

    /** Number of teams for knockout (2, 4, 8, 16) */
    @Column
    private Integer knockoutTeamCount;

    // ---- Sports ----
    @ElementCollection
    @CollectionTable(name = "championship_sports", joinColumns = @JoinColumn(name = "championship_id"))
    @Column(name = "sport")
    @Builder.Default
    private List<String> sports = new ArrayList<>();

    // ---- Teams ----
    @ManyToMany
    @JoinTable(
        name = "championship_teams",
        joinColumns = @JoinColumn(name = "championship_id"),
        inverseJoinColumns = @JoinColumn(name = "team_id")
    )
    @Builder.Default
    private List<Team> teams = new ArrayList<>();

    // ---- Matches ----
    @OneToMany(mappedBy = "championship", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Match> matches = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
