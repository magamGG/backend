package com.kh.magamGG.domain.project.repository;

import com.kh.magamGG.domain.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    List<ProjectMember> findByMember_MemberNo(Long memberNo);
}
