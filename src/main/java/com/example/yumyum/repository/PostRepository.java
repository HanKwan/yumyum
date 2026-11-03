package com.example.yumyum.repository;

import com.example.yumyum.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post,Long> {
    List<Post> findByUserIdOrderByIdDesc(Long userId);

    @Query(value = "SELECT * FROM posts ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Post> findRandomPosts(@Param("limit") int limit);
}