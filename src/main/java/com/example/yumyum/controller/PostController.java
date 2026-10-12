package com.example.yumyum.controller;

import com.example.yumyum.entity.Post;
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
    public List<Post> getPosts() {
        return postRepository.findAll();
    }
}