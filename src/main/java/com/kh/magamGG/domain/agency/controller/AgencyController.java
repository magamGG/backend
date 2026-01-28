package com.kh.magamGG.domain.agency.controller;

import com.kh.magamGG.domain.agency.dto.request.JoinRequestRequest;
import com.kh.magamGG.domain.agency.dto.request.RejectJoinRequestRequest;
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
    
    @PostMapping("/join-requests/{newRequestNo}/approve")
    public ResponseEntity<JoinRequestResponse> approveJoinRequest(@PathVariable Long newRequestNo) {
        JoinRequestResponse response = agencyService.approveJoinRequest(newRequestNo);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/join-requests/{newRequestNo}/reject")
    public ResponseEntity<JoinRequestResponse> rejectJoinRequest(
            @PathVariable Long newRequestNo,
            @RequestBody(required = false) RejectJoinRequestRequest request) {
        String rejectionReason = request != null ? request.getRejectionReason() : null;
        JoinRequestResponse response = agencyService.rejectJoinRequest(newRequestNo, rejectionReason);
        return ResponseEntity.ok(response);
    }
}
