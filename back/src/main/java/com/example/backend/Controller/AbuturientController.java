package com.example.backend.Controller;

import com.example.backend.DTO.AbuturientDTO;
import com.example.backend.DTO.ForeignAbuturientDTO;
import com.example.backend.Entity.*;
import com.example.backend.Repository.*;
import com.example.backend.Services.AuthService.AuthService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.itextpdf.text.*;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.openhtmltopdf.css.parser.property.PrimitivePropertyBuilders;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


@RestController
@RequestMapping("/api/v1/abuturient")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AbuturientController {
    private static final String CLIENT_ID = "9e491003-9cb6-4e3a-88f5-135389c1b100";
    private static final String CLIENT_SECRET = "lN3rlcMQ0VOyDIBBS8UYkm8j6O73ST78nnssrbaCs97UaUG8H8iiZ0d2Vxe0WHL4";
    private static final String REDIRECT_URI = "https://6720aadf263a.ngrok-free.app/api/v1/amocrm/oauth";
    private static final String AMO_DOMAIN = "buxpxticrm.amocrm.ru";
    private static final String TOKEN_PATH = "./tokens.json";
    private final AbuturientAmocrmRepo abuturientAmocrmRepo;

    RestTemplate restTemplate = new RestTemplate(
            new HttpComponentsClientHttpRequestFactory(HttpClients.createDefault())
    );


    //    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, Map<String, Long>> temporaryStorage = new HashMap<>();


    private final AbuturientRepo abuturientRepo;
    private final UserRepo userRepo;
    private final AppealTypeRepo appealTypeRepo;
    private final EducationFieldRepo educationFieldRepo;
    private final AgentPathRepo agentPathRepo;
    private final DistrictRepo districtRepo;

    @DeleteMapping("/{abuturientId}")
    public HttpEntity<?> deleteAbuturient(@PathVariable UUID abuturientId) {
        abuturientRepo.deleteById(abuturientId);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/foreign")
    public HttpEntity<?> addForeign(@RequestBody ForeignAbuturientDTO request) {
        System.out.println(request);
        Abuturient abuturient = abuturientRepo.findByPhone(request.getPhone());
        if (Objects.isNull(abuturient)) {
            abuturient = new Abuturient();
        }
        if (abuturient.getStatus() == null) {
            abuturient.setStatus(1);
            abuturient.setFirstName(request.getFirstName());
            abuturient.setLastName(request.getLastName());
            abuturient.setPhone(request.getPhone());
            abuturient.setFatherName(request.getFatherName());
            abuturient.setAppealType(appealTypeRepo.findById(request.getAppealTypeId()).orElseThrow());
            abuturient.setEducationField(educationFieldRepo.findById(request.getEducationFieldId()).orElseThrow());
            abuturient.setEnrolledAt(LocalDateTime.now());
            abuturient.setIsForeign(true);
            abuturient.setCountry(request.getCountry());
            abuturient.setCity(request.getCity());
            abuturientRepo.save(abuturient);
        }
        return ResponseEntity.ok(abuturient);

    }

    @PostMapping
    public HttpEntity<?> addAbuturient(@RequestBody AbuturientDTO request) {
        Optional<AgentPath> byAgentNumber = agentPathRepo.findByAgentNumber(request.getAgentId());
        User agent = null;
        if (byAgentNumber.isPresent()) {
            agent = byAgentNumber.get().getAgent();
        }
        try {
            Abuturient save = abuturientRepo.save(new Abuturient(request.getPhone(), agent, 0, LocalDateTime.now(), contractNumber()));
            leadStep1(request.getPhone(), save);
            return ResponseEntity.ok(save);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error saving Abuturient: " + e.getMessage());
        }
    }

    private boolean leadStep1(String phone, Abuturient abuturient) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        try {
            AmoCrmController.Tokens tokens = refreshTokensIfNeeded();
            HttpHeaders headers = createHeaders(tokens.getAccessToken());

            // 1️⃣ Create Lead
            Map<String, Object> lead = new HashMap<>();
            lead.put("name", "Kontakt: " + phone);
            lead.put("pipeline_id", 9193022L);
            lead.put("status_id", 78346610L);

            ResponseEntity<AmoCrmController.AmoLeadResponse> leadResp = restTemplate.exchange(
                    "https://" + AMO_DOMAIN + "/api/v4/leads",
                    HttpMethod.POST,
                    new HttpEntity<>(List.of(lead), headers),
                    AmoCrmController.AmoLeadResponse.class
            );

            Long leadId = leadResp.getBody().getEmbedded().getLeads().get(0).getId();

            // 2️⃣ Create Contact
            Map<String, Object> contact = new HashMap<>();
            contact.put("name", abuturient.getFirstName() != null ? abuturient.getFirstName() : "Без имени");

            System.out.println("create contact: " + contact);
            // Create values list manually
            List<Map<String, Object>> valuesList = new ArrayList<>();
            valuesList.add(Map.of("value", phone, "enum_code", "WORK"));

            // Create field map
            Map<String, Object> phoneField = new HashMap<>();
            phoneField.put("field_code", "PHONE");
            phoneField.put("values", valuesList);

            contact.put("custom_fields_values", List.of(phoneField));

            System.out.println(contact.toString());
            ResponseEntity<AmoCrmController.AmoContactResponse> contactResp = restTemplate.exchange(
                    "https://" + AMO_DOMAIN + "/api/v4/contacts",
                    HttpMethod.POST,
                    new HttpEntity<>(List.of(contact), headers),
                    AmoCrmController.AmoContactResponse.class
            );

            System.out.println(contactResp.getBody());
            Long contactId = contactResp.getBody().getEmbedded().getContacts().get(0).getId();
            System.out.println(contactId);
            // 3️⃣ Link Contact to Lead
            Map<String, Object> linkBody = Map.of(
                    "to_entity_id", contactId,
                    "to_entity_type", "contacts"
            );

            ResponseEntity<Map> linkResp = restTemplate.exchange(
                    "https://" + AMO_DOMAIN + "/api/v4/leads/" + leadId + "/link",
                    HttpMethod.POST,
                    new HttpEntity<>(List.of(linkBody), headers),
                    Map.class
            );
            System.out.println(linkResp.getBody());

            // 4️⃣ Save Lead ID in Database
            AbuturientAmocrm abuturientAmocrm = new AbuturientAmocrm(abuturient, leadId, LocalDateTime.now(), contactId);
            abuturientAmocrmRepo.save(abuturientAmocrm);

            System.out.println("Lead and contact linked successfully.");
            return true;

        } catch (Exception e) {
            System.out.println("Error in leadStep1: " + e.getMessage());
            return false;
        }
    }


    public Integer contractNumber() {
        Integer randomPathNumber = ThreadLocalRandom.current().nextInt(10000, 1000000);
        Optional<Abuturient> byContractNumber = abuturientRepo.findByContractNumber(randomPathNumber);
        if (byContractNumber.isPresent()) {
            contractNumber();
        }
        return randomPathNumber;
    }

    @PutMapping("/user-info")
    public HttpEntity<?> updateAbuturientUserInfo(@RequestBody AbuturientDTO request) throws IOException {
        System.out.println(request);
        Abuturient abuturient = abuturientRepo.findByPhone(request.getPhone());
        if (Objects.isNull(abuturient)) {
            return ResponseEntity.ok(null);
        }
        if (abuturient.getStatus() == 0) {
            District district = null;
            Optional<District> byId = districtRepo.findById(request.getDistrictId());
            if (byId.isPresent()) {
                district = byId.get();
            }
            abuturient.setStatus(1);
            abuturient.setFirstName(request.getFirstName());
            abuturient.setLastName(request.getLastName());
            abuturient.setPhone(request.getPhone());
            abuturient.setFatherName(request.getFatherName());
            abuturient.setPassportNumber(request.getPassportNumber());
            abuturient.setPassportPin(request.getPassportPin());
            if (request.getLevel() != null) {
                abuturient.setLevel(abuturient.getLevel());
            }
            abuturient.setDistrict(district);

            abuturientRepo.save(abuturient);
            leadStep2(abuturient);
        }
        return ResponseEntity.ok(abuturient);
    }

    private boolean leadStep2(Abuturient abuturient) throws IOException {
        Optional<AbuturientAmocrm> amocrm1 = abuturientAmocrmRepo.findByAbuturientId(abuturient.getId());
        AmoCrmController.Tokens tokens = refreshTokensIfNeeded();
        HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(tokens.getAccessToken());
        if (amocrm1.isEmpty()) {
            // 1️⃣ Create Lead
            Map<String, Object> lead = new HashMap<>();
            lead.put("name", "Kontakt: " + abuturient.getFirstName() + " " + abuturient.getLastName());
            lead.put("pipeline_id", 9193022L);
            lead.put("status_id", 73856166L);

            ResponseEntity<AmoCrmController.AmoLeadResponse> leadResp = restTemplate.exchange(
                    "https://" + AMO_DOMAIN + "/api/v4/leads",
                    HttpMethod.POST,
                    new HttpEntity<>(List.of(lead), headers),
                    AmoCrmController.AmoLeadResponse.class
            );

            Long leadId = leadResp.getBody().getEmbedded().getLeads().get(0).getId();

            // 2️⃣ Create Contact


            // 2️⃣ Update Contact with custom fields using POST
            Map<String, Object> contact = new HashMap<>();
            contact.put("name", abuturient.getFirstName() + " " + abuturient.getLastName());

            List<Map<String, Object>> customFields = new ArrayList<>();

            customFields.add(Map.of(
                    "field_code", "PHONE",
                    "values", List.of(Map.of("value", abuturient.getPhone(), "enum_code", "WORK"))
            ));
            customFields.add(Map.of("field_id", 371363, "values", List.of(Map.of("value", abuturient.getFirstName() + " " + abuturient.getLastName()))));
            customFields.add(Map.of("field_id", 371365, "values", List.of(Map.of("value", abuturient.getAgent() != null ? abuturient.getAgent().getName() : ""))));
            customFields.add(Map.of("field_id", 371367, "values", List.of(Map.of("value", abuturient.getEducationField() != null ? abuturient.getEducationField().getName() : ""))));
            customFields.add(Map.of("field_id", 371369, "values", List.of(Map.of("value", abuturient.getAppealType() != null ? abuturient.getAppealType().getName() : ""))));
            customFields.add(Map.of("field_id", 371371, "values", List.of(Map.of("value", abuturient.getEducationField() != null ? abuturient.getEducationField().getEducationForm().getName() + " " + abuturient.getEducationField().getEducationForm().getEducationType().getName() : ""))));

            contact.put("custom_fields_values", customFields);
            ResponseEntity<AmoCrmController.AmoContactResponse> contactResp = restTemplate.exchange(
                    "https://" + AMO_DOMAIN + "/api/v4/contacts",
                    HttpMethod.POST,
                    new HttpEntity<>(List.of(contact), headers),
                    AmoCrmController.AmoContactResponse.class
            );

            System.out.println(contactResp.getBody());
            Long contactId = contactResp.getBody().getEmbedded().getContacts().get(0).getId();
            System.out.println(contactId);
            // 3️⃣ Link Contact to Lead
            Map<String, Object> linkBody = Map.of(
                    "to_entity_id", contactId,
                    "to_entity_type", "contacts"
            );

            ResponseEntity<Map> linkResp = restTemplate.exchange(
                    "https://" + AMO_DOMAIN + "/api/v4/leads/" + leadId + "/link",
                    HttpMethod.POST,
                    new HttpEntity<>(List.of(linkBody), headers),
                    Map.class
            );
            System.out.println(linkResp.getBody());

            // 4️⃣ Save Lead ID in Database
            AbuturientAmocrm abuturientAmocrm = new AbuturientAmocrm(abuturient, leadId, LocalDateTime.now(), contactId);
            abuturientAmocrmRepo.save(abuturientAmocrm);

            System.out.println("Lead and contact linked successfully.");
            return true;
        }
        AbuturientAmocrm amocrm = amocrm1.get();
        Long leadId = amocrm.getLeadId();
        Long contactId = amocrm.getContactId();
        try {
            // 1️⃣ Update Lead using POST
            Map<String, Object> leadUpdate = new HashMap<>();
            leadUpdate.put("id", leadId);
            leadUpdate.put("pipeline_id", 9193022L);
            leadUpdate.put("name", abuturient.getLastName() + " " + abuturient.getFirstName());
            leadUpdate.put("status_id", 73856166L);
            restTemplate.exchange(
                    String.format("https://%s/api/v4/leads/%d", AMO_DOMAIN, leadId),
                    HttpMethod.PATCH,
                    new HttpEntity<>(leadUpdate, headers),
                    Void.class
            );
            // 2️⃣ Update Contact with custom fields using POST
            Map<String, Object> contactUpdate = new HashMap<>();
            contactUpdate.put("id", contactId);
            contactUpdate.put("name", abuturient.getFirstName() + " " + abuturient.getLastName());
            List<Map<String, Object>> customFields = new ArrayList<>();
            customFields.add(Map.of(
                    "field_code", "PHONE",
                    "values", List.of(Map.of("value", abuturient.getPhone(), "enum_code", "WORK"))
            ));
            customFields.add(Map.of("field_id", 371363, "values", List.of(Map.of("value", abuturient.getFirstName() + " " + abuturient.getLastName()))));
            customFields.add(Map.of("field_id", 371365, "values", List.of(Map.of("value", abuturient.getAgent() != null ? abuturient.getAgent().getName() : ""))));
            customFields.add(Map.of("field_id", 371367, "values", List.of(Map.of("value", abuturient.getEducationField() != null ? abuturient.getEducationField().getName() : ""))));
            customFields.add(Map.of("field_id", 371369, "values", List.of(Map.of("value", abuturient.getAppealType() != null ? abuturient.getAppealType().getName() : ""))));
            customFields.add(Map.of("field_id", 371371, "values", List.of(Map.of("value", abuturient.getEducationField() != null ? abuturient.getEducationField().getEducationForm().getName() + " " + abuturient.getEducationField().getEducationForm().getEducationType().getName() : ""))));
            contactUpdate.put("custom_fields_values", customFields);
            restTemplate.exchange(
                    "https://" + AMO_DOMAIN + "/api/v4/contacts/" + contactId,
                    HttpMethod.PATCH,
                    new HttpEntity<>(contactUpdate, headers),
                    String.class
            );
            System.out.println("Lead and contact updated successfully.");
            return true;
        } catch (Exception e) {
            System.out.println("Error updating lead/contact: " + e.getMessage());
            return false;
        }
    }

    private boolean leadStep3(Abuturient abuturient) throws IOException {
        Optional<AbuturientAmocrm> amocrm1 = abuturientAmocrmRepo.findByAbuturientId(abuturient.getId());
        AmoCrmController.Tokens tokens = refreshTokensIfNeeded();
        HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(tokens.getAccessToken());
        if (amocrm1.isEmpty()) {
            // 1️⃣ Create Lead
            Map<String, Object> lead = new HashMap<>();
            lead.put("name", "Kontakt: " + abuturient.getFirstName() + " " + abuturient.getLastName());
            lead.put("pipeline_id", 9193022L);
            lead.put("status_id", 73856170L);

            ResponseEntity<AmoCrmController.AmoLeadResponse> leadResp = restTemplate.exchange(
                    "https://" + AMO_DOMAIN + "/api/v4/leads",
                    HttpMethod.POST,
                    new HttpEntity<>(List.of(lead), headers),
                    AmoCrmController.AmoLeadResponse.class
            );

            Long leadId = leadResp.getBody().getEmbedded().getLeads().get(0).getId();

            // 2️⃣ Create Contact


            // 2️⃣ Update Contact with custom fields using POST
            Map<String, Object> contact = new HashMap<>();
            contact.put("name", abuturient.getFirstName() + " " + abuturient.getLastName());

            List<Map<String, Object>> customFields = new ArrayList<>();

            customFields.add(Map.of(
                    "field_code", "PHONE",
                    "values", List.of(Map.of("value", abuturient.getPhone(), "enum_code", "WORK"))
            ));
            customFields.add(Map.of("field_id", 371363, "values", List.of(Map.of("value", abuturient.getFirstName() + " " + abuturient.getLastName()))));
            customFields.add(Map.of("field_id", 371365, "values", List.of(Map.of("value", abuturient.getAgent() != null ? abuturient.getAgent().getName() : ""))));
            customFields.add(Map.of("field_id", 371367, "values", List.of(Map.of("value", abuturient.getEducationField() != null ? abuturient.getEducationField().getName() : ""))));
            customFields.add(Map.of("field_id", 371369, "values", List.of(Map.of("value", abuturient.getAppealType() != null ? abuturient.getAppealType().getName() : ""))));
            customFields.add(Map.of("field_id", 371371, "values", List.of(Map.of("value", abuturient.getEducationField() != null ? abuturient.getEducationField().getEducationForm().getName() + " " + abuturient.getEducationField().getEducationForm().getEducationType().getName() : ""))));

            contact.put("custom_fields_values", customFields);
            ResponseEntity<AmoCrmController.AmoContactResponse> contactResp = restTemplate.exchange(
                    "https://" + AMO_DOMAIN + "/api/v4/contacts",
                    HttpMethod.POST,
                    new HttpEntity<>(List.of(contact), headers),
                    AmoCrmController.AmoContactResponse.class
            );

            System.out.println(contactResp.getBody());
            Long contactId = contactResp.getBody().getEmbedded().getContacts().get(0).getId();
            System.out.println(contactId);
            // 3️⃣ Link Contact to Lead
            Map<String, Object> linkBody = Map.of(
                    "to_entity_id", contactId,
                    "to_entity_type", "contacts"
            );

            ResponseEntity<Map> linkResp = restTemplate.exchange(
                    "https://" + AMO_DOMAIN + "/api/v4/leads/" + leadId + "/link",
                    HttpMethod.POST,
                    new HttpEntity<>(List.of(linkBody), headers),
                    Map.class
            );
            System.out.println(linkResp.getBody());

            // 4️⃣ Save Lead ID in Database
            AbuturientAmocrm abuturientAmocrm = new AbuturientAmocrm(abuturient, leadId, LocalDateTime.now(), contactId);
            abuturientAmocrmRepo.save(abuturientAmocrm);

            System.out.println("Lead and contact linked successfully.");
            return true;
        }
        AbuturientAmocrm amocrm = amocrm1.get();
        Long leadId = amocrm.getLeadId();
        try {
            // 1️⃣ Update Lead using POST
            Map<String, Object> leadUpdate = new HashMap<>();
            leadUpdate.put("id", leadId);
            leadUpdate.put("pipeline_id", 9193022L);
            leadUpdate.put("name", abuturient.getLastName() + " " + abuturient.getFirstName());
            leadUpdate.put("status_id", 73856170L);
            restTemplate.exchange(
                    String.format("https://%s/api/v4/leads/%d", AMO_DOMAIN, leadId),
                    HttpMethod.PATCH,
                    new HttpEntity<>(leadUpdate, headers),
                    Void.class
            );
            System.out.printf("Lead step3 successfully.");
            return true;
        } catch (Exception e) {
            System.out.println("Error updating lead/contact: " + e.getMessage());
            return false;
        }
    }


    @PutMapping("data-form")
    public HttpEntity<?> updateAbuturientDataForm(@RequestBody AbuturientDTO request) throws IOException {
        Abuturient abuturient = abuturientRepo.findByPhone(request.getPhone());
        if (Objects.isNull(abuturient)) {
            return ResponseEntity.ok(null);
        }
        if (abuturient.getStatus() == 1) {
            abuturient.setStatus(2);
            abuturient.setAppealType(appealTypeRepo.findById(request.getAppealTypeId()).orElseThrow());
            abuturient.setEducationField(educationFieldRepo.findById(request.getEducationFieldId()).orElseThrow());
            abuturient.setEnrolledAt(LocalDateTime.now());
            if (request.getLevel() != null) {
                abuturient.setLevel(abuturient.getLevel());
            }
            abuturientRepo.save(abuturient);
            leadStep2(abuturient);
        }
        return ResponseEntity.ok(abuturient);
    }


    @PutMapping
    public HttpEntity<?> updateAbuturient(@RequestBody AbuturientDTO request) throws IOException {
        System.out.println(request);
        Abuturient abuturient = abuturientRepo.findByPhone(request.getPhone());
        if (Objects.isNull(abuturient)) {
            return ResponseEntity.ok(null);
        }
        if (abuturient.getStatus() == 0) {
            District district = null;
            Optional<District> byId = districtRepo.findById(request.getDistrictId());
            if (byId.isPresent()) {
                district = byId.get();
            }
            abuturient.setStatus(1);
            abuturient.setFirstName(request.getFirstName());
            abuturient.setLastName(request.getLastName());
            abuturient.setPhone(request.getPhone());
            abuturient.setFatherName(request.getFatherName());
            abuturient.setMotherName(request.getMotherName());
            abuturient.setAppealType(appealTypeRepo.findById(request.getAppealTypeId()).orElseThrow());
            abuturient.setEducationField(educationFieldRepo.findById(request.getEducationFieldId()).orElseThrow());
            abuturient.setEnrolledAt(LocalDateTime.now());
            abuturient.setPassportNumber(request.getPassportNumber());
            abuturient.setPassportPin(request.getPassportPin());
            if (request.getLevel() != null) {
                abuturient.setLevel(abuturient.getLevel());
            }
            abuturient.setDistrict(district);

            abuturientRepo.save(abuturient);
            leadStep2(abuturient);
        }
        return ResponseEntity.ok(abuturient);
    }


    @GetMapping("/{phone}")
    public HttpEntity<?> getAllAbuturient(@PathVariable String phone) {
        Abuturient abuturient = abuturientRepo.findByPhone(phone);
        if (Objects.isNull(abuturient)) {
            System.out.println(1);
            return ResponseEntity.ok(null);
        }
        if (abuturient.getStatus() == 0) {
            System.out.println(2);

            return ResponseEntity.ok(abuturient);
        }
        System.out.println(abuturient);

        return ResponseEntity.ok(abuturient);

    }


    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private AmoCrmController.Tokens refreshTokensIfNeeded() throws IOException {
        AmoCrmController.Tokens tokens = loadTokens();
        if (System.currentTimeMillis() >= tokens.getExpiresAt() - 60000) {
            Map<String, String> request = new HashMap<>();
            request.put("client_id", CLIENT_ID);
            request.put("client_secret", CLIENT_SECRET);
            request.put("grant_type", "refresh_token");
            request.put("refresh_token", tokens.getRefreshToken());
            request.put("redirect_uri", REDIRECT_URI);


            ResponseEntity<AmoCrmController.Tokens> response = restTemplate.postForEntity(
                    "https://" + AMO_DOMAIN + "/oauth2/access_token",
                    request,
                    AmoCrmController.Tokens.class
            );

            saveTokens(response.getBody());
            return response.getBody();
        }
        return tokens;
    }

    private AmoCrmController.Tokens loadTokens() throws IOException {
        if (!Files.exists(Paths.get(TOKEN_PATH))) {
            throw new FileNotFoundException("Файл токенов не найден");
        }
        try (InputStream is = new FileInputStream(TOKEN_PATH)) {
            return objectMapper.readValue(is, AmoCrmController.Tokens.class);
        }
    }

    private void saveTokens(AmoCrmController.Tokens tokens) throws IOException {
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
        private AmoCrmController.AmoLeadResponse.EmbeddedLeads embedded;

        @Data
        public static class EmbeddedLeads {
            private List<AmoCrmController.AmoLeadResponse.Lead> leads;
        }

        @Data
        public static class Lead {
            private Long id;
        }
    }

    @Data
    public static class AmoContactResponse {
        @JsonProperty("_embedded")
        private AmoCrmController.AmoContactResponse.EmbeddedContacts embedded;

        @Data
        public static class EmbeddedContacts {
            private List<AmoCrmController.AmoContactResponse.Contact> contacts;
        }

        @Data
        public static class Contact {
            private Long id;
        }
    }

    @GetMapping("/contract/{phone}")
    public void getAllAbuturientContract(@PathVariable String phone, HttpServletResponse response) throws IOException {
        Abuturient abuturient = abuturientRepo.findByPhone(phone);
        abuturient.setStatus(4);
        abuturientRepo.save(abuturient);
        if (abuturient == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        leadStep3(abuturient);
        Document document = new Document(PageSize.A4);
        String filePath = "./Contract_" + phone + ".pdf";


        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font boldFont = new Font(Font.FontFamily.TIMES_ROMAN, 8, Font.BOLD);
            Font regularFont = new Font(Font.FontFamily.TIMES_ROMAN, 8, Font.NORMAL);


            // Header
            Paragraph paragraph1 = new Paragraph("SHARTNOMA Nº K-" + abuturient.getContractNumber(), boldFont);
            paragraph1.setAlignment(Element.ALIGN_CENTER);
            paragraph1.setSpacingBefore(5f);
            document.add(paragraph1);

            Paragraph paragraph = new Paragraph("Ta'lim xizmatlarini ko'rsatish uchun", boldFont);
            paragraph.setAlignment(Element.ALIGN_CENTER);
            paragraph.setSpacingAfter(5f);
            document.add(paragraph);


//// 3 ta ustundan iborat jadval yaratish
//                PdfPTable headerTable = new PdfPTable(4); // 4 ta ustun
//                headerTable.setWidthPercentage(100);
//                headerTable.setWidths(new int[]{1, 2, 3, 2}); // Yangi nisbatlar: logo, sana, to'lov, QR
//
//                PdfPTable headerTable = new PdfPTable(3);
//                headerTable.setWidthPercentage(100);
//                headerTable.setWidths(new int[]{1, 2, 3}); // Ustun kengliklari nisbati
//// Generate QR code image
//                try {
//                    String rawPhone = abuturient.getPhone();
//                    String encodedPhone = URLEncoder.encode(rawPhone, StandardCharsets.UTF_8.toString());
//                    String qrContent = "https://qabul.bxu.uz/api/v1/abuturient/contract/" + encodedPhone;
//
////                    String qrContent = "https://qabul.bxu.uz/api/v1/abuturient/contract/" + phone;
//
//                    int qrSize = 100;
//
//                    BitMatrix bitMatrix = new MultiFormatWriter().encode(qrContent, BarcodeFormat.QR_CODE, qrSize, qrSize);
//                    ByteArrayOutputStream qrBaos = new ByteArrayOutputStream();
//                    MatrixToImageWriter.writeToStream(bitMatrix, "png", qrBaos);
//                    Image qrImage = Image.getInstance(qrBaos.toByteArray());
//                    qrImage.scaleToFit(80, 80);
//
//                    PdfPCell qrCell = new PdfPCell(qrImage);
//                    qrCell.setColspan(3); // span across all 3 columns
//                    qrCell.setBorder(Rectangle.NO_BORDER);
//                    qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
//                    qrCell.setPaddingTop(5f);
//
//                    headerTable.addCell(qrCell);
//                } catch (Exception qrEx) {
//                    PdfPCell errorCell = new PdfPCell(new Paragraph("QR code not available", regularFont));
//                    errorCell.setColspan(3);
//                    errorCell.setBorder(Rectangle.NO_BORDER);
//                    errorCell.setHorizontalAlignment(Element.ALIGN_CENTER);
//                    headerTable.addCell(errorCell);
//                }
//
////*** 1-Ustun: Logo ***/
//                PdfPCell logoCell = new PdfPCell();
//                logoCell.setBorder(Rectangle.BOX);
//                logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
//                logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//
//                try {
//                    // Logoni qo'shish
////                    Image logo = Image.getInstance(System.getProperty("user.home") + "/Downloads/logo.png");
//                    Image logo = Image.getInstance("./logo.png");
//                    logo.scaleToFit(50, 50); // Rasm hajmini moslashtirish
//                    logoCell.addElement(logo);
//                } catch (Exception e) {
//                    logoCell.addElement(new Paragraph("Logo not found", regularFont));
//                }
//
//// Jadvalga LOGO hujayrasini qo‘shish
//                headerTable.addCell(logoCell);
//
////*** 2-Ustun: Sana haqida ma’lumot ***/
//                PdfPCell dateCell = new PdfPCell();
//                dateCell.setBorder(Rectangle.BOX);
//                dateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
//                dateCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//
//// "Shartnoma berilgan sana" qo'shish
//                LocalDate today = LocalDate.now();
//                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
//                String formattedDate = today.format(formatter);
//                dateCell.addElement(new Paragraph("Shartnoma berilgan sana:", regularFont));
//                dateCell.addElement(new Paragraph(formattedDate + " yil", regularFont));
//
//// Jadvalga SANA hujayrasini qo‘shish
//                headerTable.addCell(dateCell);
//
///*** 3-Ustun: To‘lov haqida ma’lumot ***/
//                PdfPCell paymentCell = new PdfPCell();
//                paymentCell.setBorder(Rectangle.BOX);
//                paymentCell.setHorizontalAlignment(Element.ALIGN_CENTER);
//                paymentCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//
//// To‘lov ma'lumotlarini qo‘shish
//                String fullName = abuturient.getLastName().toUpperCase() + " " +
//                        abuturient.getFirstName().toUpperCase() + " " +
//                        abuturient.getFatherName().toUpperCase();
//
//                paymentCell.addElement(new Paragraph("TO'LOV UCHUN!!!", boldFont));
//                paymentCell.addElement(new Paragraph("SHARTNOMA Nº K-" + abuturient.getContractNumber() + " shartnomaga asosan", regularFont));
//                paymentCell.addElement(new Paragraph(fullName + "ning kontrakt puli ko'chirildi", regularFont));
//
//// Jadvalga TO‘LOV hujayrasini qo‘shish
//                headerTable.addCell(paymentCell);
//
//// Jadvalni hujjatga qo‘shish
//                document.add(headerTable);
//
//


            PdfPTable headerTable = new PdfPTable(4);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new int[]{1, 2, 3, 2}); // Column ratios: logo, date, payment, QR

// ====== 1. LOGO CELL ======
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.BOX);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            try {
                Image logo = Image.getInstance("./logo.png");
                logo.scaleToFit(50, 50);
                logoCell.addElement(logo);
            } catch (Exception e) {
                logoCell.addElement(new Paragraph("Logo not found", regularFont));
            }
            headerTable.addCell(logoCell);

// ====== 2. DATE CELL ======
            PdfPCell dateCell = new PdfPCell();
            dateCell.setBorder(Rectangle.BOX);
            dateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            dateCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            String formattedDate = today.format(formatter);
            dateCell.addElement(new Paragraph("Shartnoma berilgan sana:", regularFont));
            dateCell.addElement(new Paragraph(formattedDate + " yil", regularFont));
            headerTable.addCell(dateCell);

// ====== 3. PAYMENT CELL ======
            PdfPCell paymentCell = new PdfPCell();
            paymentCell.setBorder(Rectangle.BOX);
            paymentCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            paymentCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            String fullName = abuturient.getLastName().toUpperCase() + " " +
                    abuturient.getFirstName().toUpperCase() + " " +
                    abuturient.getFatherName().toUpperCase();

            paymentCell.addElement(new Paragraph("TO'LOV UCHUN!!!", boldFont));
            if (abuturient.getPassportPin() != null) {
                paymentCell.addElement(new Paragraph("JSHSHIR:" + abuturient.getPassportPin(), regularFont));
            }
            paymentCell.addElement(new Paragraph("SHARTNOMA Nº K-" + abuturient.getContractNumber() + " shartnomaga asosan", regularFont));
            paymentCell.addElement(new Paragraph(fullName + "ning kontrakt puli ko'chirildi", regularFont));
            headerTable.addCell(paymentCell);

// ====== 4. QR CODE CELL ======
            PdfPCell qrCell = new PdfPCell();
            qrCell.setBorder(Rectangle.BOX);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            try {
                String rawPhone = abuturient.getPhone();
                String encodedPhone = URLEncoder.encode(rawPhone, StandardCharsets.UTF_8.toString());
                String qrContent = "https://qabul.bxu.uz/api/v1/abuturient/contract/" + encodedPhone;

                int qrSize = 100;
                BitMatrix bitMatrix = new MultiFormatWriter().encode(qrContent, BarcodeFormat.QR_CODE, qrSize, qrSize);
                ByteArrayOutputStream qrBaos = new ByteArrayOutputStream();
                MatrixToImageWriter.writeToStream(bitMatrix, "png", qrBaos);
                Image qrImage = Image.getInstance(qrBaos.toByteArray());
                qrImage.scaleToFit(50, 50);

                qrCell.addElement(qrImage);
            } catch (Exception qrEx) {
                qrCell.addElement(new Paragraph("QR code not available", regularFont));
            }
            headerTable.addCell(qrCell);

// Add table to document
            document.add(headerTable);


            Paragraph paragraph2 = new Paragraph("O'zbekiston Respublikasi Prezidentining 2021 yil 22-iyundagi PQ-5157 son qarori, Vazirlar Mahkamasining 2017 yil 20 -iyundagi 393-son qarori va universitet kengashining tegishli qarori asosida, bir tomondan BUXORO XALQARO UNIVERSITERI (keyingi o' rinlarda \"Ta'lim muassasasi\") nomidan rektor Baratov Sharif Ramazonovich , ikkinchi tomondan talabalikka tavsiya etilgan talabgor " + fullName + " (keyingi o'rinlarda \"Ta'lim oluvchi\"), birgalikda ”tomonlar” deb ataladigan shaxslar mazkur shartnomani quyidagicha tuzdilar.", regularFont);
            paragraph2.setSpacingBefore(2f);
            document.add(paragraph2);
            //                -----
            Paragraph paragraph3 = new Paragraph("I. SHARTNOMA PREDMETI", boldFont);
            paragraph3.setSpacingBefore(2f);
            paragraph3.setAlignment(Element.ALIGN_CENTER);
            document.add(paragraph3);
            //    -----------
            Paragraph paragraph4 = new Paragraph("        1.1 Ta'lim muassasasi ta'lim xizmatini ko'rsatishni, Ta'lim oluvchi o'qish uchun belgilangan to'lovni o'z vaqtida amalga oshirishni va tasdiqlangan o'quv reja bo'yicha darslarga to'liq qatnashishni o'z zimmalariga oladi. Ta'lim oluvchining ta'lim ma'lumotlari quyidagicha:", regularFont);
            paragraph4.setSpacingAfter(2f);
            document.add(paragraph4);

            //   -----------------
            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.setWidths(new int[]{3, 1});
            PdfPCell leftDetailCell = new PdfPCell();
            leftDetailCell.setBorder(Rectangle.BOX);
            leftDetailCell.addElement(new Paragraph("Ta'lim bosqichi: " + abuturient.getEducationField().getEducationForm().getEducationType().getName(), regularFont));
            leftDetailCell.addElement(new Paragraph("Ta'lim shakli: " + abuturient.getEducationField().getEducationForm().getName(), regularFont));
            leftDetailCell.addElement(new Paragraph("Ta'lim yo'nalishi: " + abuturient.getEducationField().getName(), regularFont));
            detailsTable.addCell(leftDetailCell);

            PdfPCell rightDetailCell = new PdfPCell();
            rightDetailCell.setBorder(Rectangle.BOX);

// Get the education duration (number of years)
            int educationDuration = abuturient.getEducationField().getEducationDuration();

// Base year for the first course
            int startYear = 2025;

// Loop through the duration and add each course year
            Integer level = abuturient.getLevel();
            if (level == null) {
                level = 1;

            }
            for (int i = level; i <= educationDuration; i++) {
                int endYear = startYear + 1; // End year is start year + 1
                String courseYear = i + "-kurs: " + startYear + "-" + endYear + " o'quv yili";
                rightDetailCell.addElement(new Paragraph(courseYear, regularFont));
                startYear = endYear; // Update the start year for the next course
            }

// Add the cell to the table
            detailsTable.addCell(rightDetailCell);

// Add the table to the document
            document.add(detailsTable);


            //   -----------------
            Paragraph paragraph6 = new Paragraph("        1.2 ”Ta'lim muassasasi\"ga o'qishga qabul qilingan ”Ta'lim oluvchi”lar O'zbekiston Respublikasining ”Ta'lim to'g'risida”gi Qonuni va davlat ta'lim standartlarga muvofiq ishlab chiqilgan o'quv rejalar va fan dasturlari asosida ta'lim oladilar.", regularFont);
            paragraph6.setSpacingAfter(2f);
            document.add(paragraph6);
            //   -----------------
            Paragraph paragraph5 = new Paragraph("II. TA'LIM XIZMATINI KO'RSATISH NARXI, TO'LASH MUDDATI VA TARTIBI", boldFont);
            paragraph5.setSpacingBefore(2f);
            paragraph5.setAlignment(Element.ALIGN_CENTER);
            document.add(paragraph5);
            //    -----------
            Paragraph paragraph7 = new Paragraph("        2.1 ”Ta'lim muassasasi”da o'qish davrida ta'lim xizmatini ko'rsatish narxi universitet haqiqiy xarajatlarining kalkulyatsiyasi asosida hisoblanadi.\n" +
                    "        2.2 Ushbu shartnoma bo'yicha ta'lim oluvchini bir o'quv yili davomida o'qitish uchun to'lov miqdori " + abuturient.getEducationField().getPrice().toString() + "  so'mni (stipendiyasiz) tashkil etadi va ushbu to'lov miqdorining 50 % har o'quv yilining 1-oktyabr kuniga qadar, qolgan qismi esa keyingi yilning 1-mayigacha to'lanishi shart. Bunda to'lov miqdorini hisoblash ta'lim oluvchini talabalikka qabul qilingan kundan boshlab hisoblanadi.\n" +
                    "        2.3 Talabalar orasidan joriy o'quv yili (sentyabrdan-iyul oyigacha) davrida o'qishini boshqa davlat va nodavlat oliy ta'lim muassasalariga ko'chirishi yoki o'z xoxishiga binoan talabalik safidan chetlashtirish maqsadida murojaat qilganda, shartnomada belgilangan bir yillik to'lov-kontrakt miqdori O'zbekiston Respublikasi Oliy va o'rta maxsus ta'lim vazirining 2012 yil 28 dekabr kunidagi 508- sonli buyrug'ida ko'rsatilgan tartibda talabadan saqlanib qolinadi.", regularFont);
            paragraph7.setSpacingAfter(2f);
            document.add(paragraph7);
            //   -----------------


            //   -----------------
            Paragraph paragraph8 = new Paragraph("III. TOMONLARNING MAJBURIYATLARI", boldFont);
            paragraph8.setSpacingBefore(2f);
            paragraph8.setAlignment(Element.ALIGN_CENTER);
            document.add(paragraph8);
            //    -----------
            Paragraph paragraph9 = new Paragraph("   3.1. Ta'lim muassasasi majburiyatlari:\n" +
                    "         -O'qitish uchun belgilangan dastlabki to'lov miqdorini (50% dan kam bo'lmagan) amalga oshirgandan so'ng, ”Ta'lim oluvchi”ni buyruq asosida talabalikka qabul qilish;\n" +
                    "         -Ta'lim oluvchi kontrakt to'lovini amalga oshirgandan so'ng ”Talabalikka qabul qilish” burug'i o'quv jarayonlari boshlangan kundan chiqariladi.\n" +
                    "         -Ta'lim oluvchiga o'qishi uchun O'zbekiston Respublikasining ”Ta'lim to'g'risida”gi Qonuni va ”Ta'lim muassasasi” Ustavida nazarda tutilgan zarur shart-sharoitlarga muvofiq sharoitlarni yaratib berish;\n" +
                    "         -Ta'lim oluvchining huquq va erkinliklari, qonuniy manfaatlari hamda ta'lim muassasasi Ustaviga muvofiq professor o'qituvchilar tomonidan o'zlarining funksional vazifalarini to'laqonli bajarishini ta'minlash;\n" +
                    "         -Ta'lim oluvchini tahsil olayotgan ta'lim yo'nalishi (mutaxassisligi) bo'yicha tasdiqlangan o'quv rejasi va dasturlariga muvofiq davlat ta'lim standarti talablari darajasida tayyorlash;\n" +
                    "         -Respublikada belgilangan Mehnatga haq to'lashning eng kam miqdori yoki sifatli ta'lim xizmatlari ko'rsatish bilan bog'liq tariflar o'zgargan taqdirda o'qitish uchun belgilangan to'lov miqdori universitet kengashi qarori asosida ta'lim oluvchini 1 oy oldin xabardor qilish.", regularFont);
            paragraph9.setSpacingAfter(2f);
            document.add(paragraph9);
            //    -----------
            Paragraph paragraph10 = new Paragraph("   3.2. Ta'lim oluvchining majburiyatlari:\n" +
                    "         -Shartnomaning 2.2. bandida belgilangan to'lov summasini shu bandda ko'rsatilgan muddatlarda to'lab borish;\n" +
                    "         -Respublikada belgilangan Mehnatga haq to'lashning eng kam miqdori yoki tariflar o'zgarishi natijasida o'qitish uchun belgilangan to'lov miqdori o'zgargan taqdirda, o'qishning qolgan muddati uchun ta'lim muassasasiga haq to'lash bo'yicha bir oy muddat ichida shartnomaga qo'shimcha bitim rasmiylashtirish va to'lov farqini to'lash;\n" +
                    "         -Ta'lim oluvchi o'qitish uchun belgilangan to'lov miqdorini to'laganlik to'g'risidagi bank tasdiqnomasi va shartnomaning bir nusxasini o'z vaqtida hujjatlarni rasmiylashtirish uchun ta'lim muassasasiga topshirish;\n" +
                    "         -Tahsil olayotgan ta'lim yo'nalishining (mutaxassisligining) tegishli malaka tavsifnomasiga muvofiq kelajakda mustaqil faoliyat yuritishga zarur bo'lgan barcha bilimlarni egallash, dars va mashg'ulotlarga to'liq qatnashish;", regularFont);
            paragraph10.setSpacingAfter(2f);
            document.add(paragraph10);


            //   -----------------
            Paragraph paragraph11 = new Paragraph("IV. TOMONLARNING HUQUQLARI", boldFont);
            paragraph11.setSpacingBefore(3f);
            paragraph11.setAlignment(Element.ALIGN_CENTER);
            document.add(paragraph11);
            //   -----------------
            Paragraph paragraph12 = new Paragraph("    4.1. Talim muassasasi huquqlari:\n" +
                    "         -O'quv jarayonini mustaqil ravishda amalga oshirish, ”Ta'lim oluvchi”ning oraliq va yakuniy nazoratlarni topshirish, qayta topshirish tartibi hamda vaqtlarini belgilash;\n" +
                    "         -O'zbekiston Respublikasi qonunlari, ”Ta'lim muassasasi” nizomi hamda mahalliy normativ-huquqiy hujjatlarga muvofiq ”Ta'lim oluvchi\"ga rag'batlantiruvchi yoki intizomiy choralarni qo'llash;\n" +
                    "         -Agar ”Ta'lim oluvchi” o'quv yili semestrlarida yakuniy nazoratlarni topshirish, qayta topshirish natijalariga ko'ra akademik qarzdor bo'lib qolgan taqdirda, mazkur talabani kredit-modul tizimi talablari asosida rasmiy ogohlantirish;\n" +
                    "         -Ta'lim muassasasi, ”Ta'lim oluvchi\"ning darslarga sababsiz qatnashmaslik, intizomni buzish, ”Ta'lim muassasasi\"ning ichki tartib qoidalariga amal qilmaganda, respublikaning normativ-huquqiy hujjatlarida nazarda tutilgan boshqa sabablarga ko'ra hamda o'qitish uchun belgilangan to'lov o'z vaqtida amalga oshirilmaganda ”Ta'lim oluvchi”ni talabalar safidan chetlashtirish huquqiga ega. Ta'lim oluvchi o'qishini boshqa oliy ta'lim muassasasiga ko'chirmoqchi bo'lganda faqatgina \"Davlat oliygohlariga\" belgilangan tartibda ya'ni O'zbekiston Respublikasi Vazirlar Mahkamasining 2017 yil 393-sonli Qarorining 3-ilovasi III-bobida ko'rsatilgan tartibda amalga oshiriladi", regularFont);
            paragraph12.setSpacingAfter(2f);
            document.add(paragraph12);
            //   -----------------
            Paragraph paragraph13 = new Paragraph("         -Talaba o'quv intizomiy va oliy ta'lim muassasasining ichki tartib-qoidalarini buzganligi, bir semester davomida darslarni uzrli sabablarsiz 74 (yetmish to'rt) soatdan ortiq qoldirganligi yoki o'qitish uchun belgilangan miqdordagi to'lovni o'z vaqtida amalga oshirmaganligi sababli talabalar safidan chetlashtirilganda yoxud belgilangan muddatlarda fanlarni o'zlashtira olmaganligi (akademik qarzdor bo'lganligi) sababli kursdan qoldirilganda oliy ta'lim muassasasi tomonidan uning tegishli o'quv semestri uchun amalga oshirilgan to'lovi qaytarib berilmaydi.\n" +
                    "         -Bunda, oshirilgan to'lov-kontrakt asosida talabalikka qabul qilinganlar birinchi kursning birinchi semestri davomida talabalar safidan chetlashtirilganda ular amalga oshirilgan to'lovning 50 % qaytarib berilmaydi.\n" +
                    "         -Belgilangan muddatlarda fanlarni o'zlashtira olmagan (akademik qarzdor bo'lgan) talaba kursdan qoldirilganda to'lov miqdori o'qishi davom etadigan o'quv semestri uchun amalga oshiriladi.\n" +
                    "    4.2. Ta'lim oluvchining huquqlari:\n" +
                    "         -O'quv yili uchun shartnoma summasini semestrlarga yoki choraklarga bo'lmasdan bir yo'la to'liqligicha to'lash;\n" +
                    "         -Ta'lim oluvchi mazkur kontrakt bo'yicha naqd pul, bank plastik kartasi, bankdagi omonat hisob raqami orqali, ish joyidan arizasiga asosan oylik maoshini o'tkazishi yoki banklardan ta'lim krediti olish orqali to'lovni amalga oshirish;", regularFont);
            paragraph13.setSpacingAfter(2f);
            document.add(paragraph13);


            //   -----------------
            Paragraph paragraph_5 = new Paragraph("V. SHARTNOMANING AMAL QILISH MUDDATI, UNGA O'ZGARTIRISH VA QO'SHIMCHALAR KIRITISH HAMDA BEKOR QILISH TARTIBI", boldFont);
            paragraph_5.setSpacingBefore(3f);
            paragraph_5.setAlignment(Element.ALIGN_CENTER);
            document.add(paragraph_5);

            Paragraph paragraph_5_b = new Paragraph(
                    "        5.1. Ushbu shartnoma ikki tomonlama imzolangandan so'ng kuchga kiradi hamda o'quv davri tugagunga qadar amalda bo'ladi.\n" +
                            "        5.2. Ushbu shartnoma shartlariga ikkala tomon kelishuviga asosan tuzatish, o'zgartirish va qo'shimchalar kiritilishi mumkin.\n" +
                            "        5.3. Shartnomaga tuzatish, o'zgartirish va qo'shimchalar faqat yozma ravishda ”Shartnomaga qo'shimcha bitim” tarzida kiritiladi va imzolanadi.\n" +
                            "        5.4. Shartnoma quyidagi hollarda bekor qilinishi mumkin:\n" +
                            "        -Tomonlarning o'zaro kelishuviga binoan;\n" +
                            "        -Ta'lim oluvchi” talabalar safidan chetlashtirganda; O'zbekiston Respublikasi Vazirlar Mahkamasining 2017 yil 393-sonli Qarorining tegishli bandlari bo'yicha.\n" +
                            "        -Tomonlardan biri o'z majburiyatlarini bajarmaganda yoki lozim darajada bajarmaganda;\n" +
                            "        -Ta'lim oluvchi” tomonidan taqdim etilgan xujjatlarda qalbakilik xolatlari aniqlanganda;\n" +
                            "        -Talabgor (talaba) o'qishini boshqa oliy ta'lim muassasasidan (respublika va xorijiy OTM) o'qishini ko'chirishda mazkur universitet qabul komissiyasiga taqdim qilgan xujjatlarda qalbakilik yoki noqonuniy xolatlar aniqlanganda ushbu shartnoma bekor qilinadi va talabaga xabar beriladi”\n" +
                            "        -Ta'lim oluvchi”ning tashabbusiga ko'ra;\n" +
                            "        -Ta'lim muassasi” tugatilganda, muassasa tomonidan ko'rsatilmagan ta'lim xizmati uchungina ta'lim oluvchi bilan amaldagi qonunchilik asosida hisob-kitob qilinadi.", regularFont);
            paragraph_5_b.setSpacingAfter(2f);
            document.add(paragraph_5_b);

            //   -----------------
            Paragraph paragraph_6 = new Paragraph("VI. YAKUNIY QOIDALAR VA NIZOLARNI HAL QILISH TARTIBI", boldFont);
            paragraph_6.setSpacingBefore(3f);
            paragraph_6.setAlignment(Element.ALIGN_CENTER);
            document.add(paragraph_6);
            Paragraph paragraph_6_b = new Paragraph(" 6.1. Ushbu shartnomani bajarish jarayonida kelib chiqishi mumkin bo'lgan nizo va ziddiyatlar tomonlar o'rtasida muzokaralar olib borish yo'li bilan hal etiladi.\n" +
                    "        6.2. Muzokaralar olib borish yo'li bilan nizoni hal etish imkoniyati bo'lmagan taqdirda, tomonlar nizolarni hal etish uchun amaldagi qonunchilikka muvofiq sudga murojaat etishlari mumkin.\n" +
                    "        6.3. Ta'lim muassasasi” axborotlar va xabarnomalarni internetdagi veb-saytida, axborot tizimida yoki e'lonlar taxtasida e'lon joylashtirish orqali xabar berishi mumkin.\n" +
                    "        6.4. Shartnoma 2 (ikki) nusxada, tomonlarning har biri uchun bir nusxadan tuziladi va ikkala nusxa ham bir xil huquqiy kuchga ega.\n" +
                    "        6.5. . Ushbu shartnomaga qo'shimcha bitim kiritilgan taqdirda, barcha kiritilgan qo'shimcha bitimlar shartnomaning ajralmas qismi hisoblanadi.\n" +
                    "        6.6. Qabul komissiyasiga taqdim etilgan hujjatlarning haqqoniyligi o'rnatilgan tartibda tasdiqlangandan so'ng talabgor tegishli buyruq asosida talabalar safiga qabul qilinadi.", regularFont);
            paragraph_6_b.setSpacingAfter(2f);
            document.add(paragraph_6_b);


            //   -----------------
            Paragraph paragraph_7 = new Paragraph("VII. TOMONLARNING REKVIZITLARI VA IMZOLARI", boldFont);
            paragraph_7.setSpacingBefore(3f);
            paragraph_7.setSpacingAfter(3f);
            paragraph_7.setAlignment(Element.ALIGN_CENTER);
            document.add(paragraph_7);

//   -----------------
// Jadval yaratish (2 ta ustun)
            PdfPTable endTable = new PdfPTable(2);
            endTable.setWidthPercentage(100);
            endTable.setWidths(new int[]{3, 1}); // Ustun kengliklari

// CHAP USTUN
            PdfPCell leftCell1 = new PdfPCell();
            leftCell1.setBorder(Rectangle.BOX);

// Institut haqida ma'lumot qo‘shish
            leftCell1.addElement(new Paragraph("BUXORO XALQARO UNIVERSITETI", boldFont));
            leftCell1.addElement(new Paragraph("Manzil: Buxoro viloyati, Kogon tumani, B.Naqshband M.F.Y Abay ko'chasi 20 uy", regularFont));
            leftCell1.addElement(new Paragraph("Telefon raqami: 55-309-99-99, 99-773-17-37, 94-322-5775", regularFont));
            leftCell1.addElement(new Paragraph("STIR: 308196898", regularFont));
            leftCell1.addElement(new Paragraph("MFO: 00873", regularFont));
            leftCell1.addElement(new Paragraph("Bank nomi: 'Asaka bank' AJ Buxoro BXM", regularFont));
            UUID specificAgentId = UUID.fromString("c595703d-7d81-4476-8f0b-d75f00cf907e");
            LocalDateTime createdAt = abuturient.getCreatedAt();

            if (createdAt != null &&
                    (abuturient.getAgent() == null || specificAgentId.equals(abuturient.getAgent().getId())) &&
                    createdAt.toLocalDate().isAfter(LocalDate.of(2025, 7, 12))) {

                leftCell1.addElement(new Paragraph("Hisob raqami: 20208000305439719002", regularFont));

            } else {

                leftCell1.addElement(new Paragraph("Hisob raqami: 20208000105439719001", regularFont));
            }


// Rektor matnini qo‘shish
            PdfPTable rectorTable = new PdfPTable(2); // 2 ta ustunli ichki jadval (matn + logo)
            rectorTable.setWidths(new float[]{1, 1}); // Ustun kengligi 50% - 50%

            PdfPCell rectorTextCell = new PdfPCell(new Paragraph("Rektor: Sh.R.Barotov", regularFont));
            rectorTextCell.setBorder(Rectangle.NO_BORDER);
            rectorTextCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            rectorTable.addCell(rectorTextCell);

// Logo rasmni joylashtirish
            PdfPCell logoCell1 = new PdfPCell(); // Yangi nom
            logoCell1.setBorder(Rectangle.NO_BORDER);
            logoCell1.setHorizontalAlignment(Element.ALIGN_LEFT);
            logoCell1.setVerticalAlignment(Element.ALIGN_MIDDLE);

            try {
//                    Image logo1 = Image.getInstance(System.getProperty("user.home") + "/Downloads/logo1.png");
                Image logo1 = Image.getInstance("./logo1.png");
                logo1.scaleToFit(100, 100); // Logoni o‘lchamini moslashtirish
                logoCell1.addElement(logo1); // TO‘G‘RI ISHLATISH
            } catch (Exception e) {
                logoCell1.addElement(new Paragraph("Logo not found", regularFont));
            }

            rectorTable.addCell(logoCell1);

// Ichki jadvalni asosiy jadvalga qo‘shish
            leftCell1.addElement(rectorTable);
            endTable.addCell(leftCell1);

// O‘NG USTUN (Talaba ma‘lumotlari)
            PdfPCell rightCell1 = new PdfPCell();
            rightCell1.setBorder(Rectangle.BOX);
            rightCell1.addElement(new Paragraph("FISH: " + fullName, boldFont));
            rightCell1.addElement(new Paragraph("Men shartnoma bilan to'liq tanishdim", regularFont));
            rightCell1.addElement(new Paragraph("F.I.O: _____", regularFont));
            rightCell1.addElement(new Paragraph("Ta'lim oluvchining imzosi: (_____)", regularFont));
            endTable.addCell(rightCell1);

// Jadvalni hujjatga qo‘shish
            document.add(endTable);


            // Close document
            document.close();

            // Save the document to the file system
            try (FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
                outputStream.writeTo(fileOutputStream);
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // Send the file as a response
        File file = new File(filePath);
        abuturient.setStatus(4);
        abuturientRepo.save(abuturient);
        response.setHeader("Content-Type", "application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());

        try {
            response.getOutputStream().write(java.nio.file.Files.readAllBytes(file.toPath()));
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }


}
