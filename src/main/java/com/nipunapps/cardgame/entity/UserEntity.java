package com.nipunapps.cardgame.entity;

import com.nipunapps.cardgame.security.CustomUserDetails;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "users")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"passwordHash", "name"})
@Getter
@Setter
public class UserEntity {

    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String email;
    private String passwordHash;
    private String name;

    @Builder.Default
    private String profilePictureUrl = "";
    @Builder.Default
    private long coins = 50000;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private boolean locked = false;

    @Builder.Default
    private boolean credentialsExpired = false;

    @Builder.Default
    private int failedLoginAttempts = 0;

    @Builder.Default
    private List<String> roles = new ArrayList<>();

    public CustomUserDetails toCustomUserDetails() {
        return CustomUserDetails.builder()
                .username(this.email)
                .password(this.passwordHash)
                .roles(this.roles)
                .enabled(this.enabled)
                .locked(this.locked)
                .credentialsExpired(this.credentialsExpired)
                .build();
    }
}
