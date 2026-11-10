package com.afterApp.after.repositories;

import com.afterApp.after.entity.Events;
import com.afterApp.after.enums.EventType;
import com.afterApp.after.enums.MusicStyle;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Events, Long> {
    List<Events> findByEventType(EventType Type);
    List<Events> findByMusicStyle(MusicStyle style);
    List<Events> findByEventTypeAndMusicStyle(EventType eventType, MusicStyle musicStyle);
    List<Events> findDistinctByHost_Id(Long userId);
    List<Events> findDistinctByUsers_Id(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Events e where e.id = :id")
    Optional<Events> findByIdForUpdate(@Param("id") Long id);
}
