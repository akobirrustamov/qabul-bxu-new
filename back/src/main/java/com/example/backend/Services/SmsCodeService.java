package com.example.backend.Services;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class SmsCodeService {

    public Boolean sendSmsCode(String phoneNumber, Integer code){
        try {
            RestTemplate restTemplate = new RestTemplate();
            String email = "akobirjavadev10@gmail.com";
            String password = "qCfkQTHQbQAJLJeElWWI9bv1stjoh3Unt6dNiE04";
            String loginUrl = "https://notify.eskiz.uz/api/auth/login";

            // 1. Auth - token olish
            Map<String, String> loginPayload = new HashMap<>();
            loginPayload.put("email", email);
            loginPayload.put("password", password);

            Map loginResponse = restTemplate.postForObject(loginUrl, loginPayload, Map.class);
            String token = (String) ((Map) loginResponse.get("data")).get("token");

            // 2. Template olish
            String templateUrl = "https://notify.eskiz.uz/api/user/templates";
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders(token));
            Map templatesResponse = restTemplate.exchange(templateUrl, HttpMethod.GET, entity, Map.class).getBody();

            // 3. Template matnidan foydalanish va kodni joylashtirish
            String template = (String) ((Map) ((java.util.List) templatesResponse.get("result")).get(0)).get("template");
            String finalMessage = String.format(template, code); // %d joyga kod qo‘yiladi
            System.out.printf("SMS Code: %s\n", finalMessage);
            // 4. SMS yuborish
            String smsUrl = "https://notify.eskiz.uz/api/message/sms/send";
            Map<String, String> smsPayload = new HashMap<>();
            smsPayload.put("mobile_phone", phoneNumber);
            smsPayload.put("message", finalMessage);
            smsPayload.put("from", "4546");

            HttpEntity<Map<String, String>> smsEntity = new HttpEntity<>(smsPayload, createHeaders(token));
            restTemplate.postForObject(smsUrl, smsEntity, Map.class);
            System.out.printf("SMS Code: %s\n", smsEntity);
            return true;
        } catch (Exception e) {
            System.out.printf("Error: %s%n", e.getMessage());
            return false;
        }
    }


    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
