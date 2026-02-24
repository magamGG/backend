package com.kh.magamGG.domain.portfolio.service;

import com.kh.magamGG.domain.portfolio.dto.PortfolioExtractDto;
import org.springframework.web.multipart.MultipartFile;

/**
 * 포트폴리오 URL/이미지 → 추출 → Spring AI 구조화
 * - 이미지: Vision으로 추출
 * - URL: HTML 텍스트 또는 Playwright 스크린샷 + Vision
 */
public interface PortfolioExtractService {

    PortfolioExtractDto extractFromImage(MultipartFile imageFile);

    PortfolioExtractDto extractFromPageUrl(String pageUrl);

    PortfolioExtractDto extractFromPageScreenshot(String pageUrl);
}
