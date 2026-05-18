package com.bsuir.exhibition.repository;

import com.bsuir.exhibition.entity.PaintingComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
public interface PaintingCommentRepository extends JpaRepository<PaintingComment, String> {

    @Query("""
            SELECT c FROM PaintingComment c
            JOIN FETCH c.user
            JOIN FETCH c.painting
            WHERE c.painting.id = :paintingId
            ORDER BY c.createdAt ASC
            """)
    List<PaintingComment> findByPaintingIdWithUserOrderByCreatedAtAsc(@Param("paintingId") String paintingId);

    @Query("""
            SELECT c FROM PaintingComment c
            JOIN FETCH c.user
            JOIN FETCH c.painting
            WHERE c.user.email = :email
            ORDER BY c.createdAt DESC
            """)
    List<PaintingComment> findByUserEmailWithPaintingOrderByCreatedAtDesc(@Param("email") String email);
}
