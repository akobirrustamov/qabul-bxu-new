package com.example.backend.Repository;

import com.example.backend.Entity.Abuturient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


import org.springframework.stereotype.Repository;

@Repository
public interface AbuturientRepo extends JpaRepository<Abuturient, UUID> {

    @Query(value = "SELECT * FROM abuturient WHERE phone = :phone", nativeQuery = true)
    Abuturient findByPhone(@Param("phone") String phone);

    @Query(value = "SELECT * FROM abuturient WHERE " +
            "(:fullName IS NULL OR :fullName = '' OR LOWER(CONCAT_WS(' ', last_name, first_name, father_name)) LIKE LOWER(CONCAT('%', :fullName, '%'))) AND " +
            "(:passportNumber IS NULL OR :passportNumber = '' OR passport_number LIKE CONCAT('%', :passportNumber, '%')) AND " +
            "(:passportPin IS NULL OR :passportPin = '' OR passport_pin LIKE CONCAT('%', :passportPin, '%')) AND " +
            "(:phone IS NULL OR :phone = '' OR phone LIKE CONCAT('%', :phone, '%')) AND " +
            "(COALESCE(:appealTypeId, 0) = 0 OR appeal_type_id = :appealTypeId) AND " +
            "(COALESCE(:educationFieldId, 0) = 0 OR education_field_id = :educationFieldId) AND " +
            "(COALESCE(:agentId, '00000000-0000-0000-0000-000000000000') = '00000000-0000-0000-0000-000000000000' OR agent_id = :agentId) AND " +
            "(COALESCE(:createdAt, NULL) IS NULL OR DATE(created_at) = :createdAt)",

            countQuery = "SELECT COUNT(*) FROM abuturient WHERE " +
                    "(:fullName IS NULL OR :fullName = '' OR LOWER(CONCAT_WS(' ', last_name, first_name, father_name)) LIKE LOWER(CONCAT('%', :fullName, '%'))) AND " +
                    "(:passportNumber IS NULL OR :passportNumber = '' OR passport_number LIKE CONCAT('%', :passportNumber, '%')) AND " +
                    "(:passportPin IS NULL OR :passportPin = '' OR passport_pin LIKE CONCAT('%', :passportPin, '%')) AND " +
                    "(:phone IS NULL OR :phone = '' OR phone LIKE CONCAT('%', :phone, '%')) AND " +
                    "(COALESCE(:appealTypeId, 0) = 0 OR appeal_type_id = :appealTypeId) AND " +
                    "(COALESCE(:educationFieldId, 0) = 0 OR education_field_id = :educationFieldId) AND " +
                    "(COALESCE(:agentId, '00000000-0000-0000-0000-000000000000') = '00000000-0000-0000-0000-000000000000' OR agent_id = :agentId) AND " +
                    "(COALESCE(:createdAt, NULL) IS NULL OR DATE(created_at) = :createdAt)",

            nativeQuery = true)
    Page<Abuturient> findByFilters(
            @Param("fullName") String fullName,
            @Param("passportNumber") String passportNumber,
            @Param("passportPin") String passportPin,
            @Param("phone") String phone,
            @Param("appealTypeId") Integer appealTypeId,
            @Param("educationFieldId") Integer educationFieldId,
            @Param("agentId") UUID agentId,
            @Param("createdAt") LocalDate createdAt,
            Pageable pageable);







    @Query(value = "SELECT * FROM abuturient WHERE agent_id = :agentId", nativeQuery = true)
    List<Abuturient> findByAgentId(@Param("agentId") UUID agentId);

    @Query(value = "SELECT * FROM abuturient WHERE contract_number = :contractNumber", nativeQuery = true)
    Optional<Abuturient> findByContractNumber(@Param("contractNumber") Integer contractNumber);



