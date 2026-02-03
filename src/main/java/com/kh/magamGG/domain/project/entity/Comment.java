package com.kh.magamGG.domain.project.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor
public class Comment {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "COMMENT_NO")
	private Long commentNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_NO", nullable = false)
    private Member member;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "KANBAN_CARD_NO", nullable = false)
	private KanbanCard kanbanCard;
	
	@Column(name = "COMMENT_CONTENT", length = 1000)
	private String commentContent;
	
	@Column(name = "COMMENT_STATUS", length = 10, nullable = false, columnDefinition = "VARCHAR(10) DEFAULT 'ACTIVE'")
	private String commentStatus;
	
	@Column(name = "COMMENT_CREATED_AT", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime commentCreatedAt;

	// Setter methods
	public void setMember(Member member) {
		this.member = member;
	}

	public void setKanbanCard(KanbanCard kanbanCard) {
		this.kanbanCard = kanbanCard;
	}

	public void setCommentContent(String commentContent) {
		this.commentContent = commentContent;
	}

	public void setCommentStatus(String commentStatus) {
		this.commentStatus = commentStatus;
	}

	public void setCommentCreatedAt(LocalDateTime commentCreatedAt) {
		this.commentCreatedAt = commentCreatedAt;
	}
}
