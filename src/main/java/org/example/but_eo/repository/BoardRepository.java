package org.example.but_eo.repository;

import org.example.but_eo.entity.Board;
import org.example.but_eo.entity.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, String> {

    Page<Board> findByEventAndCategoryAndState(Board.Event event, Board.Category category, Board.State state, Pageable pageable);

    List<Board> findTop5ByStateOrderByCreatedAtDesc(Board.State state);

    Optional<Board> findById(String boardId);

    Page<Board> findByUser_UserHashIdAndState(String userHashId, Board.State state, Pageable pageable);

    void deleteAllByUser(Users user);

    Page<Board> findByTitleContainingAndUser_NameContaining(String title, String name, Pageable pageable);




}
