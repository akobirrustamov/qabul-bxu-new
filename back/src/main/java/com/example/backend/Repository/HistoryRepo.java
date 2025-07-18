package com.example.backend.Repository;

import com.example.backend.Entity.History;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface HistoryRepo extends JpaRepository<History, UUID> {



    @Modifying
    @Transactional
    @Query(value = "delete from history where abuturient_id = :abuturientId", nativeQuery = true)
    void deleteByAbuturientId(@Param("abuturientId") UUID abuturientId);


}

