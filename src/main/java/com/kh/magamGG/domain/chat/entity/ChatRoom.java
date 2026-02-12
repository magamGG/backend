package com.kh.magamGG.domain.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.action.internal.OrphanRemovalAction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "CHAT_ROOM")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CHAT_ROOM_NO")
    private Long chatRoomNo;

    @Column(name = "CHAT_ROOM_NAME", nullable = false, length = 50)
    private String chatRoomName;

    @Column(name = "CHAT_ROOM_TYPE", length = 30)
    private String chatRoomType; // 전체 / 프로젝트 / 개인

    @Column(
        name = "CHAT_ROOM_CREATED_AT",
        nullable = false,
        columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP"
    )
    private LocalDateTime chatRoomCreatedAt;

    @Column(
        name = "CHAT_ROOM_UPDATED_AT",
        columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
    )
    private LocalDateTime chatRoomUpdatedAt;

    @Column(name = "CHAT_ROOM_STATUS", nullable = false, length = 1)
    private String chatRoomStatus; // Y / N

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatRoomMember> roomMembers = new ArrayList<>();

    @Column(name = "PROJECT_NO", nullable = true)
    private Long projectNo;

    @Column(name = "AGENCY_NO")
    private Long agencyNo;
}


