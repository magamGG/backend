package com.kh.magamGG.domain.agency.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "AGENCY")
public class Agency {
	
	@Id
	@Column(name = "AGENCY_NO")
	private Long agencyNo;
	
	@Column(name = "AGENCY_NAME", length = 30)
	private String agencyName;
	
	@Column(name = "AGENCY_CODE", length = 6)
	private String agencyCode;
	
	@OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Member> members = new ArrayList<>();
}
