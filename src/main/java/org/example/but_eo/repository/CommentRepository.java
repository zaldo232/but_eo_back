package org.example.but_eo.repository;

import org.example.but_eo.entity.Comment;
import org.example.but_eo.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {

    List<Comment> findByBoard_BoardIdAndStateOrderByCreatedAtDesc(String boardId, Comment.State state);

    void deleteAllByBoard_BoardId(String boardId);

    void deleteAllByUser(Users user);
}

