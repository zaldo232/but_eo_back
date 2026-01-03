package org.example.but_eo.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.example.but_eo.entity.Chatting;
import org.example.but_eo.entity.ChattingMember;
import org.example.but_eo.entity.ChattingMemberKey;
import org.example.but_eo.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ChattingMemberRepository extends JpaRepository<ChattingMember, ChattingMemberKey> {
    void deleteAllByUser(Users user);

    @Query(value = "select * from chatting_member where chat_id = :chatId", nativeQuery = true)
    List<ChattingMember> findByChatMemberList(@Param("chatId") String chatId);
//    List<ChattingMember> findAllByChatting(Chatting chatting);

    @Query(value = "SELECT * FROM chatting_member WHERE user_hash_id = :userHashId", nativeQuery = true)
    List<ChattingMember> findByUserHashId(@Param("userHashId") String userHashId);
//    List<ChattingMember> findAllByUser(Users user);

    @Modifying
    @Query(value = "DELETE FROM chatting_member WHERE user_hash_id = :userHashId AND chat_id = :chatId", nativeQuery = true)
    @Transactional
    void deleteChattingMember(@Param("userHashId") String userHashId, @Param("chatId") String chatId);

}
