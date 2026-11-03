package com.example.yumyum.dto;

public record PostResponse(
        Long id,
        String title,
        String description,
        String imageUrl,
        String ingredients,
        String instructions,
        UserResponse user
) {
}
