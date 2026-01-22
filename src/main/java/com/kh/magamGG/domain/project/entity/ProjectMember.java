package com.kh.magamGG.domain.project.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PROJECT_MEMBER")
public class ProjectMember {
	
	@Id
	@Column(name = "PROJECT_MEMBER_NO")
	private Long projectMemberNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MEMBER_NO", nullable = false)
	private Member member;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PROJECT_NO", nullable = false)
	private Project project;
	
	@Column(name = "PROJECT_MEMBER_ROLE", nullable = false, length = 50)
	private String projectMemberRole;
	
	@OneToMany(mappedBy = "projectMember", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<KanbanCard> kanbanCards = new ArrayList<>();
}
