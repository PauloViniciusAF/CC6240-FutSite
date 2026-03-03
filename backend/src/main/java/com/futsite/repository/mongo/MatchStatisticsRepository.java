package com.futsite.repository.mongo;

import com.futsite.model.document.MatchStatistics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchStatisticsRepository extends MongoRepository<MatchStatistics, String> {
    Optional<MatchStatistics> findByMatchId(Long matchId);
    List<MatchStatistics> findByChampionshipId(Long championshipId);
    List<MatchStatistics> findByHomeTeamIdOrAwayTeamId(Long homeTeamId, Long awayTeamId);
}
