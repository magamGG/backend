package com.kh.magamGG.domain.member.controller;

import com.kh.magamGG.domain.member.dto.EmployeeStatisticsResponseDto;
import com.kh.magamGG.domain.member.dto.MemberMyPageResponseDto;
import com.kh.magamGG.domain.member.dto.MemberUpdateRequestDto;
import com.kh.magamGG.domain.member.dto.request.MemberDeleteRequest;
import com.kh.magamGG.domain.member.dto.request.MemberRequest;
import com.kh.magamGG.domain.member.dto.request.MemberDeleteRequest;
import com.kh.magamGG.domain.member.dto.response.MemberResponse;
import com.kh.magamGG.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 회원가입
     */
    @PostMapping
    public ResponseEntity<MemberResponse> register(@RequestBody MemberRequest request) {
        MemberResponse response = memberService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

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
     * 에이전시별 회원 목록 조회
     */
    @GetMapping("/agency/{agencyNo}")
    public ResponseEntity<List<MemberResponse>> getMembersByAgencyNo(
        @PathVariable Long agencyNo
    ) {
        List<MemberResponse> response = memberService.getMembersByAgencyNo(agencyNo);
        return ResponseEntity.ok(response);
    }

    /**
     * 회원 탈퇴 (비밀번호 확인 필요)
     */
    @DeleteMapping("/{memberNo}")
    public ResponseEntity<Void> deleteMember(
            @PathVariable Long memberNo,
            @RequestBody(required = false) MemberDeleteRequest request
    ) {
        String password = request != null ? request.getPassword() : null;
        memberService.deleteMember(memberNo, password);
        return ResponseEntity.ok().build();
    }
}
