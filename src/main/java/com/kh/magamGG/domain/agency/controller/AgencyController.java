package com.kh.magamGG.domain.agency.controller;

import com.kh.magamGG.domain.agency.dto.request.JoinRequestRequest;
import com.kh.magamGG.domain.agency.dto.response.JoinRequestResponse;
import com.kh.magamGG.domain.agency.service.AgencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agency")
@RequiredArgsConstructor
public class AgencyController {
    
    private final AgencyService agencyService;
    
    @PostMapping("/join-request")
    public ResponseEntity<JoinRequestResponse> createJoinRequest(
            @RequestBody JoinRequestRequest request,
            @RequestHeader("X-Member-No") Long memberNo) {
        JoinRequestResponse response = agencyService.createJoinRequest(request, memberNo);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{agencyNo}/join-requests")
    public ResponseEntity<List<JoinRequestResponse>> getJoinRequests(@PathVariable Long agencyNo) {
        List<JoinRequestResponse> responses = agencyService.getJoinRequests(agencyNo);
        return ResponseEntity.ok(responses);
    }
}
