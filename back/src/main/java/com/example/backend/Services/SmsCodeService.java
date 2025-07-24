package com.example.backend.Services;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class SmsCodeService {


    public Boolean sendSmsCode(String phoneNumber, Integer code){
        try {
            RestTemplate restTemplate = new RestTemplate();
            String email = "akobirjavadev10@gmail.com";
            String password = "qCfkQTHQbQAJLJeElWWI9bv1stjoh3Unt6dNiE04";
            String loginUrl = "https://notify.eskiz.uz/api/auth/login";

            Map<String, String> loginPayload = new HashMap<>();
            loginPayload.put("email", email);
            loginPayload.put("password", password);

            Map loginResponse = restTemplate.postForObject(loginUrl, loginPayload, Map.class);
            String token = (String) ((Map) loginResponse.get("data")).get("token");

            String templateUrl = "https://notify.eskiz.uz/api/user/templates";
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders(token));
            Map templatesResponse = restTemplate.exchange(templateUrl, HttpMethod.GET, entity, Map.class).getBody();
            String template = (String) ((Map) ((java.util.List) templatesResponse.get("result")).get(0)).get("template");

            String dynamicUrl = "https://qabul.bxu.uz/api/v1/abuturient/contract/" + phoneNumber;
            String finalMessage = template.replace("%w", dynamicUrl).replace("%d{1,3}", phoneNumber);

            String smsUrl = "https://notify.eskiz.uz/api/message/sms/send";
            Map<String, String> smsPayload = new HashMap<>();
            smsPayload.put("mobile_phone", phoneNumber);
            smsPayload.put("message", finalMessage);
            smsPayload.put("from", "4546");

            HttpEntity<Map<String, String>> smsEntity = new HttpEntity<>(smsPayload, createHeaders(token));
            restTemplate.postForObject(smsUrl, smsEntity, Map.class);
            return true;
        } catch (Exception e) {
            System.out.printf("Error: %s", e.getMessage());
            return false;
//                return new ResponseEntity<>("SMS sending failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
