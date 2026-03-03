package com.futsite.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "team_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"team_id", "athlete_id"}),
    @UniqueConstraint(columnNames = {"team_id", "jersey_number"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_id", nullable = false)
    private User athlete;

    @Column(name = "jersey_number", nullable = false)
    private Integer jerseyNumber;
}
