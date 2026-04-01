package com.example.fashonshop.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping
    public String test() {
        return "Backend OK";
    }

    // POST dạng text
    @PostMapping("/post")
    public String testPost(@RequestBody String data) {
        return "Bạn gửi: " + data;
    }

    // POST dạng JSON
    @PostMapping("/post-json")
    public Map<String, Object> testJson(@RequestBody Map<String, Object> body) {
        return body;
    }
}