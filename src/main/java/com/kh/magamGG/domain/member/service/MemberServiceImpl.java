package com.kh.magamGG.domain.member.service;

import com.kh.magamGG.domain.agency.entity.Agency;
import com.kh.magamGG.domain.agency.repository.AgencyRepository;
import com.kh.magamGG.domain.agency.service.AgencyService;
import com.kh.magamGG.domain.agency.util.AgencyCodeGenerator;
import com.kh.magamGG.domain.member.dto.EmployeeStatisticsResponseDto;
import com.kh.magamGG.domain.member.dto.MemberMyPageResponseDto;
import com.kh.magamGG.domain.member.dto.MemberUpdateRequestDto;
import com.kh.magamGG.domain.member.dto.request.MemberRequest;
import com.kh.magamGG.domain.member.dto.response.MemberResponse;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import com.kh.magamGG.global.exception.AgencyNotFoundException;
import com.kh.magamGG.global.exception.DuplicateAgencyCodeException;
import com.kh.magamGG.global.exception.DuplicateEmailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final AgencyRepository agencyRepository;
    private final AgencyService agencyService;
    private final PasswordEncoder passwordEncoder;
    private final AgencyCodeGenerator agencyCodeGenerator;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    @Transactional
    public MemberResponse register(MemberRequest request) {
        // 이메일 중복 체크
        if (memberRepository.existsByMemberEmail(request.getMemberEmail())) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다.");
        }

        // 에이전시 처리
        Agency agency = null;
        String agencyCode = null;

        if (request.getAgencyName() != null && !request.getAgencyName().trim().isEmpty()) {
            // 에이전시 관리자인 경우: 새 에이전시 생성
            if (agencyRepository.existsByAgencyName(request.getAgencyName())) {
                throw new DuplicateAgencyCodeException("이미 존재하는 에이전시 이름입니다.");
            }

            agencyCode = agencyCodeGenerator.generateUniqueAgencyCode();
            agency = Agency.builder()
                .agencyName(request.getAgencyName())
                .agencyCode(agencyCode)
                .agencyLeave(15) // 기본 연차 15일
                .build();
            agency = agencyRepository.save(agency);
        } else if (request.getAgencyCode() != null && !request.getAgencyCode().trim().isEmpty()) {
            // 담당자인 경우: 에이전시 코드로 기존 에이전시 찾기
            agency = agencyService.getAgencyByCode(request.getAgencyCode());
            agencyCode = agency.getAgencyCode();
        } else if (request.getAgencyNo() != null) {
            // 아티스트인 경우: agencyNo로 직접 찾기
            agency = agencyService.getAgency(request.getAgencyNo());
            if (agency != null) {
                agencyCode = agency.getAgencyCode();
            }
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getMemberPassword());

        // Member 엔티티 생성
        Member member = new Member();
        member.setMemberName(request.getMemberName());
        member.setMemberEmail(request.getMemberEmail());
        member.setMemberPassword(encodedPassword);
        member.setMemberPhone(request.getMemberPhone());
        member.setMemberRole(request.getMemberRole());
        member.setMemberStatus("ACTIVE");
        member.setAgency(agency);
        member.setMemberCreatedAt(LocalDateTime.now());
        member.setMemberUpdatedAt(LocalDateTime.now());

        Member savedMember = memberRepository.save(member);

        return MemberResponse.builder()
            .memberNo(savedMember.getMemberNo())
            .memberName(savedMember.getMemberName())
            .memberEmail(savedMember.getMemberEmail())
            .memberPhone(savedMember.getMemberPhone())
            .memberStatus(savedMember.getMemberStatus())
            .memberRole(savedMember.getMemberRole())
            .memberProfileImage(savedMember.getMemberProfileImage())
            .memberProfileBannerImage(savedMember.getMemberProfileBannerImage())
            .agencyNo(savedMember.getAgency() != null ? savedMember.getAgency().getAgencyNo() : null)
            .agencyCode(agencyCode)
            .memberCreatedAt(savedMember.getMemberCreatedAt())
            .memberUpdatedAt(savedMember.getMemberUpdatedAt())
            .build();
    }

    @Override
    public MemberMyPageResponseDto getMyPageInfo(Long memberNo) {
        Member member = memberRepository.findById(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        Agency agency = member.getAgency();

        return MemberMyPageResponseDto.builder()
            .memberNo(member.getMemberNo())
            .memberName(member.getMemberName())
            .memberEmail(member.getMemberEmail())
            .memberPhone(member.getMemberPhone())
            .memberAddress(member.getMemberAddress())
            .memberProfileImage(member.getMemberProfileImage())
            .memberProfileBannerImage(member.getMemberProfileBannerImage())
            .memberRole(member.getMemberRole())
            .agencyNo(agency != null ? agency.getAgencyNo() : null)
            .agencyName(agency != null ? agency.getAgencyName() : null)
            .agencyCode(agency != null ? agency.getAgencyCode() : null)
            .build();
    }

    @Override
    @Transactional
    public void updateProfile(Long memberNo, MemberUpdateRequestDto requestDto) {
        Member member = memberRepository.findById(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 이메일은 변경하지 않음 (로그인 ID이므로)
        member.updateProfile(
            requestDto.getMemberName(),
            member.getMemberEmail(), // 기존 이메일 유지
            requestDto.getMemberPhone(),
            requestDto.getMemberAddress()
        );

        // 비밀번호 변경 (제공된 경우에만)
        if (requestDto.getMemberPassword() != null && !requestDto.getMemberPassword().trim().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(requestDto.getMemberPassword());
            member.updatePassword(encodedPassword);
        }

        memberRepository.save(member);

        // 에이전시 대표인 경우 소속(스튜디오) 수정
        if (requestDto.getAgencyName() != null && !requestDto.getAgencyName().isEmpty()) {
            Agency agency = member.getAgency();
            if (agency != null && "에이전시 대표".equals(member.getMemberRole())) {
                agencyService.updateAgencyName(agency.getAgencyNo(), requestDto.getAgencyName());
            }
        }
    }

    @Override
    @Transactional
    public String uploadProfileImage(Long memberNo, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 없습니다.");
        }

        Member member = memberRepository.findById(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        String fileName = saveFile(file);
        member.updateProfileImage(fileName);
        memberRepository.save(member);

        return fileName;
    }

    @Override
    @Transactional
    public String uploadBackgroundImage(Long memberNo, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 없습니다.");
        }

        Member member = memberRepository.findById(memberNo)
            .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        String fileName = saveFile(file);
        member.updateBackgroundImage(fileName);
        memberRepository.save(member);

        return fileName;
    }

    @Override
    public EmployeeStatisticsResponseDto getEmployeeStatistics(Long agencyNo) {
        List<Object[]> results = memberRepository.countByAgencyNoAndMemberRole(agencyNo);

        List<EmployeeStatisticsResponseDto.RoleCount> roleCounts = results.stream()
            .map(result -> EmployeeStatisticsResponseDto.RoleCount.builder()
                .role((String) result[0])
                .count((Long) result[1])
                .build())
            .collect(Collectors.toList());

        Integer totalCount = roleCounts.stream()
            .mapToInt(count -> count.getCount().intValue())
            .sum();

        return EmployeeStatisticsResponseDto.builder()
            .roleCounts(roleCounts)
            .totalCount(totalCount)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> getMembersByAgencyNo(Long agencyNo) {
        List<Member> members = memberRepository.findByAgency_AgencyNo(agencyNo);

        return members.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteMember(Long memberNo, String password) {
        Member member = memberRepository.findById(memberNo)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 이미 탈퇴한 회원인지 확인
        if ("BLOCKED".equals(member.getMemberStatus())) {
            throw new IllegalArgumentException("이미 탈퇴 처리된 회원입니다.");
        }

        // 비밀번호 확인
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }

        if (!passwordEncoder.matches(password, member.getMemberPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 실제 삭제 대신 상태를 BLOCKED로 변경
        member.updateStatus("BLOCKED");
        memberRepository.save(member);
    }

    private MemberResponse convertToResponse(Member member) {
        return MemberResponse.builder()
            .memberNo(member.getMemberNo())
            .memberName(member.getMemberName())
            .memberEmail(member.getMemberEmail())
            .memberPhone(member.getMemberPhone())
            .memberStatus(member.getMemberStatus())
            .memberRole(member.getMemberRole())
            .memberProfileImage(member.getMemberProfileImage())
            .memberProfileBannerImage(member.getMemberProfileBannerImage())
            .agencyNo(member.getAgency() != null ? member.getAgency().getAgencyNo() : null)
            .agencyCode(member.getAgency() != null ? member.getAgency().getAgencyCode() : null)
            .memberCreatedAt(member.getMemberCreatedAt())
            .memberUpdatedAt(member.getMemberUpdatedAt())
            .build();
    }

    private String saveFile(MultipartFile file) {
        try {
            // 업로드 디렉토리 생성
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 파일명 생성 (UUID + 원본 파일명)
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
            String fileName = UUID.randomUUID().toString() + extension;

            // 파일 저장
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            return fileName;
        } catch (IOException e) {
            log.error("파일 저장 실패: {}", e.getMessage());
            throw new RuntimeException("파일 저장에 실패했습니다.", e);
        }
    }

    private void deleteFile(String fileName) {
        try {
            Path filePath = Paths.get(uploadDir, fileName);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            log.error("파일 삭제 실패: {}", e.getMessage());
        }
    }
}
