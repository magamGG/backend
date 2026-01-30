package com.kh.magamGG.domain.agency.repository;

import com.kh.magamGG.domain.agency.entity.Agency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgencyRepository extends JpaRepository<Agency, Long> {
    Optional<Agency> findByAgencyCode(String agencyCode);
    boolean existsByAgencyCode(String agencyCode);
    boolean existsByAgencyName(String agencyName);
}
