package com.example.yumyum.controller;

import com.example.yumyum.config.DiscoverCombinationConfig;
import com.example.yumyum.dto.DiscoverRecipesResponse;
import com.example.yumyum.dto.RatingSummary;
import com.example.yumyum.entity.Review;
import com.example.yumyum.repository.PostRepository;
import com.example.yumyum.repository.ReviewRepository;
import com.example.yumyum.service.ExternalRecipeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.example.yumyum.entity.Post;
import com.example.yumyum.entity.User;
import com.example.yumyum.repository.UserRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class PageController {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ExternalRecipeService externalRecipeService;
    private final DiscoverCombinationConfig discoverCombinationConfig;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public PageController(PostRepository postRepository,
                          UserRepository userRepository,
                          ReviewRepository reviewRepository,
                          ExternalRecipeService externalRecipeService,
                          DiscoverCombinationConfig discoverCombinationConfig) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.externalRecipeService = externalRecipeService;
        this.discoverCombinationConfig = discoverCombinationConfig;
    }

    @GetMapping("/")
    public String home(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String ingredient,
            Model model) {

        DiscoverRecipesResponse discoverData =
                externalRecipeService.getDiscoverRecipes(area, ingredient);

        if (discoverData == null || discoverData.recipeName() == null) {
            discoverData = new DiscoverRecipesResponse(
                    null,
                    JsonNodeFactory.instance.arrayNode()
            );
        }

        model.addAttribute("discoverData", discoverData);

        model.addAttribute(
                "recommendedRecipes",
                externalRecipeService.getRecommendedRecipes()
        );

        List<Post> communityRecipes = postRepository.findRandomPosts(3);

        List<RatingSummary> summaries = reviewRepository.findRatingSummaries();

        Map<Long, RatingSummary> ratingMap = summaries.stream()
                .collect(Collectors.toMap(
                        RatingSummary::getPostId,
                        summary -> summary
                ));

        model.addAttribute("communityRecipes", communityRecipes);
        model.addAttribute("ratingMap", ratingMap);

        List<String> areas = externalRecipeService.getAvailableAreas();

        List<String> ingredients = externalRecipeService.getAvailableIngredients();

        model.addAttribute("discoverAreas", areas);
        model.addAttribute("discoverIngredients", ingredients);

        model.addAttribute("selectedArea", area);
        model.addAttribute("selectedIngredient", ingredient);

        return "home";
    }

    @GetMapping("/posts")
    public String showPosts(Model model) {

        model.addAttribute("posts", postRepository.findAll());

        List<RatingSummary> summaries =
                reviewRepository.findRatingSummaries();

        Map<Long, RatingSummary> ratingMap = summaries.stream()
                .collect(Collectors.toMap(
                        RatingSummary::getPostId,
                        summary -> summary
                ));

        model.addAttribute("ratingMap", ratingMap);

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

        // passing rating data to post detail page
        List<RatingSummary> summaries =
                reviewRepository.findRatingSummaries();

        Map<Long, RatingSummary> ratingMap = summaries.stream()
                .collect(Collectors.toMap(
                        RatingSummary::getPostId,
                        summary -> summary
                ));

        model.addAttribute("ratingMap", ratingMap);

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
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false) String ingredients,
            @RequestParam(required = false) String instructions,
            Authentication authentication) {

        Post post = new Post();
        post.setTitle(title);
        post.setDescription(description);
        post.setIngredients(ingredients);
        post.setInstructions(instructions);

        // Handle uploaded recipe image
        if (imageFile != null && !imageFile.isEmpty()) {

            // Limit image size to 5 MB
            if (imageFile.getSize() > 5 * 1024 * 1024) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Image must be smaller than 5 MB."
                );
            }

            try {
                String imagePath = saveImage(imageFile, "recipes");
                post.setImageUrl(imagePath);

            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        e.getMessage()
                );
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload recipe image.", e);
            }
        }

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

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
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
            @RequestParam(required = false) MultipartFile imageFile,
            @RequestParam(required = false) String ingredients,
            @RequestParam(required = false) String instructions,
            Authentication authentication) throws IOException {

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow();

        // Only the post owner can edit it
        if (!post.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You cannot edit another user's post");
        }

        // Keep the existing image path
        String oldImagePath = post.getImageUrl();

        // Replace the image only when a new file is uploaded
        if (imageFile != null && !imageFile.isEmpty()) {

            if (imageFile.getSize() > 5 * 1024 * 1024) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Image must be smaller than 5 MB."
                );
            }

            try {
                String newImagePath = saveImage(imageFile, "recipes");
                post.setImageUrl(newImagePath);

            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        e.getMessage()
                );
            }
        }

        post.setTitle(title);
        post.setDescription(description);
        post.setIngredients(ingredients);
        post.setInstructions(instructions);

        postRepository.save(post);

        // Delete the old image only after the post has been saved
        if (imageFile != null && !imageFile.isEmpty()) {
            deleteOldProfileImage(oldImagePath);
        }

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
            if (authentication == null
                    || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken) {
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

        List<RatingSummary> summaries =
                reviewRepository.findRatingSummaries();

        Map<Long, RatingSummary> ratingMap = summaries.stream()
                .collect(Collectors.toMap(
                        RatingSummary::getPostId,
                        summary -> summary
                ));

        model.addAttribute("ratingMap", ratingMap);

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
            @RequestParam(required = false) MultipartFile profileImage,
            Authentication authentication) throws IOException {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow();

        user.setUsername(username);
        user.setBio(bio);

        // Keep the old image path before replacing it
        String oldImagePath = user.getProfileImage();

        // Update profile picture only if a new image was uploaded
        if (profileImage != null && !profileImage.isEmpty()) {
            String imagePath = saveProfileImage(profileImage);
            user.setProfileImage(imagePath);
        }

        userRepository.save(user);

        // Delete the old image after successfully saving the new path
        if (profileImage != null && !profileImage.isEmpty()) {
            deleteOldProfileImage(oldImagePath);
        }

        return "redirect:/profile";
    }

    private String getImageExtension(String contentType) {

        if (contentType == null) {
            throw new IllegalArgumentException("Invalid image type");
        }

        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException("Unsupported image type");
        };
    }

    private String saveProfileImage(MultipartFile file) throws IOException {
        return saveImage(file, "profiles");
    }

    private String saveImage(MultipartFile file, String subfolder)
            throws IOException {

        String extension = getImageExtension(file.getContentType());

        String filename = UUID.randomUUID() + extension;

        Path uploadPath = Paths.get(uploadDir)
                .resolve(subfolder);

        Files.createDirectories(uploadPath);

        Path filePath = uploadPath.resolve(filename);

        Files.copy(
                file.getInputStream(),
                filePath,
                StandardCopyOption.REPLACE_EXISTING
        );

        return "/uploads/" + subfolder + "/" + filename;
    }

    private void deleteOldProfileImage(String imagePath) throws IOException {

        if (imagePath == null || !imagePath.startsWith("/uploads/")) {
            return;
        }

        Path uploadPath = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();

        String relativePath = imagePath.substring("/uploads/".length());

        Path imageFile = uploadPath.resolve(relativePath)
                .normalize();

        // Prevent deleting files outside the upload directory
        if (imageFile.startsWith(uploadPath)) {
            Files.deleteIfExists(imageFile);
        }
    }

    @GetMapping("/search")
    public String searchRecipes(
            @RequestParam(required = false) String query,
            Model model) {

        JsonNode recipes = externalRecipeService.searchRecipes(query);

        model.addAttribute("recipes", recipes);
        model.addAttribute("query", query == null ? "" : query.trim());

        return "search";
    }

    @GetMapping("/recipe-detail")
    public String showRecipeDetail(@RequestParam String id, Model model) {

        JsonNode response = externalRecipeService.getRecipeById(id);

        if (response == null) {
            return "redirect:/";
        }

        JsonNode recipe = response.path("meals").path(0);

        if (recipe.isMissingNode() || recipe.isNull()) {
            return "redirect:/";
        }

        model.addAttribute("recipe", recipe);

        return "recipe-detail";
    }
}
