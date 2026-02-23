package com.kh.magamGG.domain.portfolio.service;

import com.kh.magamGG.domain.portfolio.dto.PortfolioExtractDto;
import org.springframework.web.multipart.MultipartFile;

/**
 * 포트폴리오 URL/이미지 → 추출 → Spring AI 구조화 (마감지기용)
 * - 이미지: Vision으로 추출·구조화
 * - 웹 페이지 URL: HTML 텍스트 추출 후 LLM으로 구조화 (실패 시 스크린샷+Vision은 추후 확장)
 */
public interface PortfolioExtractService {

    /**
     * 포트폴리오 이미지 파일에서 이름, 참여 프로젝트, 사용 기술, 경력 추출 (Vision)
     */
    PortfolioExtractDto extractFromImage(MultipartFile imageFile);

    /**
     * 포트폴리오 웹 페이지 URL → HTML 본문 텍스트 추출 → Spring AI로 구조화 (텍스트만 LLM)
     * 본문 추출 실패(SPA 등) 시 예외. 추후 스크린샷+Vision 폴백 가능.
     */
    PortfolioExtractDto extractFromPageUrl(String pageUrl);

    /**
     * 포트폴리오 웹 페이지 URL → Playwright 전체 스크린샷 → Vision으로 구조화 (SPA/Notion 대응)
     */
    PortfolioExtractDto extractFromPageScreenshot(String pageUrl);
}
