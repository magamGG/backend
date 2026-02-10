package com.kh.magamGG.domain.member.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberUpdateRequestDto {
	@Size(max = 20, message = "이름은 20자 이하여야 합니다.")
	private String memberName;
	
	@Size(max = 15, message = "연락처는 15자 이하여야 합니다.")
	private String memberPhone;
	
	@Size(max = 100, message = "주소는 100자 이하여야 합니다.")
	private String memberAddress;
	
	@Size(max = 30, message = "에이전시명은 30자 이하여야 합니다.")
	private String agencyName;  // 소속(스튜디오) - 에이전시 대표만 수정 가능
	
	@Size(max = 100, message = "비밀번호는 100자 이하여야 합니다.")
	private String memberPassword;  // 비밀번호 변경용 (null이면 변경 안 함)
}

