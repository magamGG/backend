package com.kh.magamGG.domain.memo.service;

import com.kh.magamGG.domain.memo.dto.request.MemoCreateRequest;
import com.kh.magamGG.domain.memo.dto.request.MemoUpdateRequest;
import com.kh.magamGG.domain.memo.dto.response.MemoResponse;

import java.util.List;

public interface MemoService {

	/** 개인 메모 저장 (타입 '개인') */
	MemoResponse create(Long memberNo, MemoCreateRequest request);

	/** 로그인 회원의 개인 메모 목록 조회 */
	List<MemoResponse> listPersonal(Long memberNo);

	/** 개인 메모 수정 (본인만) */
	MemoResponse update(Long memberNo, Long memoNo, MemoUpdateRequest request);

	/** 개인 메모 삭제 (본인만) */
	void delete(Long memberNo, Long memoNo);
}


