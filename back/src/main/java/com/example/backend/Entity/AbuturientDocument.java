package com.example.backend.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "abuturient_document")
public class AbuturientDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Specifies auto-increment behavior
    private Integer id;
    private Integer documentStatus;
    private String title;
    private String description;
    @ManyToOne
    private User user;
    private LocalDateTime createdAt;
    @OneToOne
    @JoinColumn(name = "abuturient_id", unique = true)
    private Abuturient abuturient;
    public AbuturientDocument(Integer documentStatus, String title, String description, User user, Abuturient abuturient, LocalDateTime createdAt) {
        this.documentStatus = documentStatus;
        this.title = title;
        this.description = description;
        this.user = user;
        this.abuturient = abuturient;
        this.createdAt = createdAt;
    }
}
