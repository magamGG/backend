package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.project.dto.response.ManagedProjectResponse;

import java.util.List;

public interface ProjectService {

    /**
     * 담당자의 담당 작가들의 프로젝트 목록 (마감 기한 대비 진행률 → 정상/주의)
     * @param memberNo 로그인한 담당자 회원 번호
     */
    List<ManagedProjectResponse> getManagedProjectsByManager(Long memberNo);
}


