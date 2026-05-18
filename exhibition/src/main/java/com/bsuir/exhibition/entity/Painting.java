package com.bsuir.exhibition.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "paintings")
public class Painting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String id;

    private String title;
    private String author;
    private String style;

    @Column(length = 1000)
    private String description;
    private String imageUrl;

    @ManyToMany(mappedBy = "favoritePaintings")
    @JsonIgnore
    private Set<User> usersWhoLiked = new HashSet<>();

}