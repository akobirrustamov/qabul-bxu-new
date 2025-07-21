package com.example.backend.Services;

import com.example.backend.Entity.BrowserToken;
import com.example.backend.Repository.BrowserTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecurityServiceUser {
    private final BrowserTokenRepository tokenRepository;

    public ResponseEntity<?> generateToken(String ip) {
        String token = UUID.randomUUID().toString();

        BrowserToken browserToken = BrowserToken.builder()
                .token(token)
                .createdAt(LocalDateTime.now())
                .ipAddress(ip)
                .isActive(true)
                .build();

        tokenRepository.save(browserToken);

        return ResponseEntity.ok().body(token);
    }

    public ResponseEntity<?> checkSecurity(String token) {
        Optional<BrowserToken> optional = tokenRepository.findByToken(token.replace("Bearer ", ""));
        if (optional.isPresent()) {
            return ResponseEntity.ok("Valid browser token");
        }
        return ResponseEntity.status(401).body("Invalid token");
    }
}
