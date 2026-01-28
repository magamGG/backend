package com.kh.magamGG.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberUpdateRequestDto {
	private String memberName;
	private String memberPhone;
	private String memberAddress;
	private String agencyName;  // 소속(스튜디오) - 에이전시 대표만 수정 가능
	private String memberPassword;  // 비밀번호 변경용 (null이면 변경 안 함)
}

