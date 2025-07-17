package com.example.backend.Controller;

import com.example.backend.Entity.Abuturient;
import com.example.backend.Entity.AbuturientAmocrm;
import com.example.backend.Repository.AbuturientAmocrmRepo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("/api/v1/amocrm")
@Slf4j
public class AmoCrmController {
    private static final String CLIENT_ID = "9e491003-9cb6-4e3a-88f5-135389c1b100";
    private static final String CLIENT_SECRET = "lN3rlcMQ0VOyDIBBS8UYkm8j6O73ST78nnssrbaCs97UaUG8H8iiZ0d2Vxe0WHL4";
    private static final String REDIRECT_URI = "https://6720aadf263a.ngrok-free.app/api/v1/amocrm/oauth";
    private static final String AMO_DOMAIN = "buxpxticrm.amocrm.ru";
    private static final String TOKEN_PATH = "./tokens.json";

    private final AbuturientAmocrmRepo abuturientAmocrmRepo;


    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, Map<String, Long>> temporaryStorage = new HashMap<>();




    @GetMapping("/oauth")
    public ResponseEntity<?> oauth(@RequestParam String code) {
        System.out.printf("OAUTH CODE: %s\n", code);
        try {
            Map<String, String> request = new HashMap<>();
            request.put("client_id", CLIENT_ID);
            request.put("client_secret", CLIENT_SECRET);
            request.put("grant_type", "authorization_code");
            request.put("code", code);
            request.put("redirect_uri", REDIRECT_URI);

            ResponseEntity<Tokens> response = restTemplate.postForEntity(
                    "https://" + AMO_DOMAIN + "/oauth2/access_token",
                    request,
                    Tokens.class
            );

            saveTokens(response.getBody());
            return ResponseEntity.ok("Токены сохранены, интеграция работает!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при авторизации: " + e.getMessage());
        }
    }

