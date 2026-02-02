package com.kh.magamGG.domain.project.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 칸반 카드 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KanbanCardResponse {
    private Long id;
    private String title;
    private String description;
    private String startDate;
    private String dueDate;
    private Long boardId;
    private boolean completed;
    private AssigneeInfo assignedTo;
    private String createdAt;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssigneeInfo {
        private Long id;
        private String name;
        private String role;
        private String email;
        private String avatar;
    }
}
