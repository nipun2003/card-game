package com.nipunapps.cardgame.models;

import com.nipunapps.cardgame.dto.response.player.PlayerDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.reactive.socket.WebSocketSession;

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
    private int pocketCoin = 50_000;

    private WebSocketSession session;

    public PlayerDto toDto() {
        return PlayerDto.builder()
                .id(id)
                .name(name)
                .profileUri(profileUri)
                .pocketCoin(pocketCoin)
                .build();
    }
}
