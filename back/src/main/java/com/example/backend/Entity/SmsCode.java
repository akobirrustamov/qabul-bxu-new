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
@Table(name = "sms_code")
public class SmsCode {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private Integer code;
    @ManyToOne
    private Abuturient abuturient;
    private LocalDateTime sendTime;
    private LocalDateTime expireTime;


    public SmsCode(Integer code, Abuturient abuturient, LocalDateTime sendTime, LocalDateTime expireTime) {
        this.code = code;
        this.abuturient = abuturient;
        this.sendTime = sendTime;
        this.expireTime = expireTime;
    }


}
