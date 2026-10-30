package com.example.yumyum.repository;

import com.example.yumyum.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByPostIdOrderByIdDesc(Long postId);

    Optional<Review> findByUserIdAndPostId(Long userId, Long postId);
}