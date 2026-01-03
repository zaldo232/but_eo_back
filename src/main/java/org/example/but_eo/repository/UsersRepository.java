package org.example.but_eo.repository;

import org.example.but_eo.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<Users, String> {

    // 이메일 중복 확인용
    boolean existsByEmail(String email);

    // 전화번호 중복 확인이 필요하다면 추가
    boolean existsByTel(String tel);

    // 이메일로 사용자 조회 (로그인이나 인증 등에서 활용 가능)
    Users findByEmail(String email);

    Users findByUserHashId(String userHashId);

    List<Users> findAllByName(String name);

    List<Users> findByNameContains(String name);

    List<Users> findByNameContainingAndUserHashIdNot(String name, String userHash);

    List<Users> findByUserHashIdIn(List<String> userIds);

    Users findByTel(String tel);

    Page<Users> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Users> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String nameKeyword, String emailKeyword, Pageable pageable);
}
