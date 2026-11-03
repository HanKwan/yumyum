package com.example.yumyum.controller;

import com.example.yumyum.dto.PostResponse;
import com.example.yumyum.dto.UserResponse;
import com.example.yumyum.repository.PostRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PostController {

    private final PostRepository postRepository;

    public PostController(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @GetMapping("/api/posts")
    public List<PostResponse> getPosts() {
        return postRepository.findAll().stream()
                .map(post -> new PostResponse(
                        post.getId(),
                        post.getTitle(),
                        post.getDescription(),
                        post.getImageUrl(),
                        post.getIngredients(),
                        post.getInstructions(),
                        new UserResponse(
                                post.getUser().getId(),
                                post.getUser().getUsername(),
                                post.getUser().getBio(),
                                post.getUser().getProfileImage()
                        )
                ))
                .toList();
    }
}
