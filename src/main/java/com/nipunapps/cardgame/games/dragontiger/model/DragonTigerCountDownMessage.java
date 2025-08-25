package com.nipunapps.cardgame.games.dragontiger.model;

import com.nipunapps.cardgame.games.dragontiger.enums.DragonTigerCountDownEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DragonTigerCountDownMessage {

    private int countDownTime;
    private DragonTigerCountDownEvent countDownEvent;
}
