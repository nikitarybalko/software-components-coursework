package com.nikitarybalko.wargamebroker.infrastructure.persistence.repository;

import com.nikitarybalko.wargamebroker.infrastructure.persistence.entity.EventLogEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventLogRepository extends JpaRepository<EventLogEntity, UUID> {

    List<EventLogEntity> findAllByOrderByTimestampDesc(Limit limit);
}
