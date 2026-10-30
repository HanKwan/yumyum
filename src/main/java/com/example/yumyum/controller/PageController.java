package com.example.yumyum.controller;

import com.example.yumyum.entity.Review;
import com.example.yumyum.repository.PostRepository;
import com.example.yumyum.repository.ReviewRepository;
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
    private final ReviewRepository reviewRepository;

    public PageController(PostRepository postRepository,
                          UserRepository userRepository,
                          ReviewRepository reviewRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/posts")
    public String showPosts(Model model) {
        model.addAttribute("posts", postRepository.findAll());
        return "posts";
    }

    @GetMapping("/post-detail")
    public String showPostDetail(
            @RequestParam Long id,
            @RequestParam(required = false) String error,
            Model model,
            Authentication authentication) {

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        model.addAttribute(
                "reviews",
                reviewRepository.findByPostIdOrderByIdDesc(id)
        );

        model.addAttribute("post", post);
        model.addAttribute("error", error);

        String currentUserEmail = authentication != null
                ? authentication.getName()
                : null;

        model.addAttribute("currentUserEmail", currentUserEmail);

        Review currentUserReview = null;

        if (authentication != null) {
            User currentUser = userRepository.findByEmail(authentication.getName())
                    .orElseThrow();

            currentUserReview = reviewRepository.findByUserIdAndPostId(
                    currentUser.getId(), id
            ).orElse(null);
        }

        model.addAttribute("currentUserReview", currentUserReview);

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

    @PostMapping("/reviews/new")
    public String createReview(
            @RequestParam Long postId,
            @RequestParam String content,
            @RequestParam int rating,
            Authentication authentication) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow();

        // Only allow one review per user per post
        if (reviewRepository.findByUserIdAndPostId(
                user.getId(), postId).isPresent()) {
            return "redirect:/post-detail?id=" + postId + "&error=already-reviewed";
        }

        Review review = new Review();
        review.setContent(content);
        review.setRating(rating);
        review.setUser(user);
        review.setPost(post);

        reviewRepository.save(review);

        return "redirect:/post-detail?id=" + postId;
    }

    @PostMapping("/reviews/delete")
    public String deleteReview(
            @RequestParam Long id,
            Authentication authentication) {

        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow();

        // Only the review owner can delete it
        if (!review.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You cannot delete another user's review");
        }

        Long postId = review.getPost().getId();

        reviewRepository.delete(review);

        return "redirect:/post-detail?id=" + postId;
    }

    @GetMapping("/profile")
    public String showProfile(
            @RequestParam(required = false) Long id,
            Model model,
            Authentication authentication) {

        User user;

        if (id == null) {
            if(authentication == null) {
                return "redirect:/login";
            }

            // Show the currently logged-in user's profile
            user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow();
        } else {
            // Show another user's profile
            user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }

        model.addAttribute("user", user);

        model.addAttribute(
                "posts",
                postRepository.findByUserIdOrderByIdDesc(user.getId())
        );

        String currentUserEmail = authentication != null
                ? authentication.getName()
                : null;

        model.addAttribute("currentUserEmail", currentUserEmail);

        return "profile";
    }

    @GetMapping("/profile/edit")
    public String showEditProfile(
            Authentication authentication,
            Model model) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow();

        model.addAttribute("user", user);

        return "edit-profile";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(
            @RequestParam String username,
            @RequestParam(required = false) String bio,
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow();

        user.setUsername(username);
        user.setBio(bio);

        userRepository.save(user);

        return "redirect:/profile";
    }
}