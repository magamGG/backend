package com.kh.magamGG.domain.member.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "artist_assignment")
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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARTIST_MEMBER_NO", nullable = false)
    private Member artist;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MANAGER_NO", nullable = false)
    private Manager manager;
}
