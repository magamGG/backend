package com.kh.magamGG.domain.member.controller;

import com.kh.magamGG.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class MemberController {
    
    private final MemberService memberService;
    
    /**
     * 회원가입 테스트 및 연관관계 확인 엔드포인트
     * GET http://localhost:8888/api/test/member-registration
     */
    @GetMapping("/member-registration")
    public ResponseEntity<String> testMemberRegistration() {
        try {
            memberService.testMemberRegistration();
            return ResponseEntity.ok("회원가입 테스트 완료! 콘솔 로그를 확인하세요.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("테스트 실패: " + e.getMessage());
        }
    }
}
