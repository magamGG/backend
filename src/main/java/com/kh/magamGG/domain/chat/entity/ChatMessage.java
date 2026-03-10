package com.kh.magamGG.domain.chat.entity;

import com.kh.magamGG.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_no")
    private Long chatNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_no", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_no", nullable = false)
    private ChatRoom chatRoom;

    @Column(name = "chat_status", nullable = false, length = 1)
    private String chatStatus; // Y / N (보임/삭제)

    @Column(name = "chat_message", nullable = false, length = 500)
    private String chatMessage;

    @Column(
        name = "chat_message_created_at",
        nullable = false,
        columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP"
    )
    private LocalDateTime chatMessageCreatedAt;

    @Column(
        name = "chat_message_updated_at",
        columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
    )
    private LocalDateTime chatMessageUpdatedAt;

    @Column(name = "chat_message_type", nullable = false, length = 10)
    private String chatMessageType; // message / image / file

    @Column(name = "attachment_url", length = 1000)
    private String attachmentUrl;
}


