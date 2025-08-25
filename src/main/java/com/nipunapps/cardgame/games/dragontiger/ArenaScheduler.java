package com.nipunapps.cardgame.games.dragontiger;

import com.nipunapps.cardgame.enums.GameLifecycle;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class ArenaScheduler {

    private final AtomicReference<Disposable> idleDestroyTask = new AtomicReference<>();
    private final AtomicReference<Disposable> gameStartTask = new AtomicReference<>();

    public void scheduleIdleDestroy(String roomId, DragonTigerArenaState state, Runnable onDestroy, int seconds) {
        cancelTask(idleDestroyTask);

        Disposable disposable = Mono.delay(Duration.ofSeconds(seconds))
                .filter(t -> state.getPlayers().isEmpty() && state.getLifecycle() == GameLifecycle.IDLE)
                .doOnNext(t -> {
                    log.info("Room {} idle for {}s. Destroying...", roomId, seconds);
                    onDestroy.run();
                })
                .subscribe();

        idleDestroyTask.set(disposable);
    }

    public void scheduleGameStart(String roomId, DragonTigerArenaState state, Runnable onStart, int seconds, Runnable onReschedule) {
        cancelTask(gameStartTask);

        Disposable disposable = Mono.delay(Duration.ofSeconds(seconds))
                .doOnNext(t -> {
                    if (state.getPlayers().isEmpty()) {
                        log.info("Room {} empty after {}s, rescheduling idle destroy", roomId, seconds);
                        onReschedule.run();
                        return;
                    }
                    if (state.getLifecycle() == GameLifecycle.WAITING) {
                        log.info("Game in room {} started.", roomId);
                        onStart.run();
                    }
                })
                .subscribe();

        gameStartTask.set(disposable);
    }

    public void cancelAll() {
        cancelTask(idleDestroyTask);
        cancelTask(gameStartTask);
    }

    private void cancelTask(AtomicReference<Disposable> ref) {
        Disposable d = ref.getAndSet(null);
        if (d != null && !d.isDisposed()) d.dispose();
    }
}
