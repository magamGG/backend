package com.kh.magamGG.domain.member.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberUpdateRequestDto {
	@Size(max = 20, message = "이름은 20자 이하여야 합니다.")
	@Pattern(regexp = "^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z]*$", message = "이름은 한글, 영문만 입력 가능합니다.")
	private String memberName;
	
	@Size(max = 15, message = "연락처는 15자 이하여야 합니다.")
	@Pattern(regexp = "^[0-9\\-]*$", message = "연락처는 숫자와 하이픈(-)만 입력 가능합니다.")
	private String memberPhone;
	
	@Size(max = 100, message = "주소는 100자 이하여야 합니다.")
	private String memberAddress;
	
	@Size(max = 30, message = "에이전시명은 30자 이하여야 합니다.")
	private String agencyName;  // 소속(스튜디오) - 에이전시 대표만 수정 가능
	
	@Size(max = 100, message = "비밀번호는 100자 이하여야 합니다.")
	@Pattern(regexp = "^\\S*$", message = "비밀번호에는 공백을 사용할 수 없습니다.")
	private String memberPassword;  // 비밀번호 변경용 (null이면 변경 안 함)
}

