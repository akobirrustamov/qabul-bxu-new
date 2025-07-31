package com.example.backend.Repository;

import com.example.backend.Entity.AbuturientDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface AbuturientDocumentRepo extends JpaRepository<AbuturientDocument,Integer> {

    @Query(value = "select * from abuturient_document where abuturient_id=:abuturientId", nativeQuery = true)
    Optional<AbuturientDocument> findByAbuturientId(UUID abuturientId);


    @Modifying
    @Transactional
    @Query(value = "DELETE FROM abuturient_document WHERE abuturient_id = :abuturientId", nativeQuery = true)
    void deleteByAbuturientId(UUID abuturientId);
}
