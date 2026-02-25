package com.kh.magamGG.domain.project.service;

import com.kh.magamGG.domain.project.entity.KanbanCard;
import com.kh.magamGG.domain.project.entity.Project;
import com.kh.magamGG.domain.project.repository.KanbanCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotionSyncService {

    private final KanbanCardRepository kanbanCardRepository;

    @Value("${notion.api-version:2022-06-28}")
    private String notionApiVersion;

    private static final String NOTION_PAGES_URL = "https://api.notion.com/v1/pages";

    private HttpHeaders buildHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Notion-Version", notionApiVersion);
        return headers;
    }

    @Async
    @Transactional
    @SuppressWarnings("unchecked")
    public void syncCardCreate(KanbanCard card, Project project) {
        syncCardCreateSync(card, project);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public void syncCardCreateSync(KanbanCard card, Project project) {
        if (project.getNotionAccessToken() == null || project.getNotionDatabaseId() == null) {
            return;
        }

        try {
            HttpHeaders headers = buildHeaders(project.getNotionAccessToken());

            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("Name", Map.of("title", List.of(
                    Map.of("text", Map.of("content", card.getKanbanCardName() != null ? card.getKanbanCardName() : ""))
            )));

            if (card.getKanbanBoard() != null && card.getKanbanBoard().getKanbanBoardName() != null) {
                properties.put("Status", Map.of("select", Map.of("name", card.getKanbanBoard().getKanbanBoardName())));
            }

            if (card.getKanbanCardDescription() != null && !card.getKanbanCardDescription().isEmpty()) {
                properties.put("Description", Map.of("rich_text", List.of(
                        Map.of("text", Map.of("content", card.getKanbanCardDescription()))
                )));
            }

            if (card.getKanbanCardEndedAt() != null) {
                properties.put("Due", Map.of("date", Map.of("start", card.getKanbanCardEndedAt().toString())));
            }

            String assigneeName = getAssigneeName(card);
            if (assigneeName != null && !assigneeName.isEmpty()) {
                properties.put("Assignee", Map.of("rich_text", List.of(
                        Map.of("text", Map.of("content", assigneeName))
                )));
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("parent", Map.of("database_id", project.getNotionDatabaseId()));
            body.put("properties", properties);

            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(NOTION_PAGES_URL, HttpMethod.POST, request, Map.class);

            Map<String, Object> result = response.getBody();
            if (result != null) {
                String pageId = (String) result.get("id");
                String pageUrl = (String) result.get("url");
                card.setNotionPageId(pageId);
                card.setNotionPageUrl(pageUrl);
                kanbanCardRepository.save(card);
                log.info("Notion 페이지 생성: cardNo={}, pageId={}", card.getKanbanCardNo(), pageId);
            }
        } catch (Exception e) {
            log.error("Notion 카드 생성 동기화 실패: cardNo={}", card.getKanbanCardNo(), e);
        }
    }

    private String getAssigneeName(KanbanCard card) {
        try {
            if (card.getProjectMember() != null && card.getProjectMember().getMember() != null) {
                return card.getProjectMember().getMember().getMemberName();
            }
        } catch (Exception e) {
            log.debug("담당자 이름 조회 실패 (lazy loading): cardNo={}", card.getKanbanCardNo());
        }
        return null;
    }

    @Async
    @SuppressWarnings("unchecked")
    public void syncCardUpdate(KanbanCard card, Project project) {
        if (project.getNotionAccessToken() == null || card.getNotionPageId() == null) {
            return;
        }

        try {
            HttpHeaders headers = buildHeaders(project.getNotionAccessToken());

            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("Name", Map.of("title", List.of(
                    Map.of("text", Map.of("content", card.getKanbanCardName() != null ? card.getKanbanCardName() : ""))
            )));

            if (card.getKanbanBoard() != null && card.getKanbanBoard().getKanbanBoardName() != null) {
                properties.put("Status", Map.of("select", Map.of("name", card.getKanbanBoard().getKanbanBoardName())));
            }

            if (card.getKanbanCardDescription() != null) {
                properties.put("Description", Map.of("rich_text", List.of(
                        Map.of("text", Map.of("content", card.getKanbanCardDescription()))
                )));
            }

            if (card.getKanbanCardEndedAt() != null) {
                properties.put("Due", Map.of("date", Map.of("start", card.getKanbanCardEndedAt().toString())));
            }

            String assigneeName = getAssigneeName(card);
            if (assigneeName != null && !assigneeName.isEmpty()) {
                properties.put("Assignee", Map.of("rich_text", List.of(
                        Map.of("text", Map.of("content", assigneeName))
                )));
            }

            Map<String, Object> body = Map.of("properties", properties);

            String url = NOTION_PAGES_URL + "/" + card.getNotionPageId();
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.exchange(url, HttpMethod.PATCH, request, Map.class);

            log.info("Notion 페이지 수정: cardNo={}, pageId={}", card.getKanbanCardNo(), card.getNotionPageId());
        } catch (Exception e) {
            log.error("Notion 카드 수정 동기화 실패: cardNo={}", card.getKanbanCardNo(), e);
        }
    }

    @Async
    public void syncCardArchive(KanbanCard card, Project project) {
        if (project.getNotionAccessToken() == null || card.getNotionPageId() == null) {
            return;
        }

        try {
            HttpHeaders headers = buildHeaders(project.getNotionAccessToken());
            Map<String, Object> body = Map.of("archived", true);

            String url = NOTION_PAGES_URL + "/" + card.getNotionPageId();
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.exchange(url, HttpMethod.PATCH, request, Map.class);

            log.info("Notion 페이지 보관: cardNo={}, pageId={}", card.getKanbanCardNo(), card.getNotionPageId());
        } catch (Exception e) {
            log.error("Notion 카드 삭제(보관) 동기화 실패: cardNo={}", card.getKanbanCardNo(), e);
        }
    }
}