    @Query(value = "SELECT * FROM abuturient WHERE " +
            "(:firstName IS NULL OR :firstName = '' OR first_name LIKE CONCAT('%', :firstName, '%')) AND " +
            "(:lastName IS NULL OR :lastName = '' OR last_name LIKE CONCAT('%', :lastName, '%')) AND " +
            "(:fatherName IS NULL OR :fatherName = '' OR father_name LIKE CONCAT('%', :fatherName, '%')) AND " +
            "(:passportNumber IS NULL OR :passportNumber = '' OR passport_number LIKE CONCAT('%', :passportNumber, '%')) AND " +
            "(:passportPin IS NULL OR :passportPin = '' OR passport_pin LIKE CONCAT('%', :passportPin, '%')) AND " +
            "(:phone IS NULL OR :phone = '' OR phone LIKE CONCAT('%', :phone, '%')) AND " +
            "(COALESCE(:appealTypeId, 0) = 0 OR appeal_type_id = :appealTypeId) AND " +
            "(COALESCE(:educationFieldId, 0) = 0 OR education_field_id = :educationFieldId) AND " +
            "(COALESCE(:agentId, '00000000-0000-0000-0000-000000000000') = '00000000-0000-0000-0000-000000000000' OR agent_id = :agentId) AND " +
            "(COALESCE(:createdAt, NULL) IS NULL OR DATE(created_at) = :createdAt)",
            countQuery = "SELECT COUNT(*) FROM abuturient WHERE " +
                    "(:firstName IS NULL OR :firstName = '' OR first_name LIKE CONCAT('%', :firstName, '%')) AND " +
                    "(:lastName IS NULL OR :lastName = '' OR last_name LIKE CONCAT('%', :lastName, '%')) AND " +
                    "(:fatherName IS NULL OR :fatherName = '' OR father_name LIKE CONCAT('%', :fatherName, '%')) AND " +
                    "(:passportNumber IS NULL OR :passportNumber = '' OR passport_number LIKE CONCAT('%', :passportNumber, '%')) AND " +
                    "(:passportPin IS NULL OR :passportPin = '' OR passport_pin LIKE CONCAT('%', :passportPin, '%')) AND " +
                    "(:phone IS NULL OR :phone = '' OR phone LIKE CONCAT('%', :phone, '%')) AND " +
                    "(COALESCE(:appealTypeId, 0) = 0 OR appeal_type_id = :appealTypeId) AND " +
                    "(COALESCE(:educationFieldId, 0) = 0 OR education_field_id = :educationFieldId) AND " +
                    "(COALESCE(:agentId, '00000000-0000-0000-0000-000000000000') = '00000000-0000-0000-0000-000000000000' OR agent_id = :agentId) AND " +
                    "(COALESCE(:createdAt, NULL) IS NULL OR DATE(created_at) = :createdAt)",
            nativeQuery = true)
    List<Abuturient> findByFiltersOne(String firstName, String lastName, String fatherName, String passportNumber, String passportPin, String phone, Integer appealTypeId, Integer educationFieldId, UUID agentId, LocalDate createdAt);




//    data manager statistic filter for one day
    @Query(value = "select * from abuturient where  education_field_id = :educationFieldId and DATE(created_at) = :createdAt and  agent_id=:agentId and status=:status", nativeQuery = true)
    List<Abuturient> findByContractByAgentAndEducationFieldAndDate(Integer educationFieldId, UUID agentId, LocalDate createdAt, Integer status);

    @Query(value = "select * from abuturient where  education_field_id = :educationFieldId and DATE(created_at) = :createdAt and  agent_id=:agentId", nativeQuery = true)
    List<Abuturient> findByContractByAgentAndEducationFieldAndDateAll(Integer educationFieldId, UUID agentId, LocalDate createdAt);


    @Query(value = "select * from abuturient where  education_field_id = :educationFieldId and DATE(created_at) = :createdAt and  agent_id=:agentId and (document_status=1 or document_status=2 )", nativeQuery = true)
    List<Abuturient> findByContractByAgentAndEducationFieldAndDateDocument(Integer educationFieldId, UUID agentId, LocalDate createdAt);

