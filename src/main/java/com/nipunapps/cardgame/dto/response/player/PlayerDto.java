package com.nipunapps.cardgame.dto.response.player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerDto {

    private String id;
    private String name;
    @Builder.Default
    private String profileUri = "";

    @Builder.Default
    private long pocketCoin = 50_000;
}
