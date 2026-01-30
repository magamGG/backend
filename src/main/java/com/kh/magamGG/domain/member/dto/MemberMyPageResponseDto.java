package com.kh.magamGG.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberMyPageResponseDto {
	private Long memberNo;
	private String memberName;
	private String memberEmail;
	private String memberPhone;
	private String memberAddress;
	private String memberProfileImage;
	private String memberProfileBannerImage;
	private String memberRole;
	
	// Agency 정보
	private Long agencyNo;
	private String agencyName;
	private String agencyCode;
}

