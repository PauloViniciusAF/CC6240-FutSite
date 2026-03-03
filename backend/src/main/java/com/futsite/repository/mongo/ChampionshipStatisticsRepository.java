package com.futsite.repository.mongo;

import com.futsite.model.document.ChampionshipStatistics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ChampionshipStatisticsRepository extends MongoRepository<ChampionshipStatistics, String> {
    Optional<ChampionshipStatistics> findByChampionshipId(Long championshipId);
}
