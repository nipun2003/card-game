package com.nipunapps.cardgame.models;

import com.nipunapps.cardgame.dto.response.player.PlayerDto;
import com.nipunapps.cardgame.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerModel {

    private String id;
    private String name;
    @Builder.Default
    private String profileUri = "";

    @Builder.Default
    private long pocketCoin = 50_000;

    private WebSocketSession session;

    @Builder.Default
    private Instant lastBreath = Instant.now();

    @Builder.Default
    private boolean isSeated = false;

    public PlayerDto toDto() {
        return PlayerDto.builder()
                .id(id)
                .name(name)
                .profileUri(profileUri)
                .pocketCoin(pocketCoin)
                .build();
    }

    public static PlayerModel fromUserEntity(UserEntity user) {
        return PlayerModel.builder()
                .id(user.getId())
                .name(user.getName())
                .profileUri(user.getProfilePictureUrl())
                .pocketCoin(user.getCoins())
                .build();
    }

    public static PlayerModel fromUserEntity(UserEntity user, WebSocketSession session) {
        return PlayerModel.builder()
                .id(user.getId())
                .name(user.getName())
                .profileUri(user.getProfilePictureUrl())
                .pocketCoin(user.getCoins())
                .session(session)
                .build();
    }
}
