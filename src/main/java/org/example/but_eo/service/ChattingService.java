package org.example.but_eo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.but_eo.dto.ChatMember;
import org.example.but_eo.dto.ChattingDTO;
import org.example.but_eo.entity.*;
import org.example.but_eo.repository.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChattingService {

    private final ChattingRepository chattingRepository;
    private final UsersRepository usersRepository;
    private final ChattingMemberRepository chattingMemberRepository;
    private final ChattingMessageRepository chattingMessageRepository;
    private final RedisChatService redisChatService;

    //채팅방 생성
    public Chatting createChatRoom(List<String> userIds, String chatRoomName) {
        Chatting chatRoom = new Chatting();
        chatRoom.setChatId(UUID.randomUUID().toString());
        chatRoom.setTitle(chatRoomName);
        chatRoom.setCreatedAt(LocalDateTime.now());
        chatRoom.setState(Chatting.State.PUBLIC);
        chattingRepository.save(chatRoom);
        chattingMemberRepository.flush();
        for (String userId : userIds) {
            Users user = usersRepository.findByUserHashId(userId);
            ChattingMember chattingMember = new ChattingMember();
            chattingMember.setChatting(chatRoom);
            chattingMember.setUser(user);
            chattingMember.setReadCheck(false);
            chattingMember.setChattingMemberKey(new ChattingMemberKey(user.getUserHashId(), chatRoom.getChatId()));
            chattingMemberRepository.save(chattingMember);
        }

        System.out.println("채팅방 생성됨: [ + 채팅방 아이디 : " +  chatRoom.getChatId() + ", 채팅방 이름 : " + chatRoomName + "], 유저 IDs: " + userIds);

        return chatRoom;
    }

    public List<ChattingDTO> searchChatRooms(String userId) {
        List<ChattingMember> rooms = chattingMemberRepository.findByUserHashId(userId);
        System.out.println(rooms);
        List<ChattingDTO> ChattingDtoList = new ArrayList<>();

        for (ChattingMember room : rooms) {
            ChattingDTO chattingDTO = new ChattingDTO();
            chattingDTO.setRoomId(room.getChatting().getChatId());
            chattingDTO.setRoomName(room.getChatting().getTitle());

            List<String> message = redisChatService.getLastMessages(room.getChatting().getChatId());
            if (message != null) {
                chattingDTO.setLastMessage(message.get(0));
                chattingDTO.setLastMessageTime(LastMessageTimeFormat(LocalDateTime.parse(message.get(1))));
            } else {
                Optional<ChattingMessage> lastMsgOpt = chattingMessageRepository.findLastMessageByChatIdNative(room.getChatting().getChatId());
                if (lastMsgOpt.isEmpty()) {
                    chattingDTO.setLastMessage(null);
                    chattingDTO.setLastMessageTime(null);
                } else {
                    ChattingMessage lastMsg = lastMsgOpt.get();
                    chattingDTO.setLastMessage(lastMsg.getMessage());
                    chattingDTO.setLastMessageTime(LastMessageTimeFormat(lastMsg.getCreatedAt()));
                }
            }

            ChattingDtoList.add(chattingDTO);
        }

        return ChattingDtoList;
    }

    //전체 채팅방 조회
//    public List<ChattingMember> allChatRooms() {
//        List<ChattingMember> rooms = chattingMemberRepository.findAll();
//        System.out.println(rooms);
//        return rooms;
//    }
    public List<ChattingDTO> allChatRooms() {
        Set<ChattingDTO> uniqueChatRoomsSet = chattingMemberRepository.findAll().stream()
                .map(ChattingMember::getChatting) // ChattingMember에서 Chatting 엔티티 추출
                .filter(chatting -> chatting != null) // null 값 방지
                .map(chatting -> new ChattingDTO( // ChattingDTO로 변환
                        chatting.getChatId(),
                        chatting.getTitle(),
                        null,
                        null
                ))
                .collect(Collectors.toSet()); // Set으로 수집하여 중복 제거

        List<ChattingDTO> uniqueChatRoomsList = new ArrayList<>(uniqueChatRoomsSet);

        System.out.println("Unique Chat Rooms Count (from service): " + uniqueChatRoomsList.size()); // 로그 변경
        return uniqueChatRoomsList;
    }


    private String LastMessageTimeFormat(LocalDateTime lastMessageTime) {
        if (lastMessageTime == null) return null;

        Duration duration = Duration.between(lastMessageTime, LocalDateTime.now());

        if (duration.toHours() >= 48) {
            return lastMessageTime.format(DateTimeFormatter.ofPattern("MM.dd"));
        } else if (duration.toHours() >= 24) {
            return "어제";
        } else {
            int hour = lastMessageTime.getHour();
            String minute = String.format("%02d", lastMessageTime.getMinute());
            if (hour >= 12) {
                return "오후 " + (hour % 12 == 0 ? 12 : hour % 12) + ":" + minute;
            } else  {
                return "오전 " + hour + ":" + minute;
            }
        }
    }

    public void exitChatRoom(String userId, String chatRoomId) {
        chattingMemberRepository.deleteChattingMember(userId, chatRoomId);
        log.warn("쿼리문 : DELETE FROM chatting_member WHERE user_hash_id = '" + userId + "' AND chat_id = '" + chatRoomId + "'");
    }

    public List<ChatMember> getChatMembers(String roomId) {
        List<ChatMember> memberList = new ArrayList<>();
        List<ChattingMember> membersData = chattingMemberRepository.findByChatMemberList(roomId);
        for (ChattingMember memberData : membersData) {
            ChatMember member = new ChatMember();
            member.setNickName(memberData.getUser().getName());
            member.setProfile(memberData.getUser().getProfile());
            memberList.add(member);
        }
        return memberList;
    }

    public String getNickName(String userId) {
        Users user = usersRepository.findByUserHashId(userId);
        return user.getName();
    }
}
