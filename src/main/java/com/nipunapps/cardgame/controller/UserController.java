package com.nipunapps.cardgame.controller;

import com.nipunapps.cardgame.dto.response.BaseResponse;
import com.nipunapps.cardgame.dto.response.auth.LoginResponseDto;
import com.nipunapps.cardgame.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequestMapping("/user")
@RestController
public class UserController {

    @GetMapping("/me")
    public Mono<ResponseEntity<BaseResponse<LoginResponseDto>>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return Mono.just(
                ResponseEntity.ok(
                        BaseResponse.<LoginResponseDto>builder()
                                .success(true)
                                .message("User details fetched successfully")
                                .data(LoginResponseDto.builder()
                                        .username(principal.getUsername())
                                        .roles(principal.getAuthorities().stream()
                                                .map(GrantedAuthority::getAuthority)
                                                .toList())
                                        .build())
                                .build()
                )
        );
        // This is a placeholder. In a real application, you would retrieve the user details from the security context or database.
    }
}
