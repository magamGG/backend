package com.kh.magamGG.domain.manager.repository;

import com.kh.magamGG.domain.manager.entity.Manager;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManagerRepository extends JpaRepository<Manager, Long> {

    Optional<Manager> findByMember_MemberNo(Long memberNo);
}
