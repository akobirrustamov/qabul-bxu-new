package com.example.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AbuturientDocumentDto {
    private Integer documentStatus;
    private String title;
    private String description;
    private UUID userId;
    private UUID abuturientId;
}
