package com.kh.magamGG.domain.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PROJECT")
@Getter
@NoArgsConstructor
public class Project {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "PROJECT_NO")
	private Long projectNo;
	
	@Column(name = "PROJECT_NAME", nullable = false, length = 100)
	private String projectName;
	
	@Column(name = "PROJECT_STATUS", nullable = false, columnDefinition = "VARCHAR(100) DEFAULT '연재'")
	private String projectStatus;
	
	@Column(name = "PROJECT_COLOR", nullable = false, length = 50)
	private String projectColor;

    @Column(name = "PROJECT_CYCLE")
    private Integer projectCycle;

    @Column(name = "THUMBNAIL_FILE", length = 500)
    private String thumbnailFile;

    @Column(name = "PROJECT_STARTED_AT")
    private LocalDateTime projectStartedAt;
	
	@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ProjectMember> projectMembers = new ArrayList<>();
	
	@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<KanbanBoard> kanbanBoards = new ArrayList<>();
}
