package com.kh.magamGG.domain.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ARTIST_ASSIGNMENT: 담당자(MANAGER_NO)와 작가(MEMBER_NO) 중계 테이블.
 * 담당자(MANAGER 테이블)의 MANAGER_NO를 기준으로, ARTIST_ASSIGNMENT에 해당 MANAGER_NO와
 * 작가의 MEMBER_NO(ARTIST_MEMBER_NO 컬럼)가 저장되어 있는지 여부로 '배정된 작가'를 판단한다.
 */
@Entity
@Table(name = "ARTIST_ASSIGNMENT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ARTIST_ASSIGNMENT_NO")
    private Long artistAssignmentNo;

    /** 작가 회원 번호 (MEMBER.MEMBER_NO) — DB 컬럼: ARTIST_MEMBER_NO */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARTIST_MEMBER_NO", nullable = false)
    private Member artist;

    /** 담당자 번호 (MANAGER.MANAGER_NO) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MANAGER_NO", nullable = false)
    private Manager manager;
}
