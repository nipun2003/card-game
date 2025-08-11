package com.nipunapps.cardgame.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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

}
