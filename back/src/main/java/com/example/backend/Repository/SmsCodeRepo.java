package com.example.backend.Repository;

import com.example.backend.Entity.SmsCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface SmsCodeRepo extends JpaRepository<SmsCode,Integer> {


    @Query(value = "SELECT * FROM sms_code WHERE abuturient_id = :abuturientId", nativeQuery = true)
    Optional<SmsCode> findByAbuturientId(UUID abuturientId);

}
