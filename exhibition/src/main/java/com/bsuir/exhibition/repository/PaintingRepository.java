package com.bsuir.exhibition.repository;

import com.bsuir.exhibition.entity.Painting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface PaintingRepository extends JpaRepository<Painting, String>, JpaSpecificationExecutor<Painting> {

    Painting getByTitle(String title);

    @Query("SELECT DISTINCT p.style FROM Painting p WHERE p.style IS NOT NULL AND p.style <> '' ORDER BY p.style ASC")
    List<String> findDistinctStyles();
}
