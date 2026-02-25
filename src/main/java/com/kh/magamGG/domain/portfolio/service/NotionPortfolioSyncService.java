package com.kh.magamGG.domain.portfolio.service;

import com.kh.magamGG.domain.portfolio.entity.Portfolio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 포트폴리오 → Notion 페이지 생성/갱신 (기본 정보, 경력, 참여 프로젝트, 스킬)
 */
@Service
@Slf4j
public class NotionPortfolioSyncService {

    private static final String NOTION_PAGES_URL = "https://api.notion.com/v1/pages";
    private static final String NOTION_BLOCKS_URL = "https://api.notion.com/v1/blocks";
    private static final String NOTION_SEARCH_URL = "https://api.notion.com/v1/search";
    private static final int MAX_RICH_TEXT_LENGTH = 2000;

    @Value("${notion.api-version:2022-06-28}")
    private String notionApiVersion;

    @Value("${notion.portfolio.enabled:false}")
    private boolean portfolioEnabled;

    @Value("${notion.portfolio.integration-token:}")
    private String integrationToken;

    @Value("${notion.portfolio.parent-page-id:}")
    private String parentPageId;

    /** Notion 등에서 이미지 로드 시 사용할 공개 URL (프로필 사진용) */
    @Value("${app.public-base-url:http://localhost:8888}")
    private String publicBaseUrl;

    /** Notion API 블록 추가 요청당 최대 개수 */
    private static final int NOTION_BLOCK_APPEND_LIMIT = 100;

    public void syncPortfolioToNotion(Portfolio portfolio) {
        String accessToken = portfolio.getNotionAccessToken();
        if (accessToken != null && !accessToken.isBlank()) {
            syncWithOAuthToken(portfolio, accessToken);
            return;
        }
        if (!portfolioEnabled || integrationToken == null || integrationToken.isBlank()
                || parentPageId == null || parentPageId.isBlank()) {
            return;
        }
        String pageId = portfolio.getNotionPageId();
        if (pageId != null && !pageId.isBlank()) {
            updateNotionPage(portfolio);
        } else {
            createNotionPage(portfolio);
        }
    }

    @SuppressWarnings("unchecked")
    private void syncWithOAuthToken(Portfolio portfolio, String accessToken) {
        String pageId = portfolio.getNotionPageId();
        if (pageId != null && !pageId.isBlank()) {
            updateNotionPageWithToken(portfolio, accessToken);
            return;
        }
        String parentPageId = findFirstPageWithToken(accessToken);
        if (parentPageId == null) {
            log.warn("Notion OAuth: 접근 가능한 페이지 없음. 포트폴리오 페이지 생성 불가.");
            return;
        }
        createNotionPageWithToken(portfolio, accessToken, parentPageId);
    }

