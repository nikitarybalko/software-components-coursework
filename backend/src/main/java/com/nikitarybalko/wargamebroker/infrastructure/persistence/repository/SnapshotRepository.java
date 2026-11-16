package com.nikitarybalko.wargamebroker.infrastructure.persistence.repository;

import com.nikitarybalko.wargamebroker.infrastructure.persistence.entity.SnapshotEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SnapshotRepository extends JpaRepository<SnapshotEntity, UUID> {

    List<SnapshotEntity> findAllByOrderByTickNumberDesc(Limit limit);

    /**
     * Empties snapshots and their units in constant time; a row-by-row delete of a long battle's history
     * takes minutes because of the foreign key check on every removed snapshot.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "TRUNCATE TABLE snapshot_units, snapshots", nativeQuery = true)
    void truncateSnapshots();
}
