package com.example.yumyum.controller;

import com.example.yumyum.repository.PostRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.example.yumyum.entity.Post;
import com.example.yumyum.entity.User;
import com.example.yumyum.repository.UserRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    public String showPostDetail(
            @RequestParam Long id,
            Model model,
            Authentication authentication) {

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        model.addAttribute("post", post);

        String currentUserEmail = authentication != null
                ? authentication.getName()
                : null;

        model.addAttribute("currentUserEmail", currentUserEmail);

        return "post-detail";
    }

    @GetMapping("/posts/new")
    public String showCreatePost() {
        return "create-post";
    }

    @PostMapping("/posts/new")
    public String createPost(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String ingredients,
            @RequestParam(required = false) String instructions,
            Authentication authentication) {

        Post post = new Post();
        post.setTitle(title);
        post.setDescription(description);
        post.setImageUrl(imageUrl);
        post.setIngredients(ingredients);
        post.setInstructions(instructions);

        // Find the currently logged-in user
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow();

        post.setUser(user);

        postRepository.save(post);

        return "redirect:/posts";
    }

    @PostMapping("/posts/delete")
    public String deletePost(
            @RequestParam Long id,
            Authentication authentication) {

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow();

        // Only the post owner can delete it
        if (!post.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You cannot delete another user's post");
        }

        postRepository.delete(post);

        return "redirect:/posts";
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @GetMapping("/posts/edit")
    public String showEditPost(
            @RequestParam Long id,
            Model model,
            Authentication authentication) {

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow();

        // Only the post owner can edit it
        if (!post.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You cannot edit another user's post");
        }

        model.addAttribute("post", post);

        return "edit-post";
    }


    @PostMapping("/posts/edit")
    public String updatePost(
            @RequestParam Long id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String ingredients,
            @RequestParam(required = false) String instructions,
            Authentication authentication) {

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow();

        // Only the post owner can edit it
        if (!post.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You cannot edit another user's post");
        }

        post.setTitle(title);
        post.setDescription(description);
        post.setImageUrl(imageUrl);
        post.setIngredients(ingredients);
        post.setInstructions(instructions);

        postRepository.save(post);

        return "redirect:/post-detail?id=" + post.getId();
    }
}