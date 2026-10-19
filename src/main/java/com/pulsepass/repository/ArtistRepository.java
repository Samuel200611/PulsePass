package com.pulsepass.repository;

import com.pulsepass.domain.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    Optional<Artist> findByStageNameIgnoreCase(String stageName);

    List<Artist> findByActiveTrueOrderByStageNameAsc();

    List<Artist> findByStageNameContainingIgnoreCase(String fragment);

    @Query("""
            SELECT a
            FROM Artist a
            JOIN a.events e
            WHERE e.eventCode = :eventCode
            ORDER BY a.stageName ASC
            """)
    List<Artist> findArtistsByEventCode(@Param("eventCode") String eventCode);
}
