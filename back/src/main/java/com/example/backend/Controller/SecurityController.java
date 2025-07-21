package com.example.backend.Controller;

import com.example.backend.Security.JwtService;
import com.example.backend.Services.CaptchaService;
import com.example.backend.Services.SecurityService.SecurityService;
import com.example.backend.Services.SecurityServiceUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/security")
@RequiredArgsConstructor
public class SecurityController {

    private final SecurityService securityService;
    private final JwtService jwtService;
    private final SecurityServiceUser securityServiceUser;
    private final CaptchaService captchaService;

    @GetMapping
    public HttpEntity<?> checkSecurity(@RequestHeader("Authorization") String authorization) {
        return securityService.checkSecurity(authorization);
    }

    @PostMapping("/generate")
    public HttpEntity<?> generateWithCaptcha(
            @RequestParam("g-recaptcha-response") String captchaToken,
            @RequestHeader(value = "X-Forwarded-For", required = false) String ip,
            HttpServletRequest request) {

        if (ip == null) ip = request.getRemoteAddr();
        System.out.printf("IP: %s\n", ip);

        if (!captchaService.verifyCaptcha(captchaToken)) {
            return ResponseEntity.status(403).body("Captcha validation failed");
        }

        return securityServiceUser.generateToken(ip);
    }
}
