package com.example.yumyum.dto;

public class RatingSummary {

    private Long postId;
    private Double averageRating;
    private Long reviewCount;

    public RatingSummary(Long postId, Double averageRating, Long reviewCount) {
        this.postId = postId;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }

    public Long getPostId() {
        return postId;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public Long getReviewCount() {
        return reviewCount;
    }
}