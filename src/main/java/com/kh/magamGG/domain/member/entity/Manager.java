package com.kh.magamGG.domain.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "MANAGER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Manager {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MANAGER_NO")
    private Long managerNo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_NO", nullable = false)
    private Member member;
}
