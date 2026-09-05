package com.powerfitness.repository;

import com.powerfitness.entity.UpiSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UpiSettingRepository extends JpaRepository<UpiSetting, Long> {
    Optional<UpiSetting> findTopByOrderByIdDesc();
}
