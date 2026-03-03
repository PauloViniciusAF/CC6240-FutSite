package com.futsite.model.entity;

import com.futsite.model.enums.MatchStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "championship_id", nullable = false)
    private Championship championship;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    /** Round number (rodada) or bracket phase (1=final, 2=semi, 4=quartas, 8=oitavas) */
    @Column(nullable = false)
    private Integer round;

    /** For knockout: bracket position within the round */
    @Column
    private Integer bracketPosition;

    /** Scheduled date/time */
    @Column
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MatchStatus status = MatchStatus.SCHEDULED;

    /** Match duration in seconds */
    @Column(nullable = false)
    @Builder.Default
    private Integer durationSeconds = 0;

    /** Max goals per team (0 = unlimited) */
    @Column
    @Builder.Default
    private Integer goalLimit = 0;

    @Column
    @Builder.Default
    private Integer homeScore = 0;

    @Column
    @Builder.Default
    private Integer awayScore = 0;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Goal> goals = new ArrayList<>();

    /** For knockout with two legs: reference to the return match */
    @OneToOne
    @JoinColumn(name = "return_match_id")
    private Match returnMatch;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
