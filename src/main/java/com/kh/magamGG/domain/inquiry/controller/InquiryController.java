package com.kh.magamGG.domain.inquiry.controller;

import com.kh.magamGG.domain.inquiry.dto.request.InquiryRequest;
import com.kh.magamGG.domain.inquiry.dto.response.InquiryResponse;
import com.kh.magamGG.domain.inquiry.entity.Inquiry;
import com.kh.magamGG.domain.inquiry.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;
    
    @PostMapping
    public ResponseEntity<InquiryResponse> createInquiry(
        @RequestHeader("X-Member-No") Long memberNo,
        @RequestPart("data") @Valid InquiryRequest request,
        @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        Inquiry inquiry = inquiryService.createInquiry(memberNo, request, files);
        InquiryResponse response = InquiryResponse.from(inquiry);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

