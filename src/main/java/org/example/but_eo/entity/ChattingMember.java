package org.example.but_eo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ChattingMember {

    @EmbeddedId
    private ChattingMemberKey chattingMemberKey;

    @JsonIgnore
    @ManyToOne
    @MapsId("userHashId")
    @JoinColumn(name = "user_hash_id", nullable = false)
    private Users user;

    @ManyToOne
    @MapsId("chatId")
    @JoinColumn(name = "chat_id", nullable = false)
    private Chatting chatting;

    @Column(nullable = false)
    private boolean readCheck;
}
