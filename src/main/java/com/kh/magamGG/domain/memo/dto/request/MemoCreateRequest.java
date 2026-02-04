package com.kh.magamGG.domain.memo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메모 생성 요청 DTO
 * MEMO 테이블 저장 시 사용 (타입 '개인')
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoCreateRequest {

	/** 메모 제목 (DB: MEMO_NAME, max 30) */
	private String memoName;

	/** 메모 내용 (DB: MEMO_TEXT, max 255) */
	private String memoText;
}
