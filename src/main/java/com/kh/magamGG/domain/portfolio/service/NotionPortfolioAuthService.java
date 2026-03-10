package com.kh.magamGG.domain.portfolio.service;

import com.kh.magamGG.domain.portfolio.entity.Portfolio;
import com.kh.magamGG.domain.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 포트폴리오 Notion OAuth: code 교환 → 토큰 저장 → 페이지 생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotionPortfolioAuthService {

    private static final String NOTION_TOKEN_URL = "https://api.notion.com/v1/oauth/token";

    private final PortfolioRepository portfolioRepository;
    private final NotionPortfolioSyncService notionPortfolioSyncService;

    @Value("${notion.client-id}")
    private String clientId;

    @Value("${notion.client-secret}")
    private String clientSecret;

    @Value("${notion.redirect-uri}")
    private String redirectUri;

    public String getClientId() {
        return clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> exchangeCodeAndSave(Long portfolioNo, String code) {
        Portfolio portfolio = portfolioRepository.findById(portfolioNo)
                .orElseThrow(() -> new IllegalArgumentException("포트폴리오를 찾을 수 없습니다: " + portfolioNo));

        String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + credentials);

        Map<String, String> body = Map.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", redirectUri
        );

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                NOTION_TOKEN_URL, HttpMethod.POST, request, Map.class);

        Map<String, Object> tokenResponse = response.getBody();
        if (tokenResponse == null) {
            throw new RuntimeException("Notion 토큰 교환 응답이 비어 있습니다.");
        }

        String accessToken = (String) tokenResponse.get("access_token");
        String workspaceName = (String) tokenResponse.get("workspace_name");

        portfolio.setNotionAccessToken(accessToken);
        portfolio.setNotionWorkspaceName(workspaceName != null ? workspaceName : "");
        portfolioRepository.save(portfolio);

        log.info("Notion 포트폴리오 토큰 저장: portfolioNo={}, workspace={}", portfolioNo, workspaceName);

        notionPortfolioSyncService.syncPortfolioToNotion(portfolio);
        portfolioRepository.save(portfolio);
        portfolio = portfolioRepository.findById(portfolioNo).orElse(portfolio);

        return Map.of(
                "workspaceName", portfolio.getNotionWorkspaceName() != null ? portfolio.getNotionWorkspaceName() : "",
                "notionPageId", portfolio.getNotionPageId() != null ? portfolio.getNotionPageId() : "",
                "notionPageUrl", portfolio.getNotionPageUrl() != null ? portfolio.getNotionPageUrl() : "",
                "pageCreated", portfolio.getNotionPageId() != null
        );
    }
}
