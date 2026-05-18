package com.bsuir.exhibition.service;

import com.bsuir.exhibition.dto.CommentResponse;
import com.bsuir.exhibition.dto.CreateCommentRequest;
import com.bsuir.exhibition.entity.Painting;
import com.bsuir.exhibition.entity.PaintingComment;
import com.bsuir.exhibition.entity.User;
import com.bsuir.exhibition.repository.PaintingCommentRepository;
import com.bsuir.exhibition.repository.PaintingRepository;
import com.bsuir.exhibition.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaintingCommentService {

    private final PaintingCommentRepository commentRepository;
    private final PaintingRepository paintingRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(String paintingId) {
        ensurePaintingExists(paintingId);
        return commentRepository.findByPaintingIdWithUserOrderByCreatedAtAsc(paintingId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CommentResponse addComment(String paintingId, String userEmail, CreateCommentRequest request) {
        Painting painting = paintingRepository.findById(paintingId)
                .orElseThrow(() -> new IllegalArgumentException("Картина не найдена"));
        User user = userRepository.findUserByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        PaintingComment comment = new PaintingComment();
        comment.setPainting(painting);
        comment.setUser(user);
        comment.setContent(request.content().trim());
        comment.setCreatedAt(Instant.now());

        PaintingComment saved = commentRepository.save(comment);
        commentRepository.flush();
        return commentRepository.findById(saved.getId())
                .map(this::toResponse)
                .orElseGet(() -> toResponse(saved));
    }

    private void ensurePaintingExists(String paintingId) {
        if (!paintingRepository.existsById(paintingId)) {
            throw new IllegalArgumentException("Картина не найдена");
        }
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getMyComments(String userEmail) {
        return commentRepository.findByUserEmailWithPaintingOrderByCreatedAtDesc(userEmail).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteComment(String commentId, String userEmail) {
        PaintingComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Комментарий не найден"));
        if (!comment.getUser().getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("Можно удалить только свой комментарий");
        }
        commentRepository.delete(comment);
    }

    private CommentResponse toResponse(PaintingComment comment) {
        Painting painting = comment.getPainting();
        return new CommentResponse(
                java.util.UUID.fromString(comment.getId()),
                comment.getUser().getUsername(),
                painting.getId(),
                painting.getTitle(),
                painting.getAuthor(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