    @SuppressWarnings("unchecked")
    private String findFirstPageWithToken(String accessToken) {
        RestTemplate rest = new RestTemplate();
        HttpHeaders headers = buildHeadersWithToken(accessToken);
        Map<String, Object> searchBody = Map.of("page_size", 100);
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(searchBody, headers);
        ResponseEntity<Map> res = rest.exchange(NOTION_SEARCH_URL, HttpMethod.POST, req, Map.class);
        Map<String, Object> body = res.getBody();
        if (body == null) return null;
        Object resultsObj = body.get("results");
        if (!(resultsObj instanceof List)) return null;
        List<Map<String, Object>> results = (List<Map<String, Object>>) resultsObj;
        for (Map<String, Object> item : results) {
            if ("page".equals(item.get("object"))) {
                String id = (String) item.get("id");
                if (id != null) return id;
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void createNotionPageWithToken(Portfolio portfolio, String accessToken, String parentPageId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("parent", Map.of("page_id", parentPageId));
        String title = "포트폴리오 - " + (portfolio.getPortfolioUserName() != null ? portfolio.getPortfolioUserName() : "이름 없음");
        Map<String, Object> titleProp = Map.of("title", List.of(
                Map.<String, Object>of("text", Map.of("content", truncate(title, 2000)))));
        body.put("properties", Map.of("title", titleProp));
        List<Map<String, Object>> children = buildPortfolioBlocks(portfolio);
        if (!children.isEmpty()) body.put("children", children);

        RestTemplate rest = new RestTemplate();
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeadersWithToken(accessToken));
        ResponseEntity<Map> response = rest.exchange(NOTION_PAGES_URL, HttpMethod.POST, request, Map.class);
        Map<String, Object> result = response.getBody();
        if (result != null) {
            String pageId = (String) result.get("id");
            Object urlObj = result.get("url");
            String pageUrl = urlObj != null ? urlObj.toString() : null;
            if (pageUrl != null && !pageUrl.contains("notion.so")) {
                pageUrl = "https://notion.so/" + (pageId != null ? pageId.replace("-", "") : "");
            }
            portfolio.setNotionPageId(pageId);
            portfolio.setNotionPageUrl(pageUrl);
            log.info("Notion 포트폴리오 페이지 생성(OAuth): portfolioNo={}, pageId={}", portfolio.getPortfolioNo(), pageId);
        }
    }

    private void updateNotionPageWithToken(Portfolio portfolio, String accessToken) {
        String pageId = portfolio.getNotionPageId();
        if (pageId == null || pageId.isBlank()) return;
        try {
            HttpHeaders headers = buildHeadersWithToken(accessToken);
            updateNotionPageTitle(pageId, portfolio.getPortfolioUserName(), headers);
            replacePageContentWithToken(pageId, portfolio, headers);
            log.info("Notion 포트폴리오 페이지 갱신(OAuth): portfolioNo={}, pageId={}", portfolio.getPortfolioNo(), pageId);
        } catch (Exception e) {
            log.warn("Notion 포트폴리오 업데이트 실패: pageId={}, error={}", pageId, e.getMessage());
        }
    }

    private HttpHeaders buildHeadersWithToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
        headers.set("Notion-Version", notionApiVersion);
        return headers;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void createNotionPage(Portfolio portfolio) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("parent", Map.of("page_id", parentPageId.trim()));
        String title = "포트폴리오 - " + (portfolio.getPortfolioUserName() != null ? portfolio.getPortfolioUserName() : "이름 없음");
        Map<String, Object> titleProp = Map.of("title", List.of(
                Map.<String, Object>of("text", Map.of("content", truncate(title, 2000)))));
        body.put("properties", Map.of("title", titleProp));

        List<Map<String, Object>> children = buildPortfolioBlocks(portfolio);
        if (!children.isEmpty()) {
            body.put("children", children);
        }

        RestTemplate rest = new RestTemplate();
        HttpHeaders headers = buildHeaders();
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = rest.exchange(NOTION_PAGES_URL, HttpMethod.POST, request, Map.class);

        Map<String, Object> result = response.getBody();
        if (result != null) {
            String pageId = (String) result.get("id");
            Object urlObj = result.get("url");
            String pageUrl = urlObj != null ? urlObj.toString() : null;
            if (pageUrl != null && !pageUrl.contains("notion.so")) {
                pageUrl = "https://notion.so/" + pageId.replace("-", "");
            }
            portfolio.setNotionPageId(pageId);
            portfolio.setNotionPageUrl(pageUrl);
            log.info("Notion 포트폴리오 페이지 생성: portfolioNo={}, pageId={}", portfolio.getPortfolioNo(), pageId);
        }
    }

    private void updateNotionPage(Portfolio portfolio) {
        String pageId = portfolio.getNotionPageId();
        if (pageId == null || pageId.isBlank()) return;
        try {
            HttpHeaders headers = buildHeaders();
            updateNotionPageTitle(pageId, portfolio.getPortfolioUserName(), headers);
            replacePageContent(pageId, portfolio, headers);
            log.info("Notion 포트폴리오 페이지 갱신: portfolioNo={}, pageId={}", portfolio.getPortfolioNo(), pageId);
        } catch (Exception e) {
            log.warn("Notion 포트폴리오 업데이트 실패: pageId={}, error={}", pageId, e.getMessage());
        }
    }

    private void updateNotionPageTitle(String pageId, String userName, HttpHeaders headers) {
        String title = "포트폴리오 - " + (userName != null ? userName : "이름 없음");
        Map<String, Object> titleProp = Map.of("title", List.of(
                Map.<String, Object>of("text", Map.of("content", truncate(title, 2000)))));
        Map<String, Object> props = Map.of("title", titleProp);
        RestTemplate rest = new RestTemplate();
        rest.exchange(NOTION_PAGES_URL + "/" + pageId, HttpMethod.PATCH,
                new HttpEntity<>(Map.of("properties", props), headers), Map.class);
    }

    @SuppressWarnings("unchecked")
    private void replacePageContentWithToken(String pageId, Portfolio portfolio, HttpHeaders headers) {
        RestTemplate rest = new RestTemplate();
        List<String> childIds = getAllBlockChildIds(rest, pageId, headers);
        for (String blockId : childIds) {
            try {
                rest.exchange(NOTION_BLOCKS_URL + "/" + blockId, HttpMethod.DELETE, new HttpEntity<>(headers), Map.class);
            } catch (Exception e) {
                log.warn("Notion 블록 삭제 실패: blockId={}, error={}", blockId, e.getMessage());
            }
        }
        appendBlocksInBatches(rest, pageId, buildPortfolioBlocks(portfolio), headers);
    }

    @SuppressWarnings("unchecked")
    private void replacePageContent(String pageId, Portfolio portfolio, HttpHeaders headers) {
        RestTemplate rest = new RestTemplate();
        List<String> childIds = getAllBlockChildIds(rest, pageId, headers);
        for (String blockId : childIds) {
            try {
                rest.exchange(NOTION_BLOCKS_URL + "/" + blockId, HttpMethod.DELETE, new HttpEntity<>(headers), Map.class);
            } catch (Exception e) {
                log.warn("Notion 블록 삭제 실패: blockId={}, error={}", blockId, e.getMessage());
            }
        }
        appendBlocksInBatches(rest, pageId, buildPortfolioBlocks(portfolio), headers);
    }

    /** Notion API 제한(요청당 최대 100블록)에 맞춰 나눠서 PATCH 호출 */
    private void appendBlocksInBatches(RestTemplate rest, String pageId, List<Map<String, Object>> children, HttpHeaders headers) {
        if (children == null || children.isEmpty()) return;
        String appendUrl = NOTION_BLOCKS_URL + "/" + pageId + "/children";
        for (int i = 0; i < children.size(); i += NOTION_BLOCK_APPEND_LIMIT) {
            int to = Math.min(i + NOTION_BLOCK_APPEND_LIMIT, children.size());
            List<Map<String, Object>> batch = children.subList(i, to);
            Map<String, Object> body = Map.of("children", batch);
            rest.exchange(appendUrl, HttpMethod.PATCH, new HttpEntity<>(body, headers), Map.class);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getAllBlockChildIds(RestTemplate rest, String blockId, HttpHeaders headers) {
        List<String> ids = new ArrayList<>();
        String cursor = null;
        do {
            String url = NOTION_BLOCKS_URL + "/" + blockId + "/children?page_size=100" + (cursor != null ? "&start_cursor=" + cursor : "");
            ResponseEntity<Map> res = rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<String, Object> body = res.getBody();
            if (body == null) break;
            Object resultsObj = body.get("results");
            if (resultsObj instanceof List) {
                for (Object o : (List<?>) resultsObj) {
                    if (o instanceof Map) {
                        String id = (String) ((Map<?, ?>) o).get("id");
                        if (id != null) ids.add(id);
                    }
                }
            }
            cursor = (String) body.get("next_cursor");
            Boolean hasMore = (Boolean) body.get("has_more");
            if (Boolean.FALSE.equals(hasMore) || cursor == null) break;
        } while (true);
        return ids;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + integrationToken.trim());
        headers.set("Notion-Version", notionApiVersion);
        return headers;
    }

    private List<Map<String, Object>> buildPortfolioBlocks(Portfolio p) {
        List<Map<String, Object>> blocks = new ArrayList<>();

        String profileImageUrl = toPublicProfileImageUrl(p);
        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            blocks.add(imageBlock(profileImageUrl));
        }

        String name = nullToEmpty(p.getPortfolioUserName());
        if (!name.isBlank()) {
            blocks.add(heading1(name));
        }

        // 연락처 | 경력 2단 레이아웃 (column_list)
        String phone = nullToEmpty(p.getPortfolioUserPhone());
        String email = nullToEmpty(p.getPortfolioUserEmail());
        List<Map<String, Object>> contactChildren = List.of(
                heading2("연락처"),
                paragraph("T: " + (phone.isBlank() ? "-" : phone)),
                paragraph("A: -"),
                paragraph("E: " + (email.isBlank() ? "-" : email))
        );

        List<Map<String, Object>> careerChildren = new ArrayList<>();
        careerChildren.add(heading2("경력"));
        if (p.getPortfolioUserCareer() != null && !p.getPortfolioUserCareer().isBlank()) {
            for (String line : p.getPortfolioUserCareer().split("\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) careerChildren.add(bulletedItem(trimmed));
            }
        }
        if (careerChildren.size() == 1) careerChildren.add(paragraph("-"));

        blocks.add(columnListBlock(contactChildren, careerChildren));

        if (p.getPortfolioUserProject() != null && !p.getPortfolioUserProject().isBlank()) {
            blocks.add(heading2("참여 프로젝트"));
            for (String line : p.getPortfolioUserProject().split("\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) blocks.add(bulletedItem(trimmed));
            }
        }
        if (p.getPortfolioUserSkill() != null && !p.getPortfolioUserSkill().isBlank()) {
            blocks.add(heading2("스킬"));
            blocks.add(paragraph(p.getPortfolioUserSkill()));
        }
        return blocks;
    }

    /** column_list: 왼쪽 연락처, 오른쪽 경력 (각 column 최소 1개 자식 필요) */
    private static Map<String, Object> columnListBlock(List<Map<String, Object>> leftChildren, List<Map<String, Object>> rightChildren) {
        Map<String, Object> leftColumn = Map.of(
                "object", "block",
                "type", "column",
                "column", Map.of("children", leftChildren)
        );
        Map<String, Object> rightColumn = Map.of(
                "object", "block",
                "type", "column",
                "column", Map.of("children", rightChildren)
        );
        return Map.of(
                "object", "block",
                "type", "column_list",
                "column_list", Map.of("children", List.of(leftColumn, rightColumn))
        );
    }

    /** 회원 프로필 이미지를 공개 URL로 변환 (Notion 이미지 블록용) */
    private String toPublicProfileImageUrl(Portfolio p) {
        if (p.getMember() == null) return null;
        String path = p.getMember().getMemberProfileImage();
        if (path == null || path.isBlank()) return null;
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        String base = publicBaseUrl != null ? publicBaseUrl.trim() : "http://localhost:8888";
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String segment = path.startsWith("/") ? path.substring(1) : (path.startsWith("uploads/") ? path : "uploads/" + path);
        return base + "/" + segment;
    }

    private static Map<String, Object> imageBlock(String imageUrl) {
        return Map.of(
                "object", "block",
                "type", "image",
                "image", Map.of(
                        "type", "external",
                        "external", Map.of("url", imageUrl)
                )
        );
    }

    private static Map<String, Object> heading1(String text) {
        return Map.of(
                "object", "block",
                "type", "heading_1",
                "heading_1", Map.of("rich_text", List.of(richText(truncate(text, MAX_RICH_TEXT_LENGTH))))
        );
    }

    private static Map<String, Object> heading2(String text) {
        return Map.of(
                "object", "block",
                "type", "heading_2",
                "heading_2", Map.of("rich_text", List.of(richText(truncate(text, MAX_RICH_TEXT_LENGTH))))
        );
    }

    private static Map<String, Object> paragraph(String text) {
        return Map.of(
                "object", "block",
                "type", "paragraph",
                "paragraph", Map.of("rich_text", splitRichText(text))
        );
    }

    private static Map<String, Object> bulletedItem(String text) {
        return Map.of(
                "object", "block",
                "type", "bulleted_list_item",
                "bulleted_list_item", Map.of("rich_text", splitRichText(text))
        );
    }

    private static List<Map<String, Object>> splitRichText(String text) {
        if (text == null || text.isBlank()) return List.of(richText(""));
        List<Map<String, Object>> list = new ArrayList<>();
        int len = text.length();
        for (int i = 0; i < len; i += MAX_RICH_TEXT_LENGTH) {
            list.add(richText(text.substring(i, Math.min(i + MAX_RICH_TEXT_LENGTH, len))));
        }
        return list;
    }

    private static Map<String, Object> richText(String content) {
        return Map.of("type", "text", "text", Map.of("content", content == null ? "" : content));
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
