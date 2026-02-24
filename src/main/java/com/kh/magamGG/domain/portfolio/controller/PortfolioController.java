package com.kh.magamGG.domain.portfolio.controller;

import com.kh.magamGG.domain.portfolio.dto.*;
import com.kh.magamGG.domain.portfolio.service.PortfolioExtractService;
import com.kh.magamGG.domain.portfolio.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 포트폴리오: 추출(URL/이미지) + CRUD. 인증 필요 (X-Member-No).
 */
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioExtractService portfolioExtractService;
    private final PortfolioService portfolioService;

    // ---- 추출 (저장하지 않음) ----
    @PostMapping(value = "/extract/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> extractFromImage(@RequestParam("image") MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("포트폴리오 이미지 파일이 필요합니다.");
        }
        PortfolioExtractDto extracted = portfolioExtractService.extractFromImage(image);
        return ResponseEntity.ok(Map.of("success", true, "data", extracted != null ? extracted : Map.of()));
    }

    @PostMapping(value = "/extract/from-page-screenshot", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> extractFromPageScreenshot(@RequestBody Map<String, Object> body) {
        String pageUrl = body != null && body.get("pageUrl") != null ? body.get("pageUrl").toString() : null;
        if (pageUrl == null || pageUrl.isBlank()) {
            throw new IllegalArgumentException("pageUrl이 필요합니다.");
        }
        PortfolioExtractDto extracted = portfolioExtractService.extractFromPageScreenshot(pageUrl.trim());
        return ResponseEntity.ok(Map.of("success", true, "data", extracted != null ? extracted : Map.of()));
    }

    // ---- 저장 (추출 결과 → 규격 저장) ----
    @PostMapping(value = "/from-extract", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PortfolioResponse> saveFromExtract(
            @RequestHeader("X-Member-No") Long memberNo,
            @RequestBody PortfolioExtractDto extractDto) {
        PortfolioResponse saved = portfolioService.createFromExtract(memberNo, extractDto);
        return ResponseEntity.ok(saved);
    }

    // ---- 직접 작성 ----
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PortfolioResponse> create(
            @RequestHeader("X-Member-No") Long memberNo,
            @RequestBody PortfolioCreateRequest request) {
        PortfolioResponse saved = portfolioService.create(memberNo, request);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/me")
    public ResponseEntity<PortfolioResponse> getMyPortfolio(@RequestHeader("X-Member-No") Long memberNo) {
        PortfolioResponse portfolio = portfolioService.getMyPortfolio(memberNo);
        return ResponseEntity.ok(portfolio);
    }

    @GetMapping("/my-projects")
    public ResponseEntity<List<ArtistProjectItemDto>> getMyProjectsForForm(@RequestHeader("X-Member-No") Long memberNo) {
        List<ArtistProjectItemDto> list = portfolioService.getMyProjectsForForm(memberNo);
        return ResponseEntity.ok(list);
    }

    /**
     * 특정 회원의 포트폴리오 조회 (담당자/에이전시 관리자용)
     */
    @GetMapping("/member/{memberNo}")
    public ResponseEntity<PortfolioResponse> getByMemberNo(@PathVariable Long memberNo) {
        PortfolioResponse portfolio = portfolioService.getByMemberNo(memberNo);
        return ResponseEntity.ok(portfolio);
    }

    @PutMapping("/{portfolioNo}")
    public ResponseEntity<PortfolioResponse> update(
            @PathVariable Long portfolioNo,
            @RequestHeader("X-Member-No") Long memberNo,
            @RequestBody PortfolioUpdateRequest request) {
        PortfolioResponse updated = portfolioService.update(portfolioNo, memberNo, request);
        return ResponseEntity.ok(updated);
    }
}
