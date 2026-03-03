package com.futsite.repository.postgres;

import com.futsite.model.entity.Championship;
import com.futsite.model.enums.ChampionshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChampionshipRepository extends JpaRepository<Championship, Long> {
    List<Championship> findByManagerId(Long managerId);
    List<Championship> findByStatus(ChampionshipStatus status);

    @Query("SELECT c FROM Championship c JOIN c.teams t WHERE t.id = :teamId")
    List<Championship> findByTeamId(Long teamId);
}
