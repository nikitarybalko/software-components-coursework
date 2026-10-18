package com.nikitarybalko.wargamebroker.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.nikitarybalko.wargamebroker.infrastructure.persistence.entity.EventLogEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventLogRepository extends JpaRepository<EventLogEntity, UUID> {

    List<EventLogEntity> findAllByOrderByTimestampDesc();
}
