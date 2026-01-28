package com.kh.magamGG.domain.manager.repository;

import com.kh.magamGG.domain.manager.entity.ArtistAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtistAssignmentRepository
        extends JpaRepository<ArtistAssignment, Long> {

    List<ArtistAssignment> findByManager_ManagerNo(Long managerNo);

    boolean existsByManager_ManagerNoAndArtist_MemberNo(
            Long managerNo,
            Long artistMemberNo
    );
}