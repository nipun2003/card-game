package com.nipunapps.cardgame.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nipunapps.cardgame.dto.request.LoginRequestDto;
import com.nipunapps.cardgame.exception.BadLoginRequestFormat;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonBodyAuthenticationConverter implements ServerAuthenticationConverter {

    private final ObjectMapper objectMapper;
    private final Validator validator;


    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return exchange.getRequest().getBody()
                .doFirst(() -> log.info("Converting JSON body to Authentication token"))
                .next()
                .map(dataBuffer -> {
                    try {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        LoginRequestDto dto = objectMapper.readValue(bytes, LoginRequestDto.class);
// ✅ Run bean validation
                        Set<ConstraintViolation<LoginRequestDto>> violations = validator.validate(dto);
                        if (!violations.isEmpty()) {
                            String message = violations.stream()
                                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                                    .collect(Collectors.joining(", "));
                            throw new BadLoginRequestFormat("Validation failed: " + message);
                        }
                        // Optional: Captcha check, IP rate limit, etc.
                        return new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword());
                    } catch (IOException e) {
                        throw new BadLoginRequestFormat("Invalid login request format", e);
                    }
                });
    }
}
