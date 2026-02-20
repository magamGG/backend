package com.kh.magamGG.domain.ai.controller;

import com.kh.magamGG.domain.ai.context.ToneContext;
import com.kh.magamGG.domain.ai.service.MagamjigiAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final MagamjigiAiService magamjigiAiService;

    @GetMapping("/artist/health-feedback")
    public ResponseEntity<String> getArtistHealthFeedback(Authentication authentication, @RequestParam(required = false) String tone) {
        Long memberNo = extractMemberNo(authentication);
        ToneContext.set(tone);
        try {
            return ResponseEntity.ok(magamjigiAiService.getArtistHealthFeedback(memberNo));
        } finally {
            ToneContext.clear();
        }
    }

    @GetMapping("/manager/artist-health-summary")
    public ResponseEntity<String> getManagerArtistHealthSummary(Authentication authentication, @RequestParam(required = false) String tone) {
        Long memberNo = extractMemberNo(authentication);
        ToneContext.set(tone);
        try {
            return ResponseEntity.ok(magamjigiAiService.getManagerArtistHealthSummary(memberNo));
        } finally {
            ToneContext.clear();
        }
    }

    @GetMapping("/agency/health-overview")
    public ResponseEntity<String> getAgencyHealthOverview(Authentication authentication, @RequestParam(required = false) String tone) {
        Long memberNo = extractMemberNo(authentication);
        ToneContext.set(tone);
        try {
            return ResponseEntity.ok(magamjigiAiService.getAgencyHealthOverview(memberNo));
        } finally {
            ToneContext.clear();
        }
    }

    @GetMapping("/agency/risk-summary")
    public ResponseEntity<String> getAgencyRiskSummary(Authentication authentication, @RequestParam(required = false) String tone) {
        Long memberNo = extractMemberNo(authentication);
        ToneContext.set(tone);
        try {
            return ResponseEntity.ok(magamjigiAiService.getAgencyRiskSummary(memberNo));
        } finally {
            ToneContext.clear();
        }
    }

    @GetMapping("/agency/leave-overlap-alert")
    public ResponseEntity<String> getAgencyLeaveOverlapAlert(Authentication authentication, @RequestParam(required = false) String tone) {
        Long memberNo = extractMemberNo(authentication);
        ToneContext.set(tone);
        try {
            return ResponseEntity.ok(magamjigiAiService.getAgencyLeaveOverlapAlert(memberNo));
        } finally {
            ToneContext.clear();
        }
    }

    @GetMapping("/agency/artist-assignment-balance")
    public ResponseEntity<String> getAgencyArtistAssignmentBalance(Authentication authentication, @RequestParam(required = false) String tone) {
        Long memberNo = extractMemberNo(authentication);
        ToneContext.set(tone);
        try {
            return ResponseEntity.ok(magamjigiAiService.getAgencyArtistAssignmentBalance(memberNo));
        } finally {
            ToneContext.clear();
        }
    }

    @GetMapping("/agency/rejected-then-reapplied-alert")
    public ResponseEntity<String> getAgencyRejectedThenReappliedAlert(Authentication authentication, @RequestParam(required = false) String tone) {
        Long memberNo = extractMemberNo(authentication);
        ToneContext.set(tone);
        try {
            return ResponseEntity.ok(magamjigiAiService.getAgencyRejectedThenReappliedAlert(memberNo));
        } finally {
            ToneContext.clear();
        }
    }

    @GetMapping("/artist/workload-summary")
    public ResponseEntity<String> getArtistWorkloadSummary(Authentication authentication, @RequestParam(required = false) String tone) {
        Long memberNo = extractMemberNo(authentication);
        ToneContext.set(tone);
        try {
            return ResponseEntity.ok(magamjigiAiService.getArtistWorkloadSummary(memberNo));
        } finally {
            ToneContext.clear();
        }
    }

    @GetMapping("/artist/project-priority-advice")
    public ResponseEntity<String> getArtistProjectPriorityAdvice(Authentication authentication, @RequestParam(required = false) String tone) {
        Long memberNo = extractMemberNo(authentication);
        ToneContext.set(tone);
        try {
            return ResponseEntity.ok(magamjigiAiService.getArtistProjectPriorityAdvice(memberNo));
        } finally {
            ToneContext.clear();
        }
    }

    @GetMapping("/artist/workation-recommendation")
    public ResponseEntity<String> getArtistWorkationRecommendation(Authentication authentication, @RequestParam(required = false) String tone) {
        Long memberNo = extractMemberNo(authentication);
        ToneContext.set(tone);
        try {
            return ResponseEntity.ok(magamjigiAiService.getArtistWorkationRecommendation(memberNo));
        } finally {
            ToneContext.clear();
        }
    }

    @GetMapping("/artist/leave-recommendation")
    public ResponseEntity<String> getArtistLeaveRecommendation(Authentication authentication, @RequestParam(required = false) String tone) {
        Long memberNo = extractMemberNo(authentication);
        ToneContext.set(tone);
        try {
            return ResponseEntity.ok(magamjigiAiService.getArtistLeaveRecommendation(memberNo));
        } finally {
            ToneContext.clear();
        }
    }

    @GetMapping("/manager/leave-recommendation")
    public ResponseEntity<String> getManagerLeaveRecommendation(Authentication authentication, @RequestParam(required = false) String tone) {
        Long memberNo = extractMemberNo(authentication);
        ToneContext.set(tone);
        try {
            return ResponseEntity.ok(magamjigiAiService.getManagerLeaveRecommendation(memberNo));
        } finally {
            ToneContext.clear();
        }
    }

    @GetMapping("/manager/artist-workload-balance")
    public ResponseEntity<String> getManagerArtistWorkloadBalance(Authentication authentication, @RequestParam(required = false) String tone) {
        Long memberNo = extractMemberNo(authentication);
        ToneContext.set(tone);
        try {
            return ResponseEntity.ok(magamjigiAiService.getManagerArtistWorkloadBalance(memberNo));
        } finally {
            ToneContext.clear();
        }
    }

    @GetMapping("/manager/my-health-feedback")
    public ResponseEntity<String> getManagerMyHealthFeedback(Authentication authentication, @RequestParam(required = false) String tone) {
        Long memberNo = extractMemberNo(authentication);
        ToneContext.set(tone);
        try {
            return ResponseEntity.ok(magamjigiAiService.getManagerMyHealthFeedback(memberNo));
        } finally {
            ToneContext.clear();
        }
    }

    @GetMapping("/manager/workation-recommendation")
    public ResponseEntity<String> getManagerWorkationRecommendation(Authentication authentication, @RequestParam(required = false) String tone) {
        Long memberNo = extractMemberNo(authentication);
        ToneContext.set(tone);
        try {
            return ResponseEntity.ok(magamjigiAiService.getManagerWorkationRecommendation(memberNo));
        } finally {
            ToneContext.clear();
        }
    }

    @GetMapping("/manager/nudge-message-recommendation")
    public ResponseEntity<String> getManagerNudgeMessageRecommendation(Authentication authentication, @RequestParam(required = false) String tone) {
        Long memberNo = extractMemberNo(authentication);
        ToneContext.set(tone);
        try {
            return ResponseEntity.ok(magamjigiAiService.getManagerNudgeMessageRecommendation(memberNo));
        } finally {
            ToneContext.clear();
        }
    }

    @GetMapping("/manager/artist-daily-health-summary")
    public ResponseEntity<String> getManagerArtistDailyHealthSummary(Authentication authentication, @RequestParam(required = false) String tone) {
        Long memberNo = extractMemberNo(authentication);
        ToneContext.set(tone);
        try {
            return ResponseEntity.ok(magamjigiAiService.getManagerArtistDailyHealthSummary(memberNo));
        } finally {
            ToneContext.clear();
        }
    }

    private Long extractMemberNo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("인증이 필요합니다.");
        }
        return (Long) authentication.getPrincipal();
    }
}
