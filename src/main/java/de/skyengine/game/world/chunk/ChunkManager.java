package de.skyengine.game.world.chunk;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChunkManager {

    private final ExecutorService executor;

    public ChunkManager() {
        int numThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        this.executor = Executors.newFixedThreadPool(numThreads, r -> {
            Thread t = new Thread(r);
            t.setPriority(Thread.MIN_PRIORITY);
            t.setName("Chunk Worker");
            t.setDaemon(true);
            return t;
        });
    }
}
