package com.kh.magamGG.domain.agency.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class JoinRequestRequest {
    @NotBlank(message = "에이전시 코드는 필수입니다.")
    @Size(max = 11, message = "에이전시 코드는 11자 이하여야 합니다.")
    @Pattern(regexp = "^\\d{1,11}$", message = "에이전시 코드는 숫자만 입력 가능합니다.")
    private String agencyCode;
    
    @Size(max = 20, message = "이름은 20자 이하여야 합니다.")
    private String memberName;
    
    @Size(max = 50, message = "이메일은 50자 이하여야 합니다.")
    @Pattern(regexp = "^\\S*$", message = "이메일에는 공백을 사용할 수 없습니다.")
    private String memberEmail;
    
    @Size(max = 15, message = "연락처는 15자 이하여야 합니다.")
    private String memberPhone;
}
