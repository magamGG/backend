package com.kh.magamGG.domain.project.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project_member")
@Getter
@NoArgsConstructor
public class ProjectMember {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "PROJECT_MEMBER_NO")
	private Long projectMemberNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MEMBER_NO", nullable = false)
	private Member member;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PROJECT_NO", nullable = false)
	private Project project;
	
	@Column(name = "PROJECT_MEMBER_ROLE", nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'MEMBER'")
	private String projectMemberRole;
	
	@OneToMany(mappedBy = "projectMember", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<KanbanCard> kanbanCards = new ArrayList<>();

	public void setMember(Member member) { this.member = member; }
	public void setProject(Project project) { this.project = project; }
	public void setProjectMemberRole(String projectMemberRole) { this.projectMemberRole = projectMemberRole; }
}
