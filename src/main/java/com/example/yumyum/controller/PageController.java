package com.example.yumyum.controller;

import com.example.yumyum.repository.PostRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.example.yumyum.entity.Post;
import com.example.yumyum.entity.User;
import com.example.yumyum.repository.UserRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PageController {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PageController(PostRepository postRepository,  UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/posts")
    public String showPosts(Model model) {
        model.addAttribute("posts", postRepository.findAll());
        return "posts";
    }

    @GetMapping("/post-detail")
    public String showPostDetail() {
        return "post-detail";
    }

    @GetMapping("/posts/new")
    public String showCreatePost() {
        return "create-post";
    }

    @PostMapping("/posts/new")
    public String createPost(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) String imageUrl,
            RedirectAttributes redirectAttributes) {

        Post post = new Post();
        post.setTitle(title);
        post.setDescription(description);
        post.setImageUrl(imageUrl);

        User user = userRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("User not found"));

        post.setUser(user);

        postRepository.save(post);

        return "redirect:/posts";
    }
}