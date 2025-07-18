package com.example.backend.Repository;

import com.example.backend.Entity.History;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface HistoryRepo extends JpaRepository<History, UUID> {
    @Query(value = "delete from history where abuturient_id=:abuturientId", nativeQuery = true)
    void deleteByAbuturientId(UUID abuturientId);
}
