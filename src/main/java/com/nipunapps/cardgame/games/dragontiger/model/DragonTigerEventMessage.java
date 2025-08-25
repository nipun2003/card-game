package com.nipunapps.cardgame.games.dragontiger.model;

import com.nipunapps.cardgame.games.dragontiger.enums.DragonTigerEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DragonTigerEventMessage<T> {

    private DragonTigerEventType eventType;
    private String message;
    private T data;
}
