package com.kh.magamGG.domain.member.entity;

import com.kh.magamGG.domain.agency.entity.Agency;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "NEW_REQUEST")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@org.hibernate.annotations.DynamicUpdate
public class NewRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NEW_REQUEST_NO")
    private Long newRequestNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AGENCY_NO", nullable = false)
    private Agency agency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_NO", nullable = false)
    private Member member;

    @Column(name = "NEW_REQUEST_DATE")
    private LocalDateTime newRequestDate;

    @Column(name = "NEW_REQUEST_STATUS", nullable = false, length = 10)
    private String newRequestStatus;

    // 상태 업데이트 메서드
    public void setNewRequestStatus(String newRequestStatus) {
        this.newRequestStatus = newRequestStatus;
    }
}
