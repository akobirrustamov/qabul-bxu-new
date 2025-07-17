package com.example.backend.Controller;

import com.example.backend.Entity.Abuturient;
import com.example.backend.Entity.HistoryOfAbuturient;
import com.example.backend.Repository.AbuturientRepo;
import com.example.backend.Repository.HistoryOfAbuturientRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("/api/v1/history-of-abuturient")
public class HistoryOfAbuturientController {
    private final HistoryOfAbuturientRepo historyOfAbuturientRepo;
    private final AbuturientRepo abuturientRepo;


    @PostMapping("/{phone}")
    public HttpEntity<?> addHistoryOfAbuturient(@PathVariable String phone) {
        System.out.println(phone);
        Abuturient abuturent = abuturientRepo.findByPhone(phone);
        if (abuturent == null) {
            return ResponseEntity.notFound().build();
        }

        HistoryOfAbuturient historyOfAbuturient = new HistoryOfAbuturient(abuturent, LocalDateTime.now());
        historyOfAbuturientRepo.save(historyOfAbuturient);
        return ResponseEntity.ok(historyOfAbuturient);
    }
    @GetMapping
    public HttpEntity<?> getHistoryOfAbuturient(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate createdAt){
        List<HistoryOfAbuturient> historyOfAbuturients= historyOfAbuturientRepo.findAllByDate(createdAt);
        return ResponseEntity.ok(historyOfAbuturients);
    }


    @DeleteMapping("/{id}")
    public HttpEntity<?> deleteHistoryOfAbuturient(@PathVariable Integer id){
        historyOfAbuturientRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }



}
