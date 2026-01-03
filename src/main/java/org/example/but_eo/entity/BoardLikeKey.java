package org.example.but_eo.entity;

import java.io.Serializable;
import java.util.Objects;

public class BoardLikeKey implements Serializable {
    private String user;
    private String board;

    public BoardLikeKey() {}

    public BoardLikeKey(String user, String board) {
        this.user = user;
        this.board = board;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoardLikeKey)) return false;
        BoardLikeKey that = (BoardLikeKey) o;
        return Objects.equals(user, that.user) &&
                Objects.equals(board, that.board);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, board);
    }
}