    @Query(value = "SELECT * FROM abuturient WHERE education_field_id = :educationFieldId AND DATE(created_at) = :createdAt AND agent_id = :agentId AND document_title IS NOT NULL", nativeQuery = true)
    List<Abuturient> findByContractByAgentAndEducationFieldAndDateDocumentTitle(
            @Param("educationFieldId") Integer educationFieldId,
            @Param("agentId") UUID agentId,
            @Param("createdAt") LocalDate createdAt);



    //    data manager statistic filter for many day
    @Query(value = " SELECT * FROM abuturient WHERE education_field_id = :educationFieldId AND agent_id = :adminId AND status = :status AND DATE(created_at) BETWEEN :startDate AND :endDate", nativeQuery = true)
    List<Abuturient> findByContractByAgentAndEducationFieldAndDateAndEndDate(Integer educationFieldId, UUID adminId, Integer status, LocalDate startDate, LocalDate endDate);

    @Query(value = "SELECT * FROM abuturient WHERE education_field_id = :educationFieldId AND agent_id = :adminId AND DATE(created_at) BETWEEN :startDate AND :endDate", nativeQuery = true)
    List<Abuturient> findByContractByAgentAndEducationFieldAndDateAllAndEndDate(Integer educationFieldId, UUID adminId, LocalDate startDate, LocalDate endDate);


    @Query(value = " SELECT * FROM abuturient WHERE education_field_id = :educationFieldId AND agent_id = :adminId AND document_status IN (1, 2)  AND DATE(created_at) BETWEEN :startDate AND :endDate", nativeQuery = true)
    List<Abuturient> findByContractByAgentAndEducationFieldAndDateDocumentAndEndDate( Integer educationFieldId,  UUID adminId, LocalDate startDate, LocalDate endDate);
    // University statistics - no agent filter (single date)
    @Query(value = "select * from abuturient where education_field_id = :educationFieldId and DATE(created_at) = :createdAt and status=:status and (agent_id is null )", nativeQuery = true)
    List<Abuturient> findByEducationFieldAndDateUniversity(Integer educationFieldId, LocalDate createdAt, Integer status);

    @Query(value = "select * from abuturient where education_field_id = :educationFieldId and DATE(created_at) = :createdAt and (agent_id is null )", nativeQuery = true)
    List<Abuturient> findByEducationFieldAndDateAllUniversity(Integer educationFieldId, LocalDate createdAt);

    @Query(value = "select * from abuturient where education_field_id = :educationFieldId and DATE(created_at) = :createdAt and (document_status=1 or document_status=2) and (agent_id is null )", nativeQuery = true)
    List<Abuturient> findByEducationFieldAndDateDocumentUniversity(Integer educationFieldId, LocalDate createdAt);

    // University statistics - date range
    @Query(value = "SELECT * FROM abuturient WHERE education_field_id = :educationFieldId AND status = :status AND DATE(created_at) BETWEEN :startDate AND :endDate AND (agent_id is null )", nativeQuery = true)
    List<Abuturient> findByEducationFieldAndDateAndEndDateUniversity(Integer educationFieldId, Integer status, LocalDate startDate, LocalDate endDate);

    @Query(value = "SELECT * FROM abuturient WHERE education_field_id = :educationFieldId AND DATE(created_at) BETWEEN :startDate AND :endDate AND (agent_id is null )", nativeQuery = true)
    List<Abuturient> findByEducationFieldAndDateAllAndEndDateUniversity(Integer educationFieldId, LocalDate startDate, LocalDate endDate);

    @Query(value = "SELECT * FROM abuturient WHERE education_field_id = :educationFieldId AND document_status IN (1, 2) AND DATE(created_at) BETWEEN :startDate AND :endDate AND (agent_id is null )", nativeQuery = true)
    List<Abuturient> findByEducationFieldAndDateDocumentAndEndDateUniversity(Integer educationFieldId, LocalDate startDate, LocalDate endDate);




}