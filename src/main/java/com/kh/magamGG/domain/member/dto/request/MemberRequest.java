package com.kh.magamGG.domain.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MemberRequest {
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 20, message = "이름은 20자 이하여야 합니다.")
    @Pattern(regexp = "^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z]*$", message = "이름은 한글, 영문만 입력 가능합니다.")
    private String memberName;
    
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 50, message = "이메일은 50자 이하여야 합니다.")
    @Pattern(regexp = "^\\S*$", message = "이메일에는 공백을 사용할 수 없습니다.")
    private String memberEmail;
    
    // OAuth 모드에서는 선택적, 일반 모드에서는 필수 (MemberServiceImpl에서 검증)
    @Size(max = 100, message = "비밀번호는 100자 이하여야 합니다.")
    @Pattern(regexp = "^\\S*$", message = "비밀번호에는 공백을 사용할 수 없습니다.")
    private String memberPassword;
    
    @NotBlank(message = "연락처는 필수입니다.")
    @Size(max = 15, message = "연락처는 15자 이하여야 합니다.")
    @Pattern(regexp = "^[0-9\\-]*$", message = "연락처는 숫자와 하이픈(-)만 입력 가능합니다.")
    private String memberPhone;
    
    @Size(max = 100, message = "주소는 100자 이하여야 합니다.")
    private String memberAddress;
    
    @NotBlank(message = "역할은 필수입니다.")
    private String memberRole;

    // 에이전시 회원가입 시 사용
    @Size(max = 30, message = "에이전시명은 30자 이하여야 합니다.")
    private String agencyName;

    // 담당자 회원가입 시 사용 (에이전시 코드로 기존 에이전시 조회)
    private String agencyCode;

    // 아티스트 회원가입 시 선택사항 (에이전시 번호)
    private Long agencyNo;
    
    // OAuth 회원가입 시 사용 (비밀번호 없이 회원가입)
    private String oauthProvider;
}
