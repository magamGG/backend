package com.kh.magamGG.domain.agency.entity;

import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.entity.NewRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "AGENCY")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agency {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AGENCY_NO")
	private Long agencyNo;
	
	@Column(name = "AGENCY_NAME", length = 30, unique = true)
	private String agencyName;
	
    // 에이전시 이름 수정을 위한 메서드
	public void updateAgencyName(String agencyName) {
		this.agencyName = agencyName;
	}

	/** 기본 연차(agencyLeave) 수정 */
	public void updateAgencyLeave(Integer agencyLeave) {
		this.agencyLeave = agencyLeave;
	}

	@Column(name = "AGENCY_CODE", length = 11, unique = true)
	private String agencyCode;

    @Column(name = "AGENCY_LEAVE", columnDefinition = "INT DEFAULT 15")
    private Integer agencyLeave;

    @Builder.Default
	@OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Member> members = new ArrayList<>();

    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NewRequest> newRequests = new ArrayList<>();
}
