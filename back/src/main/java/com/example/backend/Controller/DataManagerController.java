package com.example.backend.Controller;

import com.example.backend.DTO.DataManagerStatisticDto;
import com.example.backend.Entity.Abuturient;
import com.example.backend.Entity.EducationField;
import com.example.backend.Entity.EducationForm;
import com.example.backend.Entity.User;
import com.example.backend.Repository.AbuturientRepo;
import com.example.backend.Repository.EducationFieldRepo;
import com.example.backend.Repository.EducationFormRepo;
import com.example.backend.Repository.UserRepo;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/data-manager")
@RequiredArgsConstructor
public class DataManagerController {
    private final UserRepo userRepo;
    private final AbuturientRepo abuturientRepo;
    private final EducationFieldRepo educationFieldRepo;
    private final EducationFormRepo educationFormRepo;

    @PostMapping("/statistic")
    public ResponseEntity<?> statistic(@RequestBody DataManagerStatisticDto dataManagerStatisticDto) {
        System.out.println(dataManagerStatisticDto);

        // Handle agent names for title
        String agentNames;

        List<String> names = new ArrayList<>();
            for (UUID adminId : dataManagerStatisticDto.getAdminIds()) {
                if (adminId == null) {
                    continue; // Skip null IDs
                }
                userRepo.findById(adminId).ifPresent(user -> names.add(user.getName()));
            }
            agentNames = String.join(", ", names);
            if (agentNames.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Agentlar topilmadi");
            }

        if (dataManagerStatisticDto.getUniversity()) {
            agentNames =agentNames+ ", Universitet havolasi";
        }
        String title = dataManagerStatisticDto.getStartDate().toString();
        if (dataManagerStatisticDto.getEndDate() != null) {
            title += " dan " + dataManagerStatisticDto.getEndDate().toString() + " gacha olingan arizalar";
        } else {
            title += " sanada olingan arizalar";
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Statistika");

            // 🔹 Top title row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title + " | Agentlar: " + agentNames);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            int rowIndex = 1; // Start after title row
            int index = 1;    // Serial number for each row

            // Variables for grand totals
            int grandTotalShartnoma = 0;
            int grandTotalShartnomasiz = 0;
            int grandTotalJami = 0;
            int grandTotalHujjat = 0;

            List<EducationForm> allForms = educationFormRepo.findAll();
            for (EducationForm form : allForms) {
                // 🔹 Education form name row
                Row formRow = sheet.createRow(rowIndex++);
                Cell formCell = formRow.createCell(0);
                formCell.setCellValue(form.getEducationType().getName() + " " + form.getName());
                sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, 5));

                // 🔹 Table header
                Row headerRow = sheet.createRow(rowIndex++);
                headerRow.createCell(0).setCellValue("#");
                headerRow.createCell(1).setCellValue("Yo'nalish nomi");
                headerRow.createCell(2).setCellValue("Shartnoma olgan");
                headerRow.createCell(3).setCellValue("Shartnomasiz");
                headerRow.createCell(4).setCellValue("Jami");
                headerRow.createCell(5).setCellValue("Hujjat topshirgan");

                // Totals for current form block
                int totalShartnoma = 0;
                int totalShartnomasiz = 0;
                int totalJami = 0;
                int totalHujjat = 0;

                List<EducationField> fields = educationFieldRepo.findByEducationFormId(form.getId());
                for (EducationField field : fields) {
                    LocalDate startDate = dataManagerStatisticDto.getStartDate();
                    LocalDate endDate = dataManagerStatisticDto.getEndDate();

                    // Initialize counters
                    int shartnomaNumber = 0;
                    int jamiNumber = 0;
                    int hujjatNumber = 0;

                    if (dataManagerStatisticDto.getUniversity()) {
                        // University statistics - no agent filter
                        if (endDate == null) {
                            // Single date statistics
                            shartnomaNumber = abuturientRepo.findByEducationFieldAndDateUniversity(
                                    field.getId(), startDate, 4).size();

                            jamiNumber = abuturientRepo.findByEducationFieldAndDateAllUniversity(
                                    field.getId(), startDate).size();

                            hujjatNumber = abuturientRepo.findByEducationFieldAndDateDocumentUniversity(
                                    field.getId(), startDate).size();
                        } else {
                            // Date range statistics
                            shartnomaNumber = abuturientRepo.findByEducationFieldAndDateAndEndDateUniversity(
                                    field.getId(), 4, startDate, endDate).size();

                            jamiNumber = abuturientRepo.findByEducationFieldAndDateAllAndEndDateUniversity(
                                    field.getId(), startDate, endDate).size();

                            hujjatNumber = abuturientRepo.findByEducationFieldAndDateDocumentAndEndDateUniversity(
                                    field.getId(), startDate, endDate).size();
                        }
                    } else {
                        // Agent statistics
                        List<UUID> adminIds = dataManagerStatisticDto.getAdminIds();
                        for (UUID adminId : adminIds) {
                            if (endDate == null) {
                                // Single date statistics
                                shartnomaNumber += abuturientRepo.findByContractByAgentAndEducationFieldAndDate(
                                        field.getId(), adminId, startDate, 4).size();

                                jamiNumber += abuturientRepo.findByContractByAgentAndEducationFieldAndDateAll(
                                        field.getId(), adminId, startDate).size();

                                hujjatNumber += abuturientRepo.findByContractByAgentAndEducationFieldAndDateDocument(
                                        field.getId(), adminId, startDate).size();

                            } else {
                                // Date range statistics
                                shartnomaNumber += abuturientRepo.findByContractByAgentAndEducationFieldAndDateAndEndDate(
                                        field.getId(), adminId, 4, startDate, endDate).size();

                                jamiNumber += abuturientRepo.findByContractByAgentAndEducationFieldAndDateAllAndEndDate(
                                        field.getId(), adminId, startDate, endDate).size();

                                hujjatNumber += abuturientRepo.findByContractByAgentAndEducationFieldAndDateDocumentAndEndDate(
                                        field.getId(), adminId, startDate, endDate).size();
                            }
                        }
                    }

                    // Calculate shartnomasiz
                    int shartnomasizNumber = jamiNumber - shartnomaNumber;

                    // Update form totals
                    totalShartnoma += shartnomaNumber;
                    totalShartnomasiz += shartnomasizNumber;
                    totalJami += jamiNumber;
                    totalHujjat += hujjatNumber;

                    // Update grand totals
                    grandTotalShartnoma += shartnomaNumber;
                    grandTotalShartnomasiz += shartnomasizNumber;
                    grandTotalJami += jamiNumber;
                    grandTotalHujjat += hujjatNumber;

                    // Add data row
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(index++);
                    row.createCell(1).setCellValue(field.getName());
                    row.createCell(2).setCellValue(shartnomaNumber);
                    row.createCell(3).setCellValue(shartnomasizNumber);
                    row.createCell(4).setCellValue(jamiNumber);
                    row.createCell(5).setCellValue(hujjatNumber);
                }

                // 🔹 Total row after each form
                Row totalRow = sheet.createRow(rowIndex++);
                totalRow.createCell(1).setCellValue("Jami");
                totalRow.createCell(2).setCellValue(totalShartnoma);
                totalRow.createCell(3).setCellValue(totalShartnomasiz);
                totalRow.createCell(4).setCellValue(totalJami);
                totalRow.createCell(5).setCellValue(totalHujjat);
            }

            // 🔹 Grand total row at the end
            Row grandTotalRow = sheet.createRow(rowIndex++);
            grandTotalRow.createCell(1).setCellValue("Umumiy jami");
            grandTotalRow.createCell(2).setCellValue(grandTotalShartnoma);
            grandTotalRow.createCell(3).setCellValue(grandTotalShartnomasiz);
            grandTotalRow.createCell(4).setCellValue(grandTotalJami);
            grandTotalRow.createCell(5).setCellValue(grandTotalHujjat);

            // 🔹 Auto-size columns
            for (int i = 0; i <= 5; i++) {
                sheet.autoSizeColumn(i);
            }

            // 🔹 Return Excel file
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("attachment", "statistika.xlsx");
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Xatolik yuz berdi");
        }
    }
}