package com.kh.magamGG.domain.agency.entity;

import com.kh.magamGG.domain.member.entity.Member;
import com.kh.magamGG.domain.member.entity.NewRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "AGENCY")
@Getter
@NoArgsConstructor
public class Agency {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "AGENCY_NO")
	private Long agencyNo;
	
	@Column(name = "AGENCY_NAME", length = 30)
	private String agencyName;
	
	@Column(name = "AGENCY_CODE", length = 6)
	private String agencyCode;

    @Column(name = "AGENCY_LEAVE", columnDefinition = "INT DEFAULT 15")
    private Integer agencyLeave;
	
	@OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Member> members = new ArrayList<>();

    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NewRequest> newRequests = new ArrayList<>();
}
