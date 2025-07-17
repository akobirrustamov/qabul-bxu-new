package com.example.backend.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "abuturient_amocrm")
public class AbuturientAmocrm {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne
    private Abuturient abuturient;

    private Long leadId;
    private LocalDateTime createdAt;
    private Long contactId;

    public AbuturientAmocrm(Abuturient abuturient, Long leadId, LocalDateTime createdAt, Long contactId) {
        this.abuturient = abuturient;
        this.leadId = leadId;
        this.createdAt = createdAt;
        this.contactId = contactId;
    }
}
