package com.kh.magamGG.domain.portfolio.service;

import com.kh.magamGG.domain.portfolio.dto.*;

import java.util.List;

public interface PortfolioService {

    /**
     * 추출 결과를 규격에 맞게 저장 (로그인 회원 소유)
     */
    PortfolioResponse createFromExtract(Long memberNo, PortfolioExtractDto extractDto);

    /**
     * 직접 작성한 포트폴리오 저장
     */
    PortfolioResponse create(Long memberNo, PortfolioCreateRequest request);

    /**
     * 내 포트폴리오 1건 조회 (최신 활성)
     */
    PortfolioResponse getMyPortfolio(Long memberNo);

    /**
     * 포트폴리오 만들기 폼용: 로그인 아티스트의 참여 프로젝트 + 프로젝트 시작일
     */
    List<ArtistProjectItemDto> getMyProjectsForForm(Long memberNo);

    /**
     * 특정 회원의 포트폴리오 조회 (담당자/에이전시 관리자용)
     */
    PortfolioResponse getByMemberNo(Long memberNo);

    /**
     * 포트폴리오 수정 (본인만)
     */
    PortfolioResponse update(Long portfolioNo, Long memberNo, PortfolioUpdateRequest request);
}
