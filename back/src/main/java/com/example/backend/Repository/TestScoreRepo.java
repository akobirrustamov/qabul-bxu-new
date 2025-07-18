package com.example.backend.Repository;

import com.example.backend.Entity.Abuturient;
import com.example.backend.Entity.TestScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface TestScoreRepo extends JpaRepository<TestScore,Integer> {
    @Query(value = "select * from test_score where abuturient_id=:byPhone",  nativeQuery = true)
    TestScore findByAbuturientId(UUID byPhone);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM test_score WHERE abuturient_id = :abuturientId", nativeQuery = true)
    void deleteByAbuturientId(@Param("abuturientId") UUID abuturientId);
}
