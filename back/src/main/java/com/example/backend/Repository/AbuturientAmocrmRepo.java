package com.example.backend.Repository;

import com.example.backend.Entity.AbuturientAmocrm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface AbuturientAmocrmRepo extends JpaRepository<AbuturientAmocrm,Integer> {

    @Query(value = "select * from abuturient_amocrm where abuturient_id=:id", nativeQuery = true)
    Optional<AbuturientAmocrm> findByAbuturientId(UUID id);
}
