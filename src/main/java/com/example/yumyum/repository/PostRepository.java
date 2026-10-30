package com.example.yumyum.repository;

import com.example.yumyum.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post,Long> {
    List<Post> findByUserIdOrderByIdDesc(Long userId);
}