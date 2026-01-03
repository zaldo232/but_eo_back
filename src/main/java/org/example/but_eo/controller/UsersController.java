package org.example.but_eo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.but_eo.dto.*;
import org.example.but_eo.entity.Users;
import org.example.but_eo.service.MailService;
import org.example.but_eo.service.UsersService;
import org.example.but_eo.util.JwtUtil;
import org.example.but_eo.repository.UsersRepository;
import org.example.but_eo.util.VerificationStore;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Slf4j
public class UsersController {

    private final UsersService usersService;
    private final MailService mailService;
    private final JwtUtil jwtUtil;
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationStore verificationStore;

    @PostMapping("/check_email")
    public ResponseEntity<?> checkEmail(@RequestBody EmailRequestDto emailRequestDto) {
        boolean exists = usersRepository.existsByEmail(emailRequestDto.getEmail());
        Map<String, Boolean> response = new HashMap<>();
        response.put("exist", exists);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody UserRegisterRequestDto dto) {
        if (!verificationStore.isVerified(dto.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이메일 인증이 필요합니다.");
        }
        usersService.registerUser(dto);
        verificationStore.remove(dto.getEmail());
        return ResponseEntity.ok("회원가입 성공");
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDto> login(@RequestBody UserLoginRequestDto dto) {
        log.info("로그인 요청 : 이메일 = {}", dto.getEmail());
        UserLoginResponseDto response = usersService.login(dto);
        log.info("로그인 응답 : 유저 이메일 : {}, 계정 유형 : {}", response.getUserName(), response.getDivision());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/kakao/login")
    public ResponseEntity<Map<String, String>> kakaologin(@RequestBody KakaoLoginDto kakaoLoginDto) {
        try {
            String userHashId = UUID.randomUUID().toString();
            String userPassword = UUID.randomUUID().toString();
            Users existingUser = usersRepository.findByEmail(kakaoLoginDto.getEmail());

            if (existingUser == null) {
                Users newUser = new Users();
                newUser.setUserHashId(userHashId);
                newUser.setName(kakaoLoginDto.getNickName());
                newUser.setTel(kakaoLoginDto.getTel());
                newUser.setPassword(userPassword);
                newUser.setEmail(kakaoLoginDto.getEmail());
                newUser.setProfile(kakaoLoginDto.getProfileImage());
                newUser.setGender(kakaoLoginDto.getGender());
                newUser.setBirth(kakaoLoginDto.getBirthYear());
                newUser.setRefreshToken(kakaoLoginDto.getRefreshToken());
                newUser.setDivision(Users.Division.USER);
                newUser.setState(Users.State.ACTIVE);
                newUser.setLoginType(Users.LoginType.KAKAO);
                newUser.setEmailVerified(true);
                newUser.setCreatedAt(LocalDateTime.now());
                usersRepository.save(newUser);
            } else {
                existingUser.setName(kakaoLoginDto.getNickName());
                existingUser.setProfile(kakaoLoginDto.getProfileImage());
                existingUser.setRefreshToken(kakaoLoginDto.getRefreshToken());
                existingUser.setLoginType(Users.LoginType.KAKAO);
                usersRepository.save(existingUser);
            }

            Users savedUser = usersRepository.findByEmail(kakaoLoginDto.getEmail());
            String jwtToken = jwtUtil.generateAccessToken(savedUser.getUserHashId());

            Map<String, String> result = new HashMap<>();
            result.put("accessToken", jwtToken);

            String tel = savedUser.getTel();
            if (tel == null || tel.trim().isEmpty()) {
                result.put("dataStatus", "moreData");
            } else {
                result.put("dataStatus", "success");
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("카카오 로그인 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "로그인 실패"));
        }
    }

    @GetMapping("/my-info")
    public ResponseEntity<?> myInfo(Authentication authentication) {
        try {
            String userId = (String) authentication.getPrincipal();
            UserInfoResponseDto response = usersService.getUserInfo(userId);
            log.info("[my-info] 사용자 정보: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[my-info] 사용자 정보 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "사용자 정보 조회 실패", "detail", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        String token = refreshToken.replace("Bearer ", "");
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).body("Refresh Token이 유효하지 않습니다.");
        }
        String userId = jwtUtil.getUserIdFromToken(token);
        Users user = usersRepository.findByUserHashId(userId);
        if (user == null || !token.equals(user.getRefreshToken())) {
            return ResponseEntity.status(401).body("Refresh Token이 일치하지 않습니다.");
        }
        String newAccessToken = jwtUtil.generateAccessToken(userId);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    @PatchMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateUser(
            @ModelAttribute UserUpdateRequestDto request,
            Authentication authentication) {
        try {
            String userId = (String) authentication.getPrincipal();
            usersService.updateUser(userId, request);
            return ResponseEntity.ok("회원 정보 수정 완료");
        } catch (IllegalArgumentException e) {
            // 클라이언트 요청 오류
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // 서버 오류
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("회원정보 수정 중 오류: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteUser(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        usersService.deleteUser(userId);
        return ResponseEntity.ok("회원 탈퇴 완료");
    }

    @DeleteMapping("/permanent")
    public ResponseEntity<?> deleteUserPermanently(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        usersService.deleteUserPermanently(userId);
        return ResponseEntity.ok("계정이 완전히 삭제되었습니다.");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(Authentication authentication) {
        try {
            String userId = (String) authentication.getPrincipal();
            UserInfoResponseDto response = usersService.getUserInfo(userId);
            log.info("[/me] 사용자 정보: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[/me] 사용자 정보 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "사용자 정보 조회 실패", "detail", e.getMessage()));
        }
    }

    @GetMapping("/{userHashId}")
    public ResponseEntity<UserInfoResponseDto> getUserById(@PathVariable String userHashId) {
        UserInfoResponseDto userInfo = usersService.getUserInfo(userHashId);
        return ResponseEntity.ok(userInfo);
    }

    //닉네임으로 검색
    @GetMapping("/search")
    public ResponseEntity<List<UserSearchDto>> getUsersByName(@RequestParam String name, Authentication authentication) {
        List<Users> users = usersRepository.findByNameContainingAndUserHashIdNot(name, authentication.getPrincipal().toString());
        List<UserSearchDto> result = users.stream().map(user -> new UserSearchDto(
                user.getUserHashId(),
                user.getName(),
                user.getProfile()
        )).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/searchAll")
    public ResponseEntity<List<UserSearchDto>> searchAll() {
        List<Users> users = usersRepository.findAll();
        List<UserSearchDto> result = users.stream().map(user -> new UserSearchDto(
                user.getUserHashId(),
                user.getName(),
                user.getProfile()
        )).toList();
        return ResponseEntity.ok(result);
    }

    @RestController
    @RequestMapping("/oauth2")
    public static class OAuth2Controller {
        @GetMapping("/success")
        public String oauthLoginSuccess(@AuthenticationPrincipal OAuth2User oAuth2User) {
            return "소셜 로그인 성공! 유저 이름: " + oAuth2User.getAttribute("name");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        usersService.logout(userId);
        return ResponseEntity.ok("로그아웃 성공");
    }

    @PostMapping("/find-id")
    public ResponseEntity<?> findUserIdByTel(@RequestBody Map<String, String> request) {
        String tel = request.get("tel");
        if (tel == null || tel.isBlank()) {
            return ResponseEntity.badRequest().body("전화번호는 필수입니다.");
        }
        Users user = usersRepository.findByTel(tel);
        if (user == null) {
            return ResponseEntity.status(404).body("해당 전화번호로 가입된 계정이 없습니다.");
        }
        return ResponseEntity.ok(Map.of("email", user.getEmail()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPasswordDirect(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String tel = request.get("tel");
        String newPassword = request.get("newPassword");
        if (email == null || tel == null || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body("모든 항목은 필수입니다.");
        }
        Users user = usersRepository.findByEmail(email);
        if (user == null || !tel.equals(user.getTel())) {
            return ResponseEntity.status(404).body("일치하는 사용자 정보가 없습니다.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        usersRepository.save(user);
        return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
    }

    //이메일 인증용
    @PostMapping("/send-verification")
    public ResponseEntity<String> sendVerification(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = mailService.sendVerificationCode(email);
        verificationStore.save(email, code);
        return ResponseEntity.ok("인증 코드 발송 완료");
    }

    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");
        if (verificationStore.verify(email, code)) {
            return ResponseEntity.ok("인증 성공");
        } else {
            return ResponseEntity.badRequest().body("인증 실패");
        }
    }

    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUsersForAdmin(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<UserInfoResponseDto> usersPage = usersService.getUsersWithPagingAndFilter(keyword, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("users", usersPage.getContent());
        response.put("currentPage", usersPage.getNumber());
        response.put("totalItems", usersPage.getTotalElements());
        response.put("totalPages", usersPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    //관리자 유저 정보 변경
    @PatchMapping(value = "/admin/update/{userHashId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) //멀티 폼 데이터 처리
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateUserByAdmin(
            @PathVariable String userHashId,
            @ModelAttribute UserUpdateRequestDto request) {
        try {
            usersService.updateUserByAdmin(userHashId, request);
            return ResponseEntity.ok("관리자 : 유저(" + userHashId + ") 정보 수정 완료");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("관리자 : 유저 정보 수정 실패 : {}", userHashId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("관리자 : 유저 정보 수정 중 오류" + e.getMessage());
        }
    }

    //관리자 유저 계정 삭제
    @DeleteMapping("/admin/delete/{userHashId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> deleteUserByAdmin(@PathVariable String userHashId) {
        try {
            usersService.deleteUserByAdmin(userHashId);
            return ResponseEntity.ok("관리자: 유저(" + userHashId + ") 논리적 삭제 완료");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            // 팀 리더 계정 삭제 시도 등 비즈니스 로직 제약 위반
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            log.error("관리자: 유저 논리적 삭제 실패: {}", userHashId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("관리자: 유저 논리적 삭제 중 오류 발생: " + e.getMessage());
        }
    }

    // 관리자 유저 계정 영구 삭제 (하드 삭제)
    @DeleteMapping("/admin/permanent/{userHashId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deleteUserPermanentlyByAdmin(@PathVariable String userHashId) {
        try {
            usersService.deleteUserPermanentlyByAdmin(userHashId);
            return ResponseEntity.ok("관리자: 유저(" + userHashId + ") 계정이 완전히 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            // 삭제 대기 상태가 아닌 계정의 영구 삭제 시도 등 비즈니스 로직 제약 위반 (옵션)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            log.error("관리자: 유저 영구 삭제 실패: {}", userHashId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("관리자: 유저 영구 삭제 중 오류 발생: " + e.getMessage());
        }
    }
}