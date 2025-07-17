package com.example.backend.Repository;

import com.example.backend.Entity.HistoryOfAbuturient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface HistoryOfAbuturientRepo extends JpaRepository<HistoryOfAbuturient,Integer> {


    @Query(value = "select * from history_of_abuturient where DATE(date) = :createdAt", nativeQuery = true)
    List<HistoryOfAbuturient> findAllByDate(LocalDate createdAt);
}
