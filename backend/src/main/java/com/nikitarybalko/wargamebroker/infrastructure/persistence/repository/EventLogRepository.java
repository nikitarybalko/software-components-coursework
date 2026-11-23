package com.nikitarybalko.wargamebroker.infrastructure.persistence.repository;

import com.nikitarybalko.wargamebroker.infrastructure.persistence.entity.EventLogEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventLogRepository extends JpaRepository<EventLogEntity, UUID> {

    List<EventLogEntity> findAllByOrderByTimestampDesc(Limit limit);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "TRUNCATE TABLE event_logs", nativeQuery = true)
    void truncateEvents();
}
