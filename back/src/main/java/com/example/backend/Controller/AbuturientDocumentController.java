package com.example.backend.Controller;

import com.example.backend.DTO.AbuturientDocumentDto;
import com.example.backend.Entity.Abuturient;
import com.example.backend.Entity.AbuturientDocument;
import com.example.backend.Entity.User;
import com.example.backend.Repository.AbuturientDocumentRepo;
import com.example.backend.Repository.AbuturientRepo;
import com.example.backend.Repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.apache.xmlbeans.impl.xb.xsdschema.Attribute;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@CrossOrigin
@RequestMapping("/api/v1/abuturient-document")
public class AbuturientDocumentController {
    private final AbuturientDocumentRepo abuturientDocumentRepo;
    private final AbuturientRepo abuturientRepo;
    private final UserRepo userRepo;


    @GetMapping
    public List<AbuturientDocument> getAllAbuturientDocument(){
        return abuturientDocumentRepo.findAll();
    }


    @PostMapping
    public HttpEntity<?> addAbuturientDocument(@RequestBody AbuturientDocumentDto abuturientDocumentDto){
        Optional<User> user = userRepo.findById(abuturientDocumentDto.getUserId());
        if (user.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        Optional<Abuturient> abuturient = abuturientRepo.findById(abuturientDocumentDto.getAbuturientId());
        if (abuturient.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        if( abuturientDocumentDto.getDocumentStatus()>0){
            Abuturient abuturient1 = abuturient.get();
            abuturient1.setDocumentStatus(abuturientDocumentDto.getDocumentStatus());
            abuturientRepo.save(abuturient1);
        }
        AbuturientDocument abuturientDocument1 = new AbuturientDocument(abuturientDocumentDto.getDocumentStatus(), abuturientDocumentDto.getTitle(), abuturientDocumentDto.getDescription(), user.get(), abuturient.get(), LocalDateTime.now());
        AbuturientDocument save = abuturientDocumentRepo.save(abuturientDocument1);
        return ResponseEntity.ok(save);
    }

    @GetMapping("/abuturient/{abuturientId}")
    public HttpEntity<?> getAbuturientDocument(@PathVariable UUID abuturientId){
        Optional<AbuturientDocument> abuturientDocument=abuturientDocumentRepo.findByAbuturientId(abuturientId);
        if (abuturientDocument.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(abuturientDocument.get());
    }



}

