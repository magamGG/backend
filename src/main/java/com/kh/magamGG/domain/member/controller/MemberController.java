package com.kh.magamGG.domain.member.controller;

import com.kh.magamGG.domain.member.dto.EmployeeStatisticsResponseDto;
import com.kh.magamGG.domain.member.dto.MemberMyPageResponseDto;
import com.kh.magamGG.domain.member.dto.MemberUpdateRequestDto;
import com.kh.magamGG.domain.member.dto.request.MemberRequest;
import com.kh.magamGG.domain.member.dto.response.MemberDetailResponse;
import com.kh.magamGG.domain.member.dto.response.MemberResponse;
import com.kh.magamGG.domain.member.dto.response.WorkingArtistResponse;
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
     * 로그인 회원의 배정 작가 목록 (MANAGER·ARTIST_ASSIGNMENT 조회, 담당자만 해당)
     */
    @GetMapping("/me/assigned-artists")
    public ResponseEntity<List<MemberResponse>> getMyAssignedArtists(
        @RequestHeader("X-Member-No") Long memberNo
    ) {
        List<MemberResponse> response = memberService.getAssignedArtistsByMemberNo(memberNo);
        return ResponseEntity.ok(response);
    }

    /**
     * 담당자별 작가 목록 조회 (ARTIST_ASSIGNMENT 테이블에서 managerNo로 조회)
     * 더 구체적인 경로를 먼저 배치하여 경로 매칭 충돌 방지
     */
    @GetMapping("/manager/{managerNo}/artists")
    public ResponseEntity<List<MemberResponse>> getArtistsByManagerNo(
        @PathVariable Long managerNo
    ) {
        List<MemberResponse> response = memberService.getArtistsByManagerNo(managerNo);
        return ResponseEntity.ok(response);
    }

    /**
     * 담당자별 현재 작업중인 작가 목록 (배정 작가 중 오늘 출근 중인 사람만)
     */
    @GetMapping("/manager/{managerNo}/working-artists")
    public ResponseEntity<List<WorkingArtistResponse>> getWorkingArtistsByManagerNo(
        @PathVariable Long managerNo
    ) {
        List<WorkingArtistResponse> response = memberService.getWorkingArtistsByManagerNo(managerNo);
        return ResponseEntity.ok(response);
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
     * 회원 탈퇴
     */
    @DeleteMapping("/{memberNo}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long memberNo) {
        memberService.deleteMember(memberNo);
        return ResponseEntity.ok().build();
    }

    /**
     * 회원 상세 정보 조회 (프로젝트, 건강 체크 등)
     */
    @GetMapping("/{memberNo}/details")
    public ResponseEntity<MemberDetailResponse> getMemberDetails(
        @PathVariable Long memberNo
    ) {
        MemberDetailResponse response = memberService.getMemberDetails(memberNo);
        return ResponseEntity.ok(response);
    }

    /**
     * 에이전시별 담당자 목록 조회
     */
    @GetMapping("/agency/{agencyNo}/managers")
    public ResponseEntity<List<MemberResponse>> getManagersByAgencyNo(
        @PathVariable Long agencyNo
    ) {
        List<MemberResponse> response = memberService.getManagersByAgencyNo(agencyNo);
        return ResponseEntity.ok(response);
    }

    /**
     * 에이전시별 작가 목록 조회
     */
    @GetMapping("/agency/{agencyNo}/artists")
    public ResponseEntity<List<MemberResponse>> getArtistsByAgencyNo(
        @PathVariable Long agencyNo
    ) {
        List<MemberResponse> response = memberService.getArtistsByAgencyNo(agencyNo);
        return ResponseEntity.ok(response);
    }

    /**
     * 작가를 담당자에게 배정
     */
    @PostMapping("/{artistNo}/assign/{managerNo}")
    public ResponseEntity<Void> assignArtistToManager(
        @PathVariable Long artistNo,
        @PathVariable Long managerNo
    ) {
        memberService.assignArtistToManager(artistNo, managerNo);
        return ResponseEntity.ok().build();
    }

    /**
     * 작가의 담당자 배정 해제
     */
    @DeleteMapping("/{artistNo}/assign")
    public ResponseEntity<Void> unassignArtistFromManager(
        @PathVariable Long artistNo
    ) {
        memberService.unassignArtistFromManager(artistNo);
        return ResponseEntity.ok().build();
    }

    /**
     * 회원을 에이전시에서 제거 (agencyNo를 null로 설정)
     * 더 구체적인 경로를 나중에 배치하여 경로 매칭 충돌 방지
     */
    @PutMapping("/{memberNo}/remove-from-agency")
    public ResponseEntity<Void> removeFromAgency(
        @PathVariable Long memberNo
    ) {
        memberService.removeFromAgency(memberNo);
        return ResponseEntity.ok().build();
    }
}
