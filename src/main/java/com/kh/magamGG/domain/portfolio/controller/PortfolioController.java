package com.kh.magamGG.domain.portfolio.controller;

import com.kh.magamGG.domain.portfolio.dto.*;
import com.kh.magamGG.domain.portfolio.entity.Portfolio;
import com.kh.magamGG.domain.portfolio.repository.PortfolioRepository;
import com.kh.magamGG.domain.portfolio.service.NotionPortfolioAuthService;
import com.kh.magamGG.domain.portfolio.service.PortfolioExtractService;
import com.kh.magamGG.domain.portfolio.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
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
    private final NotionPortfolioAuthService notionPortfolioAuthService;
    private final PortfolioRepository portfolioRepository;

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
     * 특정 회원의 포트폴리오 조회 (담당자/에이전시 관리자용).
     * portfolio_status가 N(삭제)인 경우 조회되지 않으며, 활성(Y) 포트폴리오만 반환.
     * 캐시 비활성화로 항상 최신 상태만 반환.
     */
    @GetMapping("/member/{memberNo}")
    public ResponseEntity<PortfolioResponse> getByMemberNo(@PathVariable Long memberNo) {
        PortfolioResponse portfolio = portfolioService.getByMemberNo(memberNo);
        if (portfolio == null) {
            return ResponseEntity.noContent()
                    .cacheControl(CacheControl.noStore().mustRevalidate())
                    .build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .body(portfolio);
    }

    @PutMapping("/{portfolioNo}")
    public ResponseEntity<PortfolioResponse> update(
            @PathVariable Long portfolioNo,
            @RequestHeader("X-Member-No") Long memberNo,
            @RequestBody PortfolioUpdateRequest request) {
        PortfolioResponse updated = portfolioService.update(portfolioNo, memberNo, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * 포트폴리오 삭제 (본인만, portfolio_status를 N으로 변경)
     */
    @DeleteMapping("/{portfolioNo}")
    public ResponseEntity<Void> delete(
            @PathVariable Long portfolioNo,
            @RequestHeader("X-Member-No") Long memberNo) {
        portfolioService.delete(portfolioNo, memberNo);
        return ResponseEntity.noContent().build();
    }

    /**
     * 포트폴리오를 Notion 페이지로 연동 (생성 또는 제목 갱신). 본인 포트폴리오만 가능.
     */
    @PostMapping("/{portfolioNo}/notion-sync")
    public ResponseEntity<PortfolioResponse> syncNotion(
            @PathVariable Long portfolioNo,
            @RequestHeader("X-Member-No") Long memberNo) {
        PortfolioResponse updated = portfolioService.syncNotion(portfolioNo, memberNo);
        return ResponseEntity.ok(updated);
    }

    /**
     * Notion OAuth 설정 (clientId, redirectUri). 연동 버튼 클릭 시 팝업 URL 생성에 사용.
     */
    @GetMapping("/notion/config")
    public ResponseEntity<Map<String, String>> getNotionConfig() {
        return ResponseEntity.ok(Map.of(
                "clientId", notionPortfolioAuthService.getClientId(),
                "redirectUri", notionPortfolioAuthService.getRedirectUri()
        ));
    }

    /**
     * Notion OAuth 콜백 — code로 토큰 교환 후 포트폴리오에 연동 및 페이지 생성. 본인 포트폴리오만 가능.
     */
    @PostMapping("/{portfolioNo}/notion/callback")
    public ResponseEntity<Map<String, Object>> notionCallback(
            @PathVariable Long portfolioNo,
            @RequestHeader("X-Member-No") Long memberNo,
            @RequestBody Map<String, String> body) {
        Portfolio portfolio = portfolioRepository.findById(portfolioNo)
                .orElseThrow(() -> new IllegalArgumentException("포트폴리오를 찾을 수 없습니다."));
        if (portfolio.getMember() == null || !portfolio.getMember().getMemberNo().equals(memberNo)) {
            throw new IllegalArgumentException("본인의 포트폴리오만 Notion 연동할 수 있습니다.");
        }
        String code = body != null ? body.get("code") : null;
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code가 필요합니다.");
        }
        Map<String, Object> result = notionPortfolioAuthService.exchangeCodeAndSave(portfolioNo, code.trim());
        return ResponseEntity.ok(result);
    }
}
