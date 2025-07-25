package com.example.backend.Controller;

import com.example.backend.Entity.SmsCode;
import com.example.backend.Repository.AbuturientRepo;
import com.example.backend.Repository.SmsCodeRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sms")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SmsController {

    private final AbuturientRepo abuturientRepo;
    private final SmsCodeRepo smsCodeRepo;

    @GetMapping("/{abuturientId}/{code}")
    public HttpEntity<?> sendSms(@PathVariable UUID abuturientId, @PathVariable Integer code) {
        Optional<SmsCode> smsCodeOpt = smsCodeRepo.findByAbuturientId(abuturientId);
        System.out.printf("AbuturientId : %s | Code : %s \n", abuturientId, code);
        System.out.printf("SmsCode : %s \n", smsCodeOpt);
        if (smsCodeOpt.isPresent()) {
            SmsCode smsCode = smsCodeOpt.get();
            System.out.print(smsCode);

            // Kod noto'g'ri yoki muddati tugagan bo'lsa
            if (!smsCode.getCode().equals(code) || LocalDateTime.now().isAfter(smsCode.getExpireTime())) {
                return ResponseEntity.status(403).body("Kod noto‘g‘ri yoki eskirgan");
            }

            System.out.printf("SmsCode : %s \n", smsCode);
            smsCodeRepo.delete(smsCode);
            return ResponseEntity.ok(smsCode.getAbuturient());
        }

        return ResponseEntity.notFound().build();
    }
}
