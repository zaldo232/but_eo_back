package org.example.but_eo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.example.but_eo.dto.*;
import org.example.but_eo.entity.TeamMember;
import org.example.but_eo.entity.Users;
import org.example.but_eo.repository.*;
import org.example.but_eo.util.JwtUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UsersService {

    private final UsersRepository usersRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final ChattingMemberRepository chattingMemberRepository;
    private final NotificationRepository notificationRepository;
    private final FileRepository fileRepository;
    private final TeamInvitationRepository teamInvitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 회원가입
    @Transactional
    public void registerUser(UserRegisterRequestDto dto) {
        if (usersRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        Users.Division division;
        try {
            division = Users.Division.valueOf(dto.getDivision().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("division 값은 USER 또는 BUSINESS만 가능합니다.");
        }

        if (division == Users.Division.ADMIN) {
            throw new IllegalArgumentException("ADMIN 권한으로는 회원가입이 불가능합니다.");
        }
        if (division == Users.Division.BUSINESS && (dto.getBusinessNumber() == null || dto.getBusinessNumber().isBlank())) {
            throw new IllegalArgumentException("사업자는 사업자등록번호를 반드시 입력해야 합니다.");
        }

        Users user = new Users();
        user.setUserHashId(generateUserHash(dto.getEmail()));
        user.setState(Users.State.ACTIVE);
        user.setDivision(division);
        user.setLoginType(Users.LoginType.BUTEO);
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setTel(dto.getTel());
        user.setGender(dto.getGender());
        user.setPreferSports(dto.getPreferSports());
        user.setBirth(dto.getBirthYear());
        user.setRegion(dto.getRegion());
        user.setCreatedAt(LocalDateTime.now());
        user.setEmailVerified(true);
        if (division == Users.Division.BUSINESS) {
            user.setBusinessNumber(dto.getBusinessNumber());
        }
        if (user.getLoginType() == Users.LoginType.BUTEO) {
            user.setProfile("/uploads/profiles/DefaultProfileImage.png");
        }

        usersRepository.save(user);

        System.out.println(user.getEmail() + " 로 회원가입 성공 (division=" + division + ")");
    }

    // 이메일 해시값 생성
    private String generateUserHash(String email) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(email.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    // 로그인
    public UserLoginResponseDto login(UserLoginRequestDto dto) {
        Users user = usersRepository.findByEmail(dto.getEmail());
        if (user == null) {
            throw new IllegalArgumentException("존재하지 않는 이메일입니다.");
        }
        if (user.getState() != Users.State.ACTIVE) {
            throw new IllegalStateException("현재 탈퇴 대기 중이거나 비활성화된 계정입니다.");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getUserHashId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserHashId());

        user.setRefreshToken(refreshToken);
        usersRepository.save(user);

        return new UserLoginResponseDto(accessToken, refreshToken, user.getName(), user.getDivision().name());
    }

    // 회원정보 수정 (프로필 이미지/기타 정보 포함, null-safe)
    @Transactional
    public void updateUser(String userId, UserUpdateRequestDto dto) {
        Users user = usersRepository.findByUserHashId(userId);
        if (user == null) throw new IllegalArgumentException("존재하지 않는 사용자입니다.");

        if (dto.getName() != null) user.setName(dto.getName());
        if (dto.getRegion() != null) user.setRegion(dto.getRegion());
        if (dto.getPreferSports() != null) user.setPreferSports(dto.getPreferSports());
        if (dto.getTel() != null) user.setTel(dto.getTel());
        if (dto.getGender() != null) user.setGender(dto.getGender());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        MultipartFile profileFile = dto.getProfile();
        if (profileFile != null && !profileFile.isEmpty()) {
            String newProfileUrl = saveProfileImage(profileFile);
            user.setProfile(newProfileUrl);
        }

        usersRepository.save(user);
    }

    // 관리자 유저 정보 수정
    @Transactional
    public void updateUserByAdmin(String userHashId, UserUpdateRequestDto dto) {
        Users user = usersRepository.findByUserHashId(userHashId);
        if (user == null) throw new IllegalArgumentException("존재하지 않는 사용자입니다.");

        if (dto.getName() != null) user.setName(dto.getName());
        if (dto.getRegion() != null) user.setRegion(dto.getRegion());
        if (dto.getPreferSports() != null) user.setPreferSports(dto.getPreferSports());
        if (dto.getTel() != null) user.setTel(dto.getTel());
        if (dto.getGender() != null) user.setGender(dto.getGender());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        MultipartFile profileFile = dto.getProfile();
        if (profileFile != null && !profileFile.isEmpty()) {
            String newProfileUrl = saveProfileImage(profileFile);
            user.setProfile(newProfileUrl);
        }

        usersRepository.save(user);
        log.info("관리자: 유저({}) 정보 수정 완료", userHashId);
    }

    // 프로필 이미지 저장
    private String saveProfileImage(MultipartFile file) {
        validateImageFile(file);
        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads/profiles/";
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir, fileName);

            Files.createDirectories(filePath.getParent());
            file.transferTo(filePath.toFile());

            return "/uploads/profiles/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("프로필 이미지 저장 실패", e);
        }
    }

    // 이미지 파일 검증
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return;
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|bmp)$")) {
            throw new IllegalArgumentException("지원하지 않는 이미지 확장자입니다.");
        }
    }

    // 회원 탈퇴 (soft delete)
    @Transactional
    public void deleteUser(String userId) {
        Users user = usersRepository.findByUserHashId(userId);
        if (user == null) throw new IllegalArgumentException("해당 유저를 찾을 수 없습니다.");

        if (user.getDivision() == Users.Division.ADMIN) {
            throw new IllegalStateException("관리자 계정은 이 경로로 탈퇴할 수 없습니다. 관리자용 삭제 기능을 사용하세요.");
        }

        List<TeamMember> memberList = teamMemberRepository.findAllByUser(user);
        for (TeamMember member : memberList) {
            if (member.getType() == TeamMember.Type.LEADER) {
                throw new IllegalStateException("팀 리더는 계정을 삭제할 수 없습니다. 리더 위임 후 진행해주세요.");
            }
        }
        deleteRelatedUserData(user);

        user.setState(Users.State.DELETED_WAIT);
        usersRepository.save(user);

        System.out.println(user.getEmail() + " 계정 탈퇴 처리됨.");
    }

    // 회원 영구 삭제 (hard delete)
    @Transactional
    public void deleteUserPermanently(String userId) {
        Users user = usersRepository.findByUserHashId(userId);
        if (user == null) throw new IllegalArgumentException("해당 유저가 존재하지 않습니다.");
        if (user.getState() != Users.State.DELETED_WAIT) {
            throw new IllegalStateException("삭제 대기 상태인 유저만 완전 삭제할 수 있습니다.");
        }
        if (user.getDivision() == Users.Division.ADMIN) {
            throw new IllegalStateException("관리자 계정은 이 경로로 영구 삭제할 수 없습니다. 관리자용 영구 삭제 기능을 사용하세요.");
        }
        deleteRelatedUserData(user);
        usersRepository.delete(user);
        System.out.println("유저 완전 삭제 완료: " + user.getEmail());
    }

    // 관리자 유저 계정 논리적 삭제
    @Transactional
    public void deleteUserByAdmin(String userHashId) {
        Users user = usersRepository.findByUserHashId(userHashId);
        if (user == null) throw new IllegalArgumentException("해당 유저를 찾을 수 없습니다.");

        if (user.getDivision() == Users.Division.ADMIN) {
            throw new IllegalStateException("관리자 계정은 이 경로로 논리적으로 삭제할 수 없습니다. (영구 삭제만 가능)");
        }

        List<TeamMember> memberList = teamMemberRepository.findAllByUser(user);
        for (TeamMember member : memberList) {
            if (member.getType() == TeamMember.Type.LEADER) {
                throw new IllegalStateException("해당 유저는 현재 팀 리더입니다. 리더 위임 후 삭제를 진행하거나 영구 삭제를 고려해주세요.");
            }
        }
        deleteRelatedUserData(user);

        user.setState(Users.State.DELETED_WAIT);
        usersRepository.save(user);
        log.info("관리자: 유저({}) 논리적 삭제 처리됨.", userHashId);
    }

    // 관리자 유저 계정 영구 삭제 (하드 삭제)
    @Transactional
    public void deleteUserPermanentlyByAdmin(String userHashId) {
        Users user = usersRepository.findByUserHashId(userHashId);
        if (user == null) throw new IllegalArgumentException("해당 유저가 존재하지 않습니다.");

        deleteRelatedUserData(user);

        usersRepository.delete(user);
        log.info("관리자: 유저({}) 완전 삭제 완료.", userHashId);
    }

    // 사용자와 연관된 모든 데이터를 삭제하는 헬퍼 메서드
    private void deleteRelatedUserData(Users user) {
        teamMemberRepository.deleteAllByUser(user);
        commentRepository.deleteAllByUser(user);
        boardRepository.deleteAllByUser(user);
        chattingMemberRepository.deleteAllByUser(user);
        notificationRepository.deleteAllByReceiverUser(user);
        notificationRepository.deleteAllBySenderUser(user);
        fileRepository.deleteAllByUserHashId(user);
        teamInvitationRepository.deleteAllByUser(user);
    }

    // 자기 정보 조회
    public UserInfoResponseDto getUserInfo(String userId) {
        Users user = usersRepository.findByUserHashId(userId);
        if (user == null) throw new IllegalArgumentException("사용자 없음");

        return new UserInfoResponseDto(
                user.getUserHashId(),
                user.getName(),
                user.getEmail(),
                user.getTel(),
                user.getRegion(),
                user.getDivision().toString(),
                user.getPreferSports(),
                user.getGender(),
                user.getProfile(),
                user.getBirth(),
                user.getBadmintonScore(),
                user.getTennisScore(),
                user.getTableTennisScore(),
                user.getBowlingScore(),
                user.getCreatedAt()
        );
    }

    // 전체 유저 조회
    public List<UserInfoResponseDto> getAllUsers() {
        return usersRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private UserInfoResponseDto convertToDto(Users user) {
        return new UserInfoResponseDto(
                user.getUserHashId(),
                user.getName(),
                user.getEmail(),
                user.getTel(),
                user.getRegion(),
                user.getDivision().toString(),
                user.getPreferSports(),
                user.getGender(),
                user.getProfile(),
                user.getBirth(),
                user.getBadmintonScore(),
                user.getTennisScore(),
                user.getTableTennisScore(),
                user.getBowlingScore(),
                user.getCreatedAt()
        );
    }

    // 로그아웃 처리
    @Transactional
    public void logout(String userId) {
        Users user = usersRepository.findByUserHashId(userId);
        if (user == null) throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");

        switch (user.getLoginType()) {
            case BUTEO:
            case KAKAO:
            case NAVER:
                user.setRefreshToken(null);
                break;
        }
        usersRepository.save(user);
    }

    // 관리자 페이지용 사용자 목록 조회 (페이징 및 필터링)
    public Page<UserInfoResponseDto> getUsersWithPagingAndFilter(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Users> usersPage;

        if (keyword == null || keyword.isBlank()) {
            usersPage = usersRepository.findAll(pageable);
        } else {
            usersPage = usersRepository.findByNameContainingIgnoreCase(keyword, pageable);
        }

        return usersPage.map(this::convertToDto);
    }
}