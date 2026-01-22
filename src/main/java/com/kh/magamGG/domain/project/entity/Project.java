package com.kh.magamGG.domain.project.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PROJECT")
public class Project {
	
	@Id
	@Column(name = "PROJECT_NO")
	private Long projectNo;
	
	@Column(name = "PROJECT_NAME", nullable = false, length = 100)
	private String projectName;
	
	@Column(name = "PROJECT_STATUS", nullable = false, length = 100)
	private String projectStatus;
	
	@Column(name = "PROJECT_COLOR", nullable = false, length = 50)
	private String projectColor;
	
	@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProjectMember> projectMembers = new ArrayList<>();
	
	@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<KanbanBoard> kanbanBoards = new ArrayList<>();
}
