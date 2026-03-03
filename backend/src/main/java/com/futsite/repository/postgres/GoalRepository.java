package com.futsite.repository.postgres;

import com.futsite.model.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByMatchId(Long matchId);
    List<Goal> findByScorerId(Long scorerId);
    List<Goal> findByTeamId(Long teamId);
}
