package com.example.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DataManagerStatisticDto {
    private List<UUID>  adminIds;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean university;
}
