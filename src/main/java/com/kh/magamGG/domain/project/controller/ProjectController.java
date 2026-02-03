package com.kh.magamGG.domain.project.controller;

import com.kh.magamGG.domain.project.dto.response.ManagedProjectResponse;
import com.kh.magamGG.domain.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 프로젝트 API
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    /**
     * 담당자 대시보드 - 담당 프로젝트 현황 (마감 기한 대비 진행률 → 정상/주의)
     * GET /api/projects/managed
     */
    @GetMapping("/managed")
    public ResponseEntity<List<ManagedProjectResponse>> getManagedProjects(
            @RequestHeader("X-Member-No") Long memberNo) {

        log.info("담당 프로젝트 현황 조회: 담당자 회원={}", memberNo);
        List<ManagedProjectResponse> list = projectService.getManagedProjectsByManager(memberNo);
        return ResponseEntity.ok(list);
    }
}
