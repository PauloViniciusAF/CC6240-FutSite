package com.futsite.repository.postgres;

import com.futsite.model.entity.Match;
import com.futsite.model.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByChampionshipId(Long championshipId);
    List<Match> findByChampionshipIdAndRound(Long championshipId, Integer round);
    List<Match> findByChampionshipIdAndStatus(Long championshipId, MatchStatus status);
    List<Match> findByHomeTeamIdOrAwayTeamId(Long homeTeamId, Long awayTeamId);
}
