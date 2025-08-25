package com.nipunapps.cardgame.games.dragontiger.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlayerJoinedMessage {

    private String id;
    private String name;
    @Builder.Default
    private String profilePic = "";
}
