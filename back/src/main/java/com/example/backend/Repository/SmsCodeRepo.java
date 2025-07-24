package com.example.backend.Repository;

import com.example.backend.Entity.SmsCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface SmsCodeRepo extends JpaRepository<SmsCode,Integer> {


    @Query(value = "select * from sms_code where abuturient_id=:abuturientId")
    Optional<SmsCode> findByAbuturientId(UUID abuturientId);
}
