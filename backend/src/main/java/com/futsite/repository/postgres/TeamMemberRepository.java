package com.futsite.repository.postgres;

import com.futsite.model.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByTeamId(Long teamId);
    Optional<TeamMember> findByTeamIdAndAthleteId(Long teamId, Long athleteId);
    boolean existsByTeamIdAndAthleteId(Long teamId, Long athleteId);
    boolean existsByTeamIdAndJerseyNumber(Long teamId, Integer jerseyNumber);
}
