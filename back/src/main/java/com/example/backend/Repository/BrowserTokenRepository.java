package com.example.backend.Repository;

import com.example.backend.Entity.BrowserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface BrowserTokenRepository extends JpaRepository<BrowserToken, UUID> {

    @Query(value = "select * from browser_token where  token=:token", nativeQuery = true)
    Optional<BrowserToken> findByToken(String token);
}
