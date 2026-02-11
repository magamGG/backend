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
@Table(name = "CHAT_MESSAGE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CHAT_NO")
    private Long chatNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_NO", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CHAT_ROOM_NO", nullable = false)
    private ChatRoom chatRoom;

    @Column(name = "CHAT_STATUS", nullable = false, length = 1)
    private String chatStatus; // Y / N (보임/삭제)

    @Column(name = "CHAT_MESSAGE", nullable = false, length = 500)
    private String chatMessage;

    @Column(
        name = "CHAT_MESSAGE_CREATED_AT",
        nullable = false,
        columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP"
    )
    private LocalDateTime chatMessageCreatedAt;

    @Column(
        name = "CHAT_MESSAGE_UPDATED_AT",
        columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
    )
    private LocalDateTime chatMessageUpdatedAt;

    @Column(name = "CHAT_MESSAGE_TYPE", nullable = false, length = 10)
    private String chatMessageType; // message / image / file

    @Column(name = "ATTACHMENT_URL", length = 1000)
    private String attachmentUrl;
}


