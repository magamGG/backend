package com.kh.magamGG.domain.memo.dto.response;

import com.kh.magamGG.domain.memo.entity.Memo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 메모 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoResponse {

	private Long memoNo;
	private String memoName;
	private String memoText;
	private String memoType;
	private LocalDateTime memoCreatedAt;
	private LocalDateTime memoUpdatedAt;
	/** 캘린더 메모일 때 해당 날짜 (DB: CALENDAR_MEMO_DATE) */
	private LocalDateTime calendarMemoDate;

	public static MemoResponse fromEntity(Memo entity) {
		return MemoResponse.builder()
				.memoNo(entity.getMemoNo())
				.memoName(entity.getMemoName())
				.memoText(entity.getMemoText())
				.memoType(entity.getMemoType())
				.memoCreatedAt(entity.getMemoCreatedAt())
				.memoUpdatedAt(entity.getMemoUpdatedAt())
				.calendarMemoDate(entity.getCalendarMemoDate())
				.build();
	}
}