    @PostMapping("/lead-step1")
    public ResponseEntity<?> leadStep1(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String name = body.get("name");

        if (phone == null || phone.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Номер телефона обязателен"));
        }

        try {
            Tokens tokens = refreshTokensIfNeeded();
            HttpHeaders headers = createHeaders(tokens.getAccessToken());

            // 1️⃣ Create Lead (unchanged)

            Map<String, Object> lead = new HashMap<>();
            lead.put("name", "Yangi ariza: " + phone);
            lead.put("pipeline_id", 9193022L);
            lead.put("status_id", 78346610L);

            ResponseEntity<AmoLeadResponse> leadResp = restTemplate.exchange(
                    "https://" + AMO_DOMAIN + "/api/v4/leads",
                    HttpMethod.POST,
                    new HttpEntity<>(List.of(lead), headers),
                    AmoLeadResponse.class
            );

            Long leadId = leadResp.getBody().getEmbedded().getLeads().get(0).getId();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "leadId", leadId
            ));







        } catch (Exception e) {
            log.error("Ошибка на шаге 1:", e);
            return ResponseEntity.status(500).body(Map.of("error", "Ошибка на шаге 1: " + e.getMessage()));
        }
    }


    @PostMapping("/lead-step2")
    private boolean leadStep2(Abuturient abuturient) {
        Optional<AbuturientAmocrm> amocrm1 = abuturientAmocrmRepo.findByAbuturientId(abuturient.getId());
        if (amocrm1.isEmpty()) {
           return false;
        }
        AbuturientAmocrm amocrm = amocrm1.get();
        Long leadId = amocrm.getLeadId();
        try {
            Tokens tokens = refreshTokensIfNeeded();
            HttpHeaders headers = createHeaders(tokens.getAccessToken());

            Map<String, Object> updatedLead = new HashMap<>();
            updatedLead.put("pipeline_id", 9193022L);
            updatedLead.put("status_id", 73856166L);

            ResponseEntity<String> updateResponse = restTemplate.exchange(
                    "https://" + AMO_DOMAIN + "/api/v4/leads/" + leadId,
                    HttpMethod.PATCH,
                    new HttpEntity<>(updatedLead, headers),
                    String.class
            );

            System.out.println("Lead updated: " + updateResponse.getBody());
            return true;
        } catch (Exception e) {
            System.out.println("Error updating lead: " + e.getMessage());
            return false;
        }
    }



    //    amocrm varonkalarni olish
    @GetMapping("/pipelines")
    public ResponseEntity<?> getPipelines() {
        System.out.println("eererer");
        try {
            Tokens tokens = refreshTokensIfNeeded();
            HttpHeaders headers = createHeaders(tokens.getAccessToken());

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://" + AMO_DOMAIN + "/api/v4/leads/pipelines",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            System.out.println("Pipeline list response: ");
            System.out.println(response.getBody());

            return ResponseEntity.ok(objectMapper.readTree(response.getBody()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Pipelines ma'lumotini olishda xatolik: " + e.getMessage()));
        }
    }

//    amocrm varonkalar malumotini olish
    @GetMapping("/leads/custom-fields")
    public ResponseEntity<?> getLeadsCustomFields() {
        try {
            Tokens tokens = refreshTokensIfNeeded();
            HttpHeaders headers = createHeaders(tokens.getAccessToken());

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://" + AMO_DOMAIN + "/api/v4/leads/custom_fields",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            System.out.println("Custom Fields Response:");
            System.out.println(response.getBody());

            return ResponseEntity.ok(objectMapper.readTree(response.getBody()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Custom fields olishda xatolik: " + e.getMessage()));
        }
    }




    @PostMapping("/webhook-handler")
    public ResponseEntity<?> handleWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Webhook received: {}", payload);

        try {
            Map<String, Object> event = (Map<String, Object>) payload.get("event");
            if (event != null) {
                String typeCode = (String) event.get("type_code");
                log.info("Webhook type_code: {}", typeCode);

                if ("lead_status_changed".equals(typeCode) || "lead_appeared_in_status".equals(typeCode)) {
                    Map<String, Object> data = (Map<String, Object>) event.get("data");
                    Long leadId = Long.parseLong(data.get("id").toString());

                    // Optional: log stage ID and pipeline ID for debugging
                    log.info("Lead ID: {}, Status ID: {}, Pipeline ID: {}",
                            leadId, data.get("status_id"), data.get("pipeline_id"));

                    // Respond immediately — don't do long processing here!
                    return ResponseEntity.ok(Map.of(
                            "status", "received",
                            "leadId", leadId
                    ));
                }
            }

            return ResponseEntity.ok(Map.of("status", "ignored"));
        } catch (Exception e) {
            log.error("Webhook processing error", e);
            return ResponseEntity.status(500).body(Map.of("error", "Webhook processing error: " + e.getMessage()));
        }
    }


    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private Tokens refreshTokensIfNeeded() throws IOException {
        Tokens tokens = loadTokens();
        if (System.currentTimeMillis() >= tokens.getExpiresAt() - 60000) {
            Map<String, String> request = new HashMap<>();
            request.put("client_id", CLIENT_ID);
            request.put("client_secret", CLIENT_SECRET);
            request.put("grant_type", "refresh_token");
            request.put("refresh_token", tokens.getRefreshToken());
            request.put("redirect_uri", REDIRECT_URI);


            ResponseEntity<Tokens> response = restTemplate.postForEntity(
                    "https://" + AMO_DOMAIN + "/oauth2/access_token",
                    request,
                    Tokens.class
            );

            saveTokens(response.getBody());
            return response.getBody();
        }
        return tokens;
    }

    private Tokens loadTokens() throws IOException {
        if (!Files.exists(Paths.get(TOKEN_PATH))) {
            throw new FileNotFoundException("Файл токенов не найден");
        }
        try (InputStream is = new FileInputStream(TOKEN_PATH)) {
            return objectMapper.readValue(is, Tokens.class);
        }
    }

    private void saveTokens(Tokens tokens) throws IOException {
        tokens.setExpiresAt(System.currentTimeMillis() + tokens.getExpiresIn() * 1000);
        try (OutputStream os = new FileOutputStream(TOKEN_PATH)) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(os, tokens);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tokens {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("refresh_token")
        private String refreshToken;
        @JsonProperty("expires_in")
        private long expiresIn;
        private long expiresAt;
    }

    @Data
    public static class AmoLeadResponse {
        @JsonProperty("_embedded")
        private EmbeddedLeads embedded;

        @Data
        public static class EmbeddedLeads {
            private List<Lead> leads;
        }

        @Data
        public static class Lead {
            private Long id;
        }
    }

    @Data
    public static class AmoContactResponse {
        @JsonProperty("_embedded")
        private EmbeddedContacts embedded;

        @Data
        public static class EmbeddedContacts {
            private List<Contact> contacts;
        }

        @Data
        public static class Contact {
            private Long id;
        }
    }
}
