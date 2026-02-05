package com.kh.magamGG.domain.memo.controller;

import com.kh.magamGG.domain.memo.dto.request.MemoCreateRequest;
import com.kh.magamGG.domain.memo.dto.request.MemoUpdateRequest;
import com.kh.magamGG.domain.memo.dto.response.MemoResponse;
import com.kh.magamGG.domain.memo.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memo")
@RequiredArgsConstructor
public class MemoController {

	private final MemoService memoService;

	/**
	 * 개인 메모 저장 (타입 '개인')
	 * POST /api/memo
	 */
	@PostMapping
	public ResponseEntity<MemoResponse> create(
			@RequestBody MemoCreateRequest request,
			@RequestHeader("X-Member-No") Long memberNo) {
		MemoResponse response = memoService.create(memberNo, request);
		return ResponseEntity.ok(response);
	}

	/**
	 * 로그인 회원의 개인 메모 목록 조회
	 * GET /api/memo
	 */
	@GetMapping
	public ResponseEntity<List<MemoResponse>> listPersonal(
			@RequestHeader("X-Member-No") Long memberNo) {
		List<MemoResponse> list = memoService.listPersonal(memberNo);
		return ResponseEntity.ok(list);
	}

	/**
	 * 로그인 회원의 캘린더 메모 목록 조회 (타입 '캘린더')
	 * GET /api/memo/calendar
	 */
	@GetMapping("/calendar")
	public ResponseEntity<List<MemoResponse>> listCalendar(
			@RequestHeader("X-Member-No") Long memberNo) {
		List<MemoResponse> list = memoService.listCalendar(memberNo);
		return ResponseEntity.ok(list);
	}

	/**
	 * 개인 메모 수정
	 * PUT /api/memo/{memoNo}
	 */
	@PutMapping("/{memoNo}")
	public ResponseEntity<MemoResponse> update(
			@PathVariable Long memoNo,
			@RequestBody MemoUpdateRequest request,
			@RequestHeader("X-Member-No") Long memberNo) {
		MemoResponse response = memoService.update(memberNo, memoNo, request);
		return ResponseEntity.ok(response);
	}

	/**
	 * 개인 메모 삭제
	 * DELETE /api/memo/{memoNo}
	 */
	@DeleteMapping("/{memoNo}")
	public ResponseEntity<Void> delete(
			@PathVariable Long memoNo,
			@RequestHeader("X-Member-No") Long memberNo) {
		memoService.delete(memberNo, memoNo);
		return ResponseEntity.noContent().build();
	}
}
