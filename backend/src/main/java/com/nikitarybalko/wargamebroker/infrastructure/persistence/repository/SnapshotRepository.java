package com.nikitarybalko.wargamebroker.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.nikitarybalko.wargamebroker.infrastructure.persistence.entity.SnapshotEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SnapshotRepository extends JpaRepository<SnapshotEntity, UUID> {

    List<SnapshotEntity> findTop50ByOrderByTickNumberDesc();

    Optional<SnapshotEntity> findFirstByOrderByTickNumberDesc();
}
