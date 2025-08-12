package com.nipunapps.cardgame.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/user")
@RestController
public class UserController {

    @GetMapping("/me")
    public String getCurrentUser() {
        // This is a placeholder. In a real application, you would retrieve the user details from the security context or database.
        return "Current User Details";
    }
}
