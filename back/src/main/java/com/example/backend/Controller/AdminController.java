package com.example.backend.Controller;

import com.example.backend.DTO.AbuturientPostDTO;
import com.example.backend.Entity.*;
import com.example.backend.Enums.UserRoles;
import com.example.backend.Repository.*;
import com.example.backend.Services.AuthService.AuthService;
import com.example.backend.Services.ExcelExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final UserRepo userRepo;
    private final AbuturientRepo abuturientRepo;
    private final RoleRepo roleRepo;
    private final AuthService service;
    private final EducationFieldRepo educationFieldRepo;
    private final AppealTypeRepo appealTypeRepo;
    private final HistoryRepo historyRepo;
    private final ExcelExportService excelExportService;
//    @GetMapping("/appeals")
//    public HttpEntity<?> getAbuturients() {
//        return ResponseEntity.ok(abuturientRepo.findAll());
//    }

    @GetMapping("/appeals")
    public ResponseEntity<?> getAbuturients(
            @RequestParam(required = false, defaultValue = "") String firstName,
            @RequestParam(required = false, defaultValue = "") String lastName,
            @RequestParam(required = false, defaultValue = "") String fatherName,
            @RequestParam(required = false, defaultValue = "") String passportNumber,
            @RequestParam(required = false, defaultValue = "") String passportPin,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam(required = false) Integer appealTypeId,
            @RequestParam(required = false) Integer educationFieldId,
            @RequestParam(required = false) UUID agentId,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate createdAt,
            @PageableDefault(size = 2, page = 0) Pageable pageable) {
        System.out.println(createdAt);
        System.out.println(firstName);

        Page<Abuturient> abuturients = abuturientRepo.findByFilters(
                firstName,
                passportNumber,
                passportPin,
                phone,
                appealTypeId,
                educationFieldId,
                agentId,
                createdAt,
                pageable);
        return ResponseEntity.ok(abuturients);
    }


    @GetMapping("/appeals/excel")
    public void exportAbuturientsToExcel(
            @RequestParam(required = false, defaultValue = "") String firstName,
            @RequestParam(required = false, defaultValue = "") String lastName,
            @RequestParam(required = false, defaultValue = "") String fatherName,
            @RequestParam(required = false, defaultValue = "") String passportNumber,
            @RequestParam(required = false, defaultValue = "") String passportPin,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam(required = false) Integer appealTypeId,
            @RequestParam(required = false) Integer educationFieldId,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate createdAt,
            HttpServletResponse response) throws IOException {

        // Generate the Excel file
        ByteArrayInputStream in = excelExportService.exportToExcel(
                firstName, lastName, fatherName, passportNumber, passportPin, phone,
                appealTypeId, educationFieldId, agentId, createdAt);

        // Set response headers
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=abiturients.xlsx");

        // Write the Excel file to the response output stream
        FileCopyUtils.copy(in, response.getOutputStream());
    }


    @GetMapping("/appeals/transform")
    public ResponseEntity<?> getAbuturientsTransform(
            @RequestParam(required = false, defaultValue = "") String firstName,
            @RequestParam(required = false, defaultValue = "") String lastName,
            @RequestParam(required = false, defaultValue = "") String fatherName,
            @RequestParam(required = false, defaultValue = "") String passportNumber,
            @RequestParam(required = false, defaultValue = "") String passportPin,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam(required = false) Integer appealTypeId,
            @RequestParam(required = false) Integer educationFieldId,
            @RequestParam(required = false) UUID agentId,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate createdAt,
            @PageableDefault(size = 2, page = 0) Pageable pageable) {
        Page<Abuturient> abuturients = abuturientRepo.findByFilters(
                firstName,
                passportNumber,
                passportPin,
                phone,
                2,
                educationFieldId,
                agentId,
                createdAt,
                pageable);
        return ResponseEntity.ok(abuturients);
    }

    @GetMapping("/appeals/excel/transform")
    public void exportAbuturientsToExcelTransform(
            @RequestParam(required = false, defaultValue = "") String firstName,
            @RequestParam(required = false, defaultValue = "") String lastName,
            @RequestParam(required = false, defaultValue = "") String fatherName,
            @RequestParam(required = false, defaultValue = "") String passportNumber,
            @RequestParam(required = false, defaultValue = "") String passportPin,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam(required = false) Integer appealTypeId,
            @RequestParam(required = false) Integer educationFieldId,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate createdAt,
            HttpServletResponse response) throws IOException {

        // Generate the Excel file
        ByteArrayInputStream in = excelExportService.exportToExcel(
                firstName, lastName, fatherName, passportNumber, passportPin, phone,
                2, educationFieldId, agentId, createdAt);

        // Set response headers
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=abiturients.xlsx");

        // Write the Excel file to the response output stream
        FileCopyUtils.copy(in, response.getOutputStream());
    }


    @PutMapping("/appeals/{id}/{token}")
    public HttpEntity<?> updateAbuturient(@PathVariable UUID id, @PathVariable String token, @RequestBody AbuturientPostDTO dto) {
        System.out.println(dto);
        Optional<Abuturient> optionalAbuturient = abuturientRepo.findById(id);
        User decode = service.decode(token);
        if (!optionalAbuturient.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Abuturient abuturient = optionalAbuturient.get();
        Optional<EducationField> edu = educationFieldRepo.findById(dto.getEducationFieldId());
        Optional<AppealType> appealType = appealTypeRepo.findById(dto.getAppealTypeId());
        if (!edu.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        if (!appealType.isPresent()) {
            return ResponseEntity.notFound().build();
        }

//        History history = new History(decode, abuturient, abuturient.getFirstName(), abuturient.getLastName(), abuturient.getPassportNumber(),abuturient.getFatherName(), abuturient.getPassportPin(), abuturient.getAdditionalPhone(),abuturient.getAppealType(), abuturient.getEducationField(), dto.getFirstName(), dto.getLastName(), dto.getFatherName(), dto.getPassportNumber(), dto.getPassportPin(), dto.getPhone(), appealType.get(), edu.get(), LocalDateTime.now());
        abuturient.setFirstName(dto.getFirstName());
        abuturient.setLastName(dto.getLastName());
        abuturient.setFatherName(dto.getFatherName());
        abuturient.setMotherName(dto.getMotherName());
        if (!dto.getPassportPin().isEmpty()) {
            abuturient.setPassportPin(dto.getPassportPin());
            abuturient.setPassportNumber(dto.getPassportNumber());
        }

        abuturient.setLevel(dto.getLevel());


        abuturient.setEducationField(edu.get());
        abuturient.setAppealType(appealType.get());
        abuturientRepo.save(abuturient);
//        historyRepo.save(history);
        return ResponseEntity.ok("Abuturient updated successfully!");
    }

    @PutMapping("/status/{id}")
    public HttpEntity<?> updateStatus(@PathVariable UUID id) {
        Optional<User> optionalUser = userRepo.findById(id);
        if (!optionalUser.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        User user = optionalUser.get();
        List<Role> roles = user.getRoles();
        Role dataManagerRole = roleRepo.findByName(UserRoles.ROLE_DATA_MANAGER);

        if (roles.contains(dataManagerRole)) {
            // If the role exists, remove it
            System.out.println("remove");
            roles.remove(dataManagerRole);
        } else {
            // If the role doesn't exist, add it
            System.out.println("add");
            roles.add(dataManagerRole);
        }

        user.setRoles(roles);
        userRepo.save(user);

        return ResponseEntity.ok("User updated successfully!");
    }


    @PutMapping("/appeals/ball/{id}/{ball}/{token}")
    public HttpEntity<?> updateAbuturientBall(
            @PathVariable UUID id,
            @PathVariable Double ball,
            @PathVariable String token
    ) {
        Optional<Abuturient> optionalAbuturient = abuturientRepo.findById(id);
        if (!optionalAbuturient.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Abuturient abuturient = optionalAbuturient.get();

        if (ball <= 0 || ball >= 189) {
            return ResponseEntity.badRequest().body("Ball 0 dan katta va 189 dan kichik bo‘lishi kerak.");
        }

        System.out.printf("abuturient: %s\n", abuturient);

        if (ball > 56) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                String email = "akobirjavadev10@gmail.com";
                String password = "qCfkQTHQbQAJLJeElWWI9bv1stjoh3Unt6dNiE04";
                String loginUrl = "https://notify.eskiz.uz/api/auth/login";

                Map<String, String> loginPayload = new HashMap<>();
                loginPayload.put("email", email);
                loginPayload.put("password", password);

                Map loginResponse = restTemplate.postForObject(loginUrl, loginPayload, Map.class);
                token = (String) ((Map) loginResponse.get("data")).get("token");


                String templateUrl = "https://notify.eskiz.uz/api/user/templates";
                HttpEntity<Void> entity = new HttpEntity<>(createHeaders(token));
                Map templatesResponse = restTemplate.exchange(templateUrl, HttpMethod.GET, entity, Map.class).getBody();
                String template = (String) ((Map) ((java.util.List) templatesResponse.get("result")).get(0)).get("template");
                System.out.printf("template: %s\n", template);
                String dynamicUrl = "https://qabul.bxu.uz/api/v1/abuturient/contract/" + abuturient.getPhone();
                String finalMessage = template.replace("%w", dynamicUrl).replace("%d{1,3}", "+998553099999");
                System.out.printf("finalMessage: %s\n", finalMessage);
                String smsUrl = "https://notify.eskiz.uz/api/message/sms/send";
                Map<String, String> smsPayload = new HashMap<>();
                smsPayload.put("mobile_phone", abuturient.getPhone());
                smsPayload.put("message", finalMessage);
                smsPayload.put("from", "4546");

                HttpEntity<Map<String, String>> smsEntity = new HttpEntity<>(smsPayload, createHeaders(token));
                restTemplate.postForObject(smsUrl, smsEntity, Map.class);
                System.out.printf("sms: %s\n", smsEntity);
            } catch (Exception e) {
                return new ResponseEntity<>("SMS sending failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }


        abuturient.setBall(ball.toString());
        abuturient.setGetContract(true);
        abuturient.setStatus(4);
        abuturientRepo.save(abuturient);


        return ResponseEntity.ok("Ball muvaffaqiyatli yangilandi.");
    }
    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

}
