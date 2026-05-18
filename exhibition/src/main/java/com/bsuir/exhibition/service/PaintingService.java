package com.bsuir.exhibition.service;

import com.bsuir.exhibition.entity.Painting;
import com.bsuir.exhibition.repository.PaintingRepository;
import com.bsuir.exhibition.spec.PaintingSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaintingService {

    private final PaintingRepository repository;

    public List<Painting> get() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "title"));
    }

    @Transactional(readOnly = true)
    public List<Painting> search(String q, String style, String author, String sortParam) {
        Specification<Painting> spec = PaintingSpecifications.build(q, style, author);
        return repository.findAll(spec, resolveSort(sortParam));
    }

    @Transactional(readOnly = true)
    public List<String> distinctStyles() {
        return repository.findDistinctStyles();
    }

    private Sort resolveSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "title");
        }
        return switch (sortParam.trim().toUpperCase()) {
            case "TITLE_DESC" -> Sort.by(Sort.Direction.DESC, "title");
            case "AUTHOR_ASC" -> Sort.by(Sort.Direction.ASC, "author");
            case "AUTHOR_DESC" -> Sort.by(Sort.Direction.DESC, "author");
            default -> Sort.by(Sort.Direction.ASC, "title");
        };
    }

    public Painting post(Painting painting) {
        return repository.save(painting);
    }

}

