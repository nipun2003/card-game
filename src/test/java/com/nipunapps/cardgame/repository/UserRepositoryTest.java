package com.nipunapps.cardgame.repository;

import com.nipunapps.cardgame.entity.UserEntity;
import com.nipunapps.cardgame.testannotation.IntegrationTest;
import com.nipunapps.cardgame.testconfig.AbstractContainerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import java.util.List;

@IntegrationTest
class UserRepositoryTest extends AbstractContainerProperties {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Test inserting a user")
    void testInsertUser() {
        final var user = UserEntity.builder()
                .email("test@gmail.com")
                .name("Test User")
                .passwordHash("hashedPassword")
                .build();

        // First save the user and then try to find the user with the same id in reactive chain
        userRepository.save(user)
                .flatMap(savedUser -> userRepository.findById(user.getId()))
                .as(StepVerifier::create)
                .expectNextMatches(existingUser -> existingUser.getEmail().equals(user.getEmail())
                        && existingUser.getName().equals(user.getName())
                        && existingUser.getPasswordHash().equals(user.getPasswordHash())
                        && existingUser.getProfilePictureUrl().isEmpty()
                        && existingUser.getCoins() == 50000)
                .verifyComplete();

    }

    @BeforeEach
    void setUp() {
        // Clear the repository before each test to ensure a clean state
        userRepository.deleteAll()
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    @DisplayName("Test findAll users")
    void testFindAllUsers() {
        // This test assumes that the database is empty before running
        userRepository.findAll()
                .as(StepVerifier::create)
                .expectNextCount(0) // Expect no users to be present initially
                .verifyComplete();

        // Optionally, you can insert a user and then check if it appears in the findAll results
        final var user1 = UserEntity.builder()
                .email("hello@gmail.com")
                .name("Hello User")
                .passwordHash("hashedPassword")
                .build();

        final var user2 = UserEntity.builder()
                .email("hello2@gmail.com")
                .name("Hello User 2")
                .passwordHash("hashedPassword2")
                .build();

        // Save users and then find all
        userRepository.saveAll(List.of(user1, user2))
                .thenMany(userRepository.findAll())
                .as(StepVerifier::create)
                .expectNextCount(2) // Just verify count since order is not guaranteed
                .verifyComplete();
    }

    @Test
    @DisplayName("Test find by email")
    void testFindByEmail() {

        // Check for non-existing email
        userRepository.findByEmail("abc")
                .as(StepVerifier::create)
                .expectNextCount(0) // Expect no user to be found
                .verifyComplete();

        // Insert a user and then find by email
        final var user = UserEntity.builder()
                .email("hello@gmail.com")
                .name("Hello User")
                .passwordHash("hashedPassword")
                .build();

        userRepository.save(user)
                .then(userRepository.findByEmail("hello@gmail.com"))
                .as(StepVerifier::create)
                .expectNextMatches(existingUser -> existingUser.getEmail().equals(user.getEmail())
                        && existingUser.getName().equals(user.getName())
                        && existingUser.getPasswordHash().equals(user.getPasswordHash())
                        && existingUser.getProfilePictureUrl().isEmpty()
                        && existingUser.getCoins() == 50000)
                .verifyComplete();
    }

}