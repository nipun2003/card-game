package com.nipunapps.cardgame.service;

import com.nipunapps.cardgame.dto.request.UserCreateRequestDto;
import com.nipunapps.cardgame.entity.UserEntity;
import com.nipunapps.cardgame.enums.AppRoles;
import com.nipunapps.cardgame.exception.EmailAlreadyExistException;
import com.nipunapps.cardgame.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Mono<Void> createUser(
            UserCreateRequestDto request
    ) {

        String email = request.getEmail();
        // Check if the user already exists
        return userRepository.findByEmail(email)
                .flatMap(user -> Mono.error(new EmailAlreadyExistException(email)))
                .switchIfEmpty(Mono.defer(() -> {
                    UserEntity entity = request.toEntity();
                    entity.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                    entity.setRoles(List.of(AppRoles.USER.name()));
                    return userRepository.save(entity);
                })).then();
    }
}
