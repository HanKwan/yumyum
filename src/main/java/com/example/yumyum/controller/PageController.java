package com.example.yumyum.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/posts")
    public String showPosts() {
        return "posts";
    }

    @GetMapping("/post-detail")
    public String showPostDetail() {
        return "post-detail";
    }
}
