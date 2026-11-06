package com.nikitarybalko.wargamebroker.infrastructure.persistence.repository;

import com.nikitarybalko.wargamebroker.infrastructure.persistence.entity.SnapshotEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SnapshotRepository extends JpaRepository<SnapshotEntity, UUID> {

    List<SnapshotEntity> findAllByOrderByTickNumberDesc(Limit limit);
}
