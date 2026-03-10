package com.kh.magamGG.domain.project.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PROJECT_MEMBER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
	
	@Column(name = "PROJECT_MEMBER_ROLE", nullable = false, columnDefinition = "VARCHAR(50) DEFAULT '담당자'")
	private String projectMemberRole;

	@Column(name = "PROJECT_MEMBER_CREATED_AT", nullable = false, updatable = false)
	private LocalDateTime projectMemberCreatedAt;

	@Column(name = "PROJECT_MEMBER_ENDED_AT")
	private LocalDateTime projectMemberEndedAt;
	
	@OneToMany(mappedBy = "projectMember", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<KanbanCard> kanbanCards = new ArrayList<>();

	@PrePersist
	protected void onCreate() {
		if (projectMemberCreatedAt == null) {
			projectMemberCreatedAt = LocalDateTime.now();
		}
	}

	public void setMember(Member member) { this.member = member; }
	public void setProject(Project project) { this.project = project; }
	public void setProjectMemberRole(String projectMemberRole) { this.projectMemberRole = projectMemberRole; }
}
