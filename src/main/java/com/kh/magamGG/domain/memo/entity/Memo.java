package com.kh.magamGG.domain.memo.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "MEMO")
@Getter
@NoArgsConstructor
public class Memo {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MEMO_NO")
	private Long memoNo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MEMBER_NO", nullable = false)
	private Member member;
	
	@Column(name = "MEMO_NAME", nullable = false, length = 30)
	private String memoName;
	
	@Column(name = "MEMO_TEXT", length = 255)
	private String memoText;
	
	@Column(name = "MEMO_CREATED_AT", nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
	private LocalDateTime memoCreatedAt;
	
	@Column(name = "MEMO_UPDATED_AT", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	private LocalDateTime memoUpdatedAt;
	
	@Column(name = "MEMO_TYPE", length = 12)
	private String memoType;
}
