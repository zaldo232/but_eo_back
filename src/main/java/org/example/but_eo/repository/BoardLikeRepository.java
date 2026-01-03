package org.example.but_eo.repository;

import org.example.but_eo.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardLikeRepository extends JpaRepository<BoardLike, BoardLikeKey> {
    boolean existsByUserAndBoard(Users user, Board board);
    void deleteByUserAndBoard(Users user, Board board);
    boolean existsByUser_UserHashIdAndBoard_BoardId(String userHashId, String boardId);

    void deleteAllByBoard_BoardId(String boardId);
}
