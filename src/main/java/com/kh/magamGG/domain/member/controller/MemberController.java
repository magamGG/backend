package com.kh.magamGG.domain.member.controller;

import com.kh.magamGG.domain.member.dto.EmployeeStatisticsResponseDto;
import com.kh.magamGG.domain.member.dto.MemberMyPageResponseDto;
import com.kh.magamGG.domain.member.dto.MemberUpdateRequestDto;
import com.kh.magamGG.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
	
	private final MemberService memberService;
	
	/**
	 * 마이페이지 정보 조회
	 */
	@GetMapping("/{memberNo}")
	public ResponseEntity<MemberMyPageResponseDto> getMyPageInfo(@PathVariable Long memberNo) {
		MemberMyPageResponseDto response = memberService.getMyPageInfo(memberNo);
		return ResponseEntity.ok(response);
	}
	
	/**
	 * 프로필 정보 수정
	 */
	@PutMapping("/{memberNo}")
	public ResponseEntity<Void> updateProfile(
		@PathVariable Long memberNo,
		@RequestBody MemberUpdateRequestDto requestDto
	) {
		memberService.updateProfile(memberNo, requestDto);
		return ResponseEntity.ok().build();
	}
	
	/**
	 * 프로필 이미지 업로드
	 */
	@PostMapping("/{memberNo}/profile-image")
	public ResponseEntity<String> uploadProfileImage(
		@PathVariable Long memberNo,
		@RequestParam("file") MultipartFile file
	) {
		String fileName = memberService.uploadProfileImage(memberNo, file);
		return ResponseEntity.ok(fileName);
	}
	
	/**
	 * 배경 이미지 업로드
	 */
	@PostMapping("/{memberNo}/background-image")
	public ResponseEntity<String> uploadBackgroundImage(
		@PathVariable Long memberNo,
		@RequestParam("file") MultipartFile file
	) {
		String fileName = memberService.uploadBackgroundImage(memberNo, file);
		return ResponseEntity.ok(fileName);
	}
	
	/**
	 * 에이전시별 직원 통계 조회
	 */
	@GetMapping("/agency/{agencyNo}/statistics")
	public ResponseEntity<EmployeeStatisticsResponseDto> getEmployeeStatistics(
		@PathVariable Long agencyNo
	) {
		EmployeeStatisticsResponseDto response = memberService.getEmployeeStatistics(agencyNo);
		return ResponseEntity.ok(response);
	}
	
	/**
	 * 회원 탈퇴
	 */
	@DeleteMapping("/{memberNo}")
	public ResponseEntity<Void> deleteMember(@PathVariable Long memberNo) {
		memberService.deleteMember(memberNo);
		return ResponseEntity.ok().build();
	}
}
