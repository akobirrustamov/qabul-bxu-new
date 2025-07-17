package com.example.backend.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "history_of_abuturient")
public class HistoryOfAbuturient {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @ManyToOne
    private Abuturient abuturient;
    private LocalDateTime date;


    public HistoryOfAbuturient(Abuturient abuturient, LocalDateTime date) {
        this.abuturient = abuturient;
        this.date = date;
    }
}
