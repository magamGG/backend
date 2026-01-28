package com.kh.magamGG.domain.member.service;

import com.kh.magamGG.domain.member.dto.request.MemberRequest;
import com.kh.magamGG.domain.member.dto.response.MemberResponse;

import java.util.List;

public interface MemberService {
    MemberResponse register(MemberRequest request);
    List<MemberResponse> getMembersByAgencyNo(Long agencyNo);
}
