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
     * 회원 탈퇴
     */
    void deleteMember(Long memberNo);

    /**
     * 에이전시별 회원 목록 조회
     */
    List<MemberResponse> getMembersByAgencyNo(Long agencyNo);

    /**
     * 회원 상세 정보 조회 (프로젝트, 건강 체크 등)
     */
    com.kh.magamGG.domain.member.dto.response.MemberDetailResponse getMemberDetails(Long memberNo);

    /**
     * 에이전시별 담당자 목록 조회
     */
    List<MemberResponse> getManagersByAgencyNo(Long agencyNo);

    /**
     * 에이전시별 작가 목록 조회
     */
    List<MemberResponse> getArtistsByAgencyNo(Long agencyNo);

    /**
     * 작가를 담당자에게 배정
     */
    void assignArtistToManager(Long artistNo, Long managerNo);

    /**
     * 작가의 담당자 배정 해제
     */
    void unassignArtistFromManager(Long artistNo);
}
