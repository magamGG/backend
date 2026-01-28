package com.kh.magamGG.domain.member.service;

import com.kh.magamGG.domain.agency.entity.Agency;
import com.kh.magamGG.domain.agency.repository.AgencyRepository;
import com.kh.magamGG.domain.agency.service.AgencyService;
import com.kh.magamGG.domain.member.dto.EmployeeStatisticsResponseDto;
import com.kh.magamGG.domain.member.dto.MemberMyPageResponseDto;
import com.kh.magamGG.domain.member.dto.MemberUpdateRequestDto;
import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
	
	@Value("${file.upload-dir:uploads}")
	private String uploadDir;
	
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
		
		member.updateProfile(
			requestDto.getMemberName(),
			requestDto.getMemberEmail(),
			requestDto.getMemberPhone(),
			requestDto.getMemberAddress()
		);
		
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
	@Transactional
	public void deleteMember(Long memberNo) {
		Member member = memberRepository.findById(memberNo)
			.orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
		
		// 프로필 이미지 삭제
		if (member.getMemberProfileImage() != null) {
			deleteFile(member.getMemberProfileImage());
		}
		
		// 배경 이미지 삭제
		if (member.getMemberProfileBannerImage() != null) {
			deleteFile(member.getMemberProfileBannerImage());
		}
		
		memberRepository.delete(member);
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
