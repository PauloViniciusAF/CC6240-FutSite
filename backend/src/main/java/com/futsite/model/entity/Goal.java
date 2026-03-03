package com.futsite.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "goals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /** Null if own goal */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scorer_id")
    private User scorer;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ownGoal = false;

    /** Minute of the goal in the match */
    @Column(nullable = false)
    private Integer minute;

    /** Exact second within the match timer */
    @Column
    private Integer second;
}
