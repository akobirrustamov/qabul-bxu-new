package com.example.backend.Controller;

import com.example.backend.Security.JwtService;
import com.example.backend.Services.SecurityService.SecurityService;
import com.example.backend.Services.SecurityServiceUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security")
@RequiredArgsConstructor
public class SecurityController {
    private final SecurityService securityService;
    private final JwtService jwtService;
    private final SecurityServiceUser securityServiceUser;

    @GetMapping
    public HttpEntity<?> checkSecurity(@RequestHeader("Authorization") String authorization) {
        return securityService.checkSecurity(authorization);
    }

    @GetMapping("/generate")
    public HttpEntity<?> generateBrowserToken(@RequestHeader(value = "X-Forwarded-For", required = false) String ip,
                                              HttpServletRequest request) {
        if (ip == null) ip = request.getRemoteAddr();
        return securityServiceUser.generateToken(ip);
    }


}

