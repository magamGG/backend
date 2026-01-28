package com.kh.magamGG.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberUpdateRequestDto {
	private String memberName;
	private String memberEmail;
	private String memberPhone;
	private String memberAddress;
	private String agencyName;  // 소속(스튜디오) - 에이전시 대표만 수정 가능
}

