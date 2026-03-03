package com.futsite.repository.postgres;

import com.futsite.model.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByCaptainId(Long captainId);
    List<Team> findBySport(String sport);

    @Query("SELECT t FROM Team t JOIN t.members m WHERE m.athlete.id = :athleteId")
    List<Team> findByAthleteId(Long athleteId);
}
