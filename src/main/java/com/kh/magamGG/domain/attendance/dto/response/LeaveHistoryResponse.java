package com.kh.magamGG.domain.attendance.dto.response;

import com.kh.magamGG.domain.attendance.entity.LeaveHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 연차 변경 이력 응답 DTO
 * DB: LEAVE_HISTORY
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveHistoryResponse {

    private Long id;
    private String date;
    private String target;
    private String type;
    private Integer changedDays;
    private String reason;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static LeaveHistoryResponse fromEntity(LeaveHistory entity) {
        String dateStr = entity.getLeaveHistoryDate() != null
                ? entity.getLeaveHistoryDate().format(FORMATTER)
                : "";
        String targetName = entity.getMember() != null ? entity.getMember().getMemberName() : "-";
        return LeaveHistoryResponse.builder()
                .id(entity.getLeaveHistoryNo())
                .date(dateStr)
                .target(targetName)
                .type(entity.getLeaveHistoryType() != null ? entity.getLeaveHistoryType() : "")
                .changedDays(entity.getLeaveHistoryAmount() != null ? entity.getLeaveHistoryAmount() : 0)
                .reason(entity.getLeaveHistoryReason() != null ? entity.getLeaveHistoryReason() : "")
                .build();
    }
}
