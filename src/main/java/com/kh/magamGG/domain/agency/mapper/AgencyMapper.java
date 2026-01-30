package com.kh.magamGG.domain.agency.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AgencyMapper {
    
    // NEW_REQUEST 상태 업데이트
    int updateNewRequestStatus(@Param("newRequestNo") Long newRequestNo, @Param("status") String status);
    
    // MEMBER의 AGENCY_NO 업데이트
    int updateMemberAgencyNo(@Param("memberNo") Long memberNo, @Param("agencyNo") Long agencyNo);
}
