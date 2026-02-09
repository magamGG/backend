package com.kh.magamGG.domain.memo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메모 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemoUpdateRequest {

	private String memoName;
	private String memoText;
}
