package com.example.backend.Repository;

import com.example.backend.Entity.BrowserToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BrowserTokenRepository extends JpaRepository<BrowserToken, UUID> {
    Optional<BrowserToken> findByToken(String token);
}
