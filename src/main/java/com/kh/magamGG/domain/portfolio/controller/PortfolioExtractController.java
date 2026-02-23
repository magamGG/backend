package com.kh.magamGG.domain.portfolio.controller;

import com.kh.magamGG.domain.portfolio.dto.PortfolioExtractDto;
import com.kh.magamGG.domain.portfolio.service.PortfolioExtractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 포트폴리오 URL/이미지 → Spring AI Vision → 구조화 정보 추출 API (마감지기용)
 * Security ignoring 대상이므로 CORS는 컨트롤러에서 명시.
 */
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:3000"}, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class PortfolioExtractController {

    private final PortfolioExtractService portfolioExtractService;

    /**
     * 포트폴리오 이미지 파일 업로드 → 이름, 참여 프로젝트, 사용 기술, 경력 추출
     */
    @PostMapping(value = "/extract/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> extractFromImage(
            @RequestParam("image") MultipartFile image
    ) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("포트폴리오 이미지 파일이 필요합니다.");
        }
        PortfolioExtractDto extracted = portfolioExtractService.extractFromImage(image);
        return ResponseEntity.ok(Map.of("success", true, "data", extracted != null ? extracted : Map.of()));
    }

    /**
     * 포트폴리오 웹 페이지 URL → HTML 본문 텍스트 추출 → Spring AI로 구조화 (1-1, 2번)
     * 텍스트 추출 실패(SPA 등) 시 안내 메시지 반환. 추후 스크린샷+Vision 폴백 확장 가능.
     */
    @PostMapping(value = "/extract/from-page", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> extractFromPageUrl(
            @RequestBody Map<String, Object> body
    ) {
        String pageUrl = body != null && body.get("pageUrl") != null ? body.get("pageUrl").toString() : null;
        if (pageUrl == null || pageUrl.isBlank()) {
            throw new IllegalArgumentException("pageUrl이 필요합니다.");
        }
        PortfolioExtractDto extracted = portfolioExtractService.extractFromPageUrl(pageUrl.trim());
        return ResponseEntity.ok(Map.of("success", true, "data", extracted != null ? extracted : Map.of()));
    }

    /**
     * 포트폴리오 웹 페이지 URL → Playwright 전체 스크린샷 → Vision으로 구조화 (SPA/Notion 대응)
     */
    @PostMapping(value = "/extract/from-page-screenshot", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> extractFromPageScreenshot(
            @RequestBody Map<String, Object> body
    ) {
        String pageUrl = body != null && body.get("pageUrl") != null ? body.get("pageUrl").toString() : null;
        if (pageUrl == null || pageUrl.isBlank()) {
            throw new IllegalArgumentException("pageUrl이 필요합니다.");
        }
        PortfolioExtractDto extracted = portfolioExtractService.extractFromPageScreenshot(pageUrl.trim());
        return ResponseEntity.ok(Map.of("success", true, "data", extracted != null ? extracted : Map.of()));
    }
}
