package com.nipunapps.cardgame.controller;

import com.nipunapps.cardgame.dto.request.UserCreateRequestDto;
import com.nipunapps.cardgame.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequestMapping("/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    public Mono<ResponseEntity<Void>> signup(@RequestBody UserCreateRequestDto user) {
        return userService.createUser(user)
                .then(Mono.just(ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(null))
                );
    }
}
