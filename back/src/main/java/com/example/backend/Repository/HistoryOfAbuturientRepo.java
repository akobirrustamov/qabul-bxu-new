package com.example.backend.Repository;

import com.example.backend.Entity.HistoryOfAbuturient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface HistoryOfAbuturientRepo extends JpaRepository<HistoryOfAbuturient,Integer> {


    @Query(value = "select * from history_of_abuturient where DATE(date) = :createdAt", nativeQuery = true)
    List<HistoryOfAbuturient> findAllByDate(LocalDate createdAt);
    @Modifying
    @Transactional
    @Query(value = "delete from history_of_abuturient where abuturient_id = :abuturientId", nativeQuery = true)
    void deleteByAbuturientId( UUID abuturientId);
}
