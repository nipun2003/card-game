package com.nipunapps.cardgame.controller;

import com.nipunapps.cardgame.dto.request.LoginRequestDto;
import com.nipunapps.cardgame.dto.request.UserCreateRequestDto;
import com.nipunapps.cardgame.dto.response.BaseResponse;
import com.nipunapps.cardgame.dto.response.auth.LoginResponseDto;
import com.nipunapps.cardgame.security.CustomReactiveAuthenticationManager;
import com.nipunapps.cardgame.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RequestMapping("/auth")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final CustomReactiveAuthenticationManager authenticationManager;
    private final ServerSecurityContextRepository securityContextRepository;

    @PostMapping("/signup")
    public Mono<ResponseEntity<Void>> signup(@RequestBody UserCreateRequestDto user) {
        return userService.createUser(user)
                .then(Mono.just(ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(null))
                );
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<BaseResponse<LoginResponseDto>>> login(@RequestBody LoginRequestDto request,
                                                                      ServerWebExchange exchange) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
        );
        return authenticationManager.authenticate(token)
                .flatMap(authentication -> {
                    // If authentication is successful, return a 200 OK response
                    LoginResponseDto responseDto = new LoginResponseDto(
                            authentication.getName(),
                            authentication.getAuthorities().stream().map(c -> c.getAuthority()).toList()
                    );
                    final var responseData = BaseResponse.<LoginResponseDto>builder()
                            .success(true)
                            .data(responseDto)
                            .build();
                    return securityContextRepository.save(exchange, new SecurityContextImpl(authentication))
                            .thenReturn(ResponseEntity.ok()
                                    .body(responseData));
                });
    }
}
