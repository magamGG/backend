package com.kh.magamGG.domain.member.service;

import com.kh.magamGG.domain.member.dto.request.MemberRequest;
import com.kh.magamGG.domain.member.dto.response.MemberResponse;

public interface MemberService {
    MemberResponse register(MemberRequest request);
}
