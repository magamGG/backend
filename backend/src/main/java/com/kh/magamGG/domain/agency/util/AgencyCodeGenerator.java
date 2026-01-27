package com.kh.magamGG.domain.agency.util;

import com.kh.magamGG.domain.agency.repository.AgencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@RequiredArgsConstructor
public class AgencyCodeGenerator {
    
    private final AgencyRepository agencyRepository;
    private static final int CODE_LENGTH = 11;
    private static final Random random = new Random();
    
    /**
     * 11자리 랜덤 숫자 에이전시 코드 생성
     * 중복 체크를 통해 유일한 코드를 반환
     */
    public String generateUniqueAgencyCode() {
        String code;
        int maxAttempts = 100; // 최대 100번 시도
        
        do {
            code = generateRandomCode();
            maxAttempts--;
            
            if (maxAttempts <= 0) {
                throw new RuntimeException("에이전시 코드 생성에 실패했습니다. 다시 시도해주세요.");
            }
        } while (agencyRepository.existsByAgencyCode(code));
        
        return code;
    }
    
    /**
     * 11자리 랜덤 숫자 코드 생성
     */
    private String generateRandomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        
        // 첫 번째 자리는 1-9 (0으로 시작하지 않도록)
        code.append(random.nextInt(9) + 1);
        
        // 나머지 10자리는 0-9
        for (int i = 1; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        
        return code.toString();
    }
}
