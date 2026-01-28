package com.kh.magamGG.domain.manager.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ARTIST_ASSIGNMENT")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ARTIST_ASSIGNMENT_NO")
    private Long artistAssignmentNo;

    // 배정된 작가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARTIST_MEMBER_NO", nullable = false)
    private Member artist;

    // 담당자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MANAGER_NO", nullable = false)
    private Manager manager;
}

