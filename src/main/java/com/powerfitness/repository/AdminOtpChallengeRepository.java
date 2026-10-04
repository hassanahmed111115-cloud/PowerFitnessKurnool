package com.powerfitness.repository;

import com.powerfitness.entity.AdminOtpChallenge;
import com.powerfitness.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminOtpChallengeRepository extends JpaRepository<AdminOtpChallenge, Long> {
    Optional<AdminOtpChallenge> findByChallengeToken(String challengeToken);
    Optional<AdminOtpChallenge> findByResetToken(String resetToken);
    List<AdminOtpChallenge> findByUserAndUsedFalse(User user);
    long countByMobileNumberAndCreatedAtAfter(String mobileNumber, LocalDateTime after);
}
