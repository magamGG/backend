package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.project.entity.KanbanCard;
import com.kh.magamGG.domain.project.entity.Project;
import com.kh.magamGG.domain.project.repository.KanbanCardRepository;
import com.kh.magamGG.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotionAuthService {

    private final ProjectRepository projectRepository;
    private final KanbanCardRepository kanbanCardRepository;
    private final NotionSyncService notionSyncService;

    @Value("${notion.client-id}")
    private String clientId;

    @Value("${notion.client-secret}")
    private String clientSecret;

    @Value("${notion.redirect-uri}")
    private String redirectUri;

    @Value("${notion.api-version:2022-06-28}")
    private String notionApiVersion;

    private static final String NOTION_TOKEN_URL = "https://api.notion.com/v1/oauth/token";
    private static final String NOTION_SEARCH_URL = "https://api.notion.com/v1/search";
    private static final String NOTION_DATABASES_URL = "https://api.notion.com/v1/databases";

    public String getClientId() {
        return clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    private HttpHeaders buildBearerHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Notion-Version", notionApiVersion);
        return headers;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> exchangeCodeAndSave(Long projectNo, String code) {
        Project project = projectRepository.findById(projectNo)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + projectNo));

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
        String duplicatedTemplateId = (String) tokenResponse.get("duplicated_template_id");

        project.setNotionAccessToken(accessToken);
        project.setNotionWorkspaceName(workspaceName);

        String dbStatus = "ok";
        if (duplicatedTemplateId != null) {
            project.setNotionDatabaseId(duplicatedTemplateId);
            dbStatus = "template";
        } else {
            try {
                String databaseId = findOrCreateDatabase(accessToken, project.getProjectName());
                if (databaseId != null) {
                    project.setNotionDatabaseId(databaseId);
                    dbStatus = "created";
                } else {
                    dbStatus = "no_pages_shared";
                    log.warn("Notion DB 미생성: 공유된 페이지 없음 (projectNo={})", projectNo);
                }
            } catch (Exception e) {
                dbStatus = "db_error";
                log.error("Notion DB 생성 중 예외: projectNo={}", projectNo, e);
            }
        }

        projectRepository.save(project);
        log.info("Notion 연동 완료: projectNo={}, workspace={}, databaseId={}, dbStatus={}",
                projectNo, workspaceName, project.getNotionDatabaseId(), dbStatus);

        int syncedCount = 0;
        if (project.getNotionDatabaseId() != null) {
            syncedCount = syncExistingCardsCount(project);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workspaceName", workspaceName != null ? workspaceName : "");
        result.put("databaseId", project.getNotionDatabaseId() != null ? project.getNotionDatabaseId() : "");
        result.put("dbStatus", dbStatus);
        result.put("syncedCards", syncedCount);
        return result;
    }

    @SuppressWarnings("unchecked")
    private String findOrCreateDatabase(String accessToken, String projectName) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = buildBearerHeaders(accessToken);

        // 1단계: Search API로 접근 가능한 모든 항목 조회 (필터 없이)
        String parentPageId = null;
        try {
            Map<String, Object> searchBody = new LinkedHashMap<>();
            searchBody.put("page_size", 100);

            HttpEntity<Map<String, Object>> searchReq = new HttpEntity<>(searchBody, headers);
            ResponseEntity<Map> searchRes = restTemplate.exchange(
                    NOTION_SEARCH_URL, HttpMethod.POST, searchReq, Map.class);
            Map<String, Object> searchResult = searchRes.getBody();

            if (searchResult != null) {
                List<Map<String, Object>> results =
                        (List<Map<String, Object>>) searchResult.get("results");
                log.info("[Notion] Search 결과: {}건", results != null ? results.size() : 0);

                if (results != null) {
                    for (Map<String, Object> item : results) {
                        String objectType = (String) item.get("object");
                        String id = (String) item.get("id");
                        log.info("[Notion]   - type={}, id={}", objectType, id);

                        if ("page".equals(objectType) && parentPageId == null) {
                            parentPageId = id;
                        }
                    }
                }
            } else {
                log.warn("[Notion] Search 응답 body가 null");
            }
        } catch (Exception e) {
            log.error("[Notion] Search API 호출 실패", e);
        }

        // 2단계: 부모 페이지가 없으면 생성 불가
        if (parentPageId == null) {
            log.warn("[Notion] 접근 가능한 페이지 없음 → DB 생성 불가. OAuth 시 페이지 공유 필요.");
            return null;
        }

        log.info("[Notion] 부모 페이지 선택: {}", parentPageId);

        // 3단계: 항상 프로젝트 전용 데이터베이스 새로 생성
        try {
            return createNotionDatabase(accessToken, parentPageId, projectName);
        } catch (Exception e) {
            log.error("[Notion] DB 생성 실패: parentPageId={}", parentPageId, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String createNotionDatabase(String accessToken, String parentPageId, String projectName) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = buildBearerHeaders(accessToken);

        String dbTitle = (projectName != null ? projectName : "마감지기") + " - 업무 보드";
        log.info("[Notion] DB 생성 시도: title='{}', parent={}", dbTitle, parentPageId);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("Name", Map.of("title", Map.of()));
        properties.put("Status", Map.of("select", Map.of("options", List.of(
                Map.of("name", "진행중", "color", "blue"),
                Map.of("name", "완료", "color", "green"),
                Map.of("name", "보류", "color", "yellow")
        ))));
        properties.put("Description", Map.of("rich_text", Map.of()));
        properties.put("Due", Map.of("date", Map.of()));
        properties.put("Assignee", Map.of("rich_text", Map.of()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("parent", Map.of("type", "page_id", "page_id", parentPageId));
        body.put("title", List.of(Map.of("type", "text", "text", Map.of("content", dbTitle))));
        body.put("properties", properties);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                NOTION_DATABASES_URL, HttpMethod.POST, request, Map.class);

        log.info("[Notion] DB 생성 API 응답 status: {}", response.getStatusCode());
        Map<String, Object> result = response.getBody();

        if (result != null) {
            String dbId = (String) result.get("id");
            String dbUrl = (String) result.get("url");
            log.info("[Notion] DB 생성 완료: id={}, url={}, title='{}'", dbId, dbUrl, dbTitle);
            return dbId;
        }
        log.warn("[Notion] DB 생성 응답 body가 null");
        return null;
    }

    private int syncExistingCardsCount(Project project) {
        try {
            List<KanbanCard> cards = kanbanCardRepository
                    .findByProjectNoWithBoardAndMember(project.getProjectNo());
            int synced = 0;
            for (KanbanCard card : cards) {
                if ("D".equals(card.getKanbanCardStatus())) continue;
                if (card.getNotionPageId() != null) continue;
                notionSyncService.syncCardCreateSync(card, project);
                synced++;
            }
            log.info("기존 칸반 카드 {}건 Notion 동기화 완료 (project={})", synced, project.getProjectNo());
            return synced;
        } catch (Exception e) {
            log.error("기존 카드 Notion 동기화 실패: projectNo={}", project.getProjectNo(), e);
            return 0;
        }
    }

    @Transactional
    public void saveDatabaseId(Long projectNo, String databaseId) {
        Project project = projectRepository.findById(projectNo)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + projectNo));
        project.setNotionDatabaseId(databaseId);
        projectRepository.save(project);
    }

    @Transactional
    public void disconnectNotion(Long projectNo) {
        Project project = projectRepository.findById(projectNo)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + projectNo));

        List<KanbanCard> cards = kanbanCardRepository.findByProjectNo(projectNo);
        for (KanbanCard card : cards) {
            if (card.getNotionPageId() != null) {
                card.setNotionPageId(null);
                card.setNotionPageUrl(null);
            }
        }
        kanbanCardRepository.saveAll(cards);

        project.setNotionAccessToken(null);
        project.setNotionDatabaseId(null);
        project.setNotionWorkspaceName(null);
        projectRepository.save(project);
        log.info("Notion 연동 해제: projectNo={} (카드 {}건 초기화)", projectNo, cards.size());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getNotionStatus(Long projectNo) {
        Project project = projectRepository.findById(projectNo)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + projectNo));
        boolean connected = project.getNotionAccessToken() != null
                && !project.getNotionAccessToken().isEmpty();
        return Map.of(
                "connected", connected,
                "workspaceName", project.getNotionWorkspaceName() != null
                        ? project.getNotionWorkspaceName() : "",
                "databaseId", project.getNotionDatabaseId() != null
                        ? project.getNotionDatabaseId() : ""
        );
    }

    @Transactional
    public int manualSyncCards(Long projectNo) {
        Project project = projectRepository.findById(projectNo)
                .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + projectNo));

        if (project.getNotionAccessToken() == null || project.getNotionDatabaseId() == null) {
            throw new IllegalStateException("Notion 연동이 완료되지 않았습니다.");
        }

        List<KanbanCard> cards = kanbanCardRepository
                .findByProjectNoWithBoardAndMember(projectNo);
        int synced = 0;
        for (KanbanCard card : cards) {
            if ("D".equals(card.getKanbanCardStatus())) continue;
            if (card.getNotionPageId() != null) continue;
            notionSyncService.syncCardCreateSync(card, project);
            synced++;
        }
        log.info("수동 Notion 동기화: {}건 (projectNo={})", synced, projectNo);
        return synced;
    }
}
