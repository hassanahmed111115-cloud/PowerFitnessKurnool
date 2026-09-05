package com.powerfitness.repository;

import com.powerfitness.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByMemberCode(String memberCode);
    Optional<Member> findByPhoneNumber(String phoneNumber);
    Optional<Member> findByUserId(Long userId);
    List<Member> findByBatch(String batch);
    List<Member> findByTrainingCategory(String trainingCategory);
    List<Member> findByStatus(String status);
    
    @Query("SELECT COUNT(m) FROM Member m WHERE m.status = 'ACTIVE'")
    long countActiveMembers();

    @Query("SELECT COUNT(m) FROM Member m WHERE m.status = 'EXPIRED'")
    long countExpiredMembers();

    @Query("SELECT COUNT(m) FROM Member m WHERE m.status = 'EXPIRING_SOON'")
    long countExpiringSoonMembers();

    @Query("SELECT COUNT(m) FROM Member m WHERE m.admissionDate = :today")
    long countTodayAdmissions(@Param("today") LocalDate today);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.batch = 'Morning Batch'")
    long countMorningBatch();

    @Query("SELECT COUNT(m) FROM Member m WHERE m.batch = 'Evening Batch'")
    long countEveningBatch();

    @Query("SELECT COUNT(m) FROM Member m WHERE m.hasCardio = true OR m.trainingCategory = 'Cardio'")
    long countCardioMembers();

    @Query("SELECT COUNT(m) FROM Member m WHERE m.trainingCategory = 'Strength Training'")
    long countStrengthMembers();
}
