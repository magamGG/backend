package com.kh.magamGG.domain.member.service;

import com.kh.magamGG.domain.member.dto.EmployeeStatisticsResponseDto;
import com.kh.magamGG.domain.member.dto.MemberMyPageResponseDto;
import com.kh.magamGG.domain.member.dto.MemberUpdateRequestDto;
import com.kh.magamGG.domain.member.dto.request.MemberRequest;
import com.kh.magamGG.domain.member.dto.response.MemberResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MemberService {

    /**
     * 회원가입
     */
    MemberResponse register(MemberRequest request);

    /**
     * 마이페이지 정보 조회
     */
    MemberMyPageResponseDto getMyPageInfo(Long memberNo);

    /**
     * 프로필 정보 수정
     */
    void updateProfile(Long memberNo, MemberUpdateRequestDto requestDto);

    /**
     * 프로필 이미지 업로드
     */
    String uploadProfileImage(Long memberNo, MultipartFile file);

    /**
     * 배경 이미지 업로드
     */
    String uploadBackgroundImage(Long memberNo, MultipartFile file);

    /**
     * 에이전시별 직원 통계 조회
     */
    EmployeeStatisticsResponseDto getEmployeeStatistics(Long agencyNo);

    /**
     * 회원 탈퇴 (비밀번호 확인 필요)
     */
    void deleteMember(Long memberNo, String password);

    /**
     * 에이전시별 회원 목록 조회
     */
    List<MemberResponse> getMembersByAgencyNo(Long agencyNo);
}
