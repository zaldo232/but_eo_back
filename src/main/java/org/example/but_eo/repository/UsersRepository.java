package org.example.but_eo.repository;

import org.example.but_eo.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsersRepository extends JpaRepository<Users, String> {

    // 이메일 중복 확인용
    boolean existsByEmail(String email);

    // 전화번호 중복 확인이 필요하다면 추가
    boolean existsByTel(String tel);

    // 이메일로 사용자 조회 (로그인이나 인증 등에서 활용 가능)
    Users findByEmail(String email);

    Users findByUserHashId(String userHashId);
}
