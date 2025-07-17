package com.example.backend.Services;

import com.example.backend.Entity.*;
import com.example.backend.Repository.AbuturientDocumentRepo;
import com.example.backend.Repository.AbuturientRepo;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExcelExportService {
    private final AbuturientRepo abuturientRepo;
    private final AbuturientDocumentRepo abuturientDocumentRepo;

    public ExcelExportService(AbuturientRepo abuturientRepo, AbuturientDocumentRepo abuturientDocumentRepo) {
        this.abuturientRepo = abuturientRepo;
        this.abuturientDocumentRepo = abuturientDocumentRepo;
    }


    public ByteArrayInputStream exportToExcel(String firstName, String lastName, String fatherName,
                                              String passportNumber, String passportPin, String phone,
                                              Integer appealTypeId, Integer educationFieldId, UUID agentId,
                                              java.time.LocalDate createdAt) throws IOException {

        List<Abuturient> abuturients = abuturientRepo.findByFiltersOne(
                firstName, lastName, fatherName, passportNumber, passportPin, phone,
                appealTypeId, educationFieldId, agentId, createdAt);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Abuturients");

            // 🔹 Header Row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "№", "Ism", "Familia", "Otasining ismi", "Onasining ismi", "Passport raqami", "JSHR", "Telefon",
                    "Ro'yxatdan o'tgan sana", "Ta'lim turi", "Ta'lim shakli", "Yo'nalishi",
                    "To'plangan bal", "Agent", "Viloyat", "Tuman", "Shartnoma olgan", "Hujjat holati", "Batafsil"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 🔹 Data Rows
            int rowIdx = 1;
            int counter = 1;
            for (Abuturient abuturient : abuturients) {
                Row row = sheet.createRow(rowIdx++);
                int colIdx = 0;
                row.createCell(colIdx++).setCellValue(counter++); // №
                row.createCell(colIdx++).setCellValue(abuturient.getFirstName() != null ? abuturient.getFirstName() : "");
                row.createCell(colIdx++).setCellValue(abuturient.getLastName() != null ? abuturient.getLastName() : "");
                row.createCell(colIdx++).setCellValue(abuturient.getFatherName() != null ? abuturient.getFatherName() : "");
                row.createCell(colIdx++).setCellValue(abuturient.getMotherName() != null ? abuturient.getMotherName() : "");
                row.createCell(colIdx++).setCellValue(abuturient.getPassportNumber() != null ? abuturient.getPassportNumber() : "");
                row.createCell(colIdx++).setCellValue(abuturient.getPassportPin() != null ? abuturient.getPassportPin() : "");
                row.createCell(colIdx++).setCellValue(abuturient.getPhone() != null ? abuturient.getPhone() : "");
                LocalDateTime createdAtDateTime = abuturient.getCreatedAt();
                String formattedDate = createdAtDateTime != null
                        ? createdAtDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        : "";
                row.createCell(colIdx++).setCellValue(formattedDate);

                // Education
                if (abuturient.getEducationField() != null) {
                    EducationField educationField = abuturient.getEducationField();
                    EducationForm educationForm = educationField.getEducationForm();
                    EducationType educationType = (educationForm != null) ? educationForm.getEducationType() : null;

                    row.createCell(colIdx++).setCellValue(educationType != null ? educationType.getName() : "");
                    row.createCell(colIdx++).setCellValue(educationForm != null ? educationForm.getName() : "");
                    row.createCell(colIdx++).setCellValue(educationField.getName() != null ? educationField.getName() : "");
                } else {
                    row.createCell(colIdx++).setCellValue("");
                    row.createCell(colIdx++).setCellValue("");
                    row.createCell(colIdx++).setCellValue("");
                }

                // Ball
                row.createCell(colIdx++).setCellValue(abuturient.getBall() != null ? abuturient.getBall() : "");

                // Agent Name
                String agentName = (abuturient.getAgent() != null) ? abuturient.getAgent().getName() : "";
                row.createCell(colIdx++).setCellValue(agentName);

                // Viloyat (Region) and Tuman (District)
                District district = abuturient.getDistrict();
                if (district != null) {
                    Region region = district.getRegion();
                    row.createCell(colIdx++).setCellValue(region != null ? region.getName() : ""); // Viloyat
                    row.createCell(colIdx++).setCellValue(district.getName() != null ? district.getName() : ""); // Tuman
                } else {
                    row.createCell(colIdx++).setCellValue(""); // Viloyat
                    row.createCell(colIdx++).setCellValue(""); // Tuman
                }

                // Shartnoma olgan - based on status
                String statusText = "";
                Integer status = abuturient.getStatus();
                if (status != null) {
                    switch (status) {
                        case 1:
                            statusText = "Telefon raqam kiritgan";
                            break;
                        case 2:
                            statusText = "Ma'lumot kiritgan";
                            break;
                        case 3:
                            statusText = "Test yechgan";
                            break;
                        case 4:
                            statusText = "Shartnoma olgan";
                            break;
                        default:
                            statusText = "";
                    }
                }
                row.createCell(colIdx++).setCellValue(statusText);

                // Hujjat holati - based on documentStatus
                String documentStatusText = "Hujjat topshirmagan";
                Integer documentStatus = abuturient.getDocumentStatus();
                if (documentStatus != null) {
                    switch (documentStatus) {
                        case 1:
                            documentStatusText = "Chala topshirgan";
                            break;
                        case 2:
                            documentStatusText = "To'liq hujjat topshirgan";
                            break;
                    }
                }
                row.createCell(colIdx++).setCellValue(documentStatusText);
                String description = "";
                Optional<AbuturientDocument> abuturientDocument = abuturientDocumentRepo.findByAbuturientId(abuturient.getId());
                if (abuturientDocument.isPresent()) {
                    description = abuturientDocument.get().getDescription();
                }
                row.createCell(colIdx++).setCellValue(description);
            }


            // Auto-size columns for better visibility
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}