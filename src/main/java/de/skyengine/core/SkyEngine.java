package de.skyengine.core;

import de.skyengine.core.file.Files;
import de.skyengine.core.input.Input;
import de.skyengine.util.DelayedRunnable;
import de.skyengine.util.logging.LogManager;
import de.skyengine.util.logging.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

public class SkyEngine {

    public static final String ENGINE_NAME = "SkyEngine";
    public static final String ENGINE_VERSION = "1.0.0";

    private static SkyEngine instance = null;
    private final Logger logger = LogManager.getLogger(SkyEngine.class.getName());

    private static final int TPS = 20;
    private static final long TICK_TIME_NANOS = 1_000_000_000 / TPS;

    private final EngineConfig config;

    private final Window window;
    private final Input input;
    private final Files files;

    private final Queue<DelayedRunnable> tasks;

    public SkyEngine(EngineConfig config) {
        instance = this;

        this.config = config;
        this.window = new Window(config);
        this.input = new Input(this.window);
        this.files = new Files();
        this.tasks = new ConcurrentLinkedQueue<>();
    }

    private void onUpdate() {

    }

    private void render(float partialTick) {
        GL11.glClearColor(
                this.config.getWindowClearColor().red,
                this.config.getWindowClearColor().green,
                this.config.getWindowClearColor().blue,
                this.config.getWindowClearColor().alpha
        );
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GLFW.glfwSwapBuffers(this.window.getWindowID());
    }

    private void onExit() {
        this.logger.info("Stopping!");

        this.drainRunnables();
        GL.setCapabilities(null);
    }

    private void gameLoop() {
        this.drainRunnables();

        long lastTickTime = System.nanoTime();
        long accumulatedTime = 0;
        double lastLoopTime = 0;

        int frames = 0;
        int updates = 0;
        long lastStatusTime = System.currentTimeMillis();

        while (!this.window.shouldClose()) {
            long currentTime = System.nanoTime();
            long frameTime = currentTime - lastTickTime;
            lastTickTime = currentTime;
            lastLoopTime = GLFW.glfwGetTime();

            accumulatedTime += frameTime;

            this.drainRunnables();

            this.input.update();

            int ticksProcessed = 0;
            while (accumulatedTime >= TICK_TIME_NANOS && ticksProcessed < 10) {
                this.onUpdate();
                accumulatedTime -= TICK_TIME_NANOS;
                ticksProcessed++;
                updates++;
            }

            if (ticksProcessed >= 10) {
                this.logger.warning("Can't keep up with TPS! Skipping " + (accumulatedTime / TICK_TIME_NANOS) + " Ticks.");
                accumulatedTime = 0;
            }

            float partialTick = (float) accumulatedTime / TICK_TIME_NANOS;
            this.render(partialTick);
            frames++;

            // TODO: Add fps limit function here
            if (this.config.getBackgroundFPS() > TPS && this.config.isMinimized()) {
                this.sync(this.config.getBackgroundFPS(), lastLoopTime);
            }

            // show states each 1 second
            if (System.currentTimeMillis() - lastStatusTime >= 1000) {
                System.out.printf("FPS: %d, TPS: %d%n", frames, updates);
                if (this.config.isWindowed() && !this.config.getDebugMode().equals(EngineConfig.DebugMode.NONE)) {
                    this.window.setTitle("%s v%s | FPS: %d, TPS: %d".formatted(SkyEngine.ENGINE_NAME, SkyEngine.ENGINE_VERSION, frames, updates));
                }

                frames = 0;
                updates = 0;
                lastStatusTime = System.currentTimeMillis();
            }
        }

        this.onExit();
    }

    public void sync(int fps, double currentLoopTime) {
        float targetTime = 1F / fps;
        double nextFrameTime = currentLoopTime + targetTime;

        while (GLFW.glfwGetTime() < nextFrameTime) {
            double remainingTime = nextFrameTime - GLFW.glfwGetTime();
            if (remainingTime > 0.002) { // Mehr als 2ms übrig -> sleep
                try {
                    Thread.sleep((long)(remainingTime * 900));
                } catch (InterruptedException e) {
                    this.logger.error(null, e);
                }
            } else {
                Thread.yield();
            }
        }
    }

    public void launch() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            this.tasks.add(new DelayedRunnable(() -> {
                this.window.init();
                this.window.printDebug();
                this.input.init();

                // Configure VAOs, Shader, Textures here
                this.window.getFrameBuffer().create();

                /* Make sure everything is ready before we show the window */
                GL11.glFlush();
                GL11.glFinish();

                /* Notify the latch so that the window can be shown */
                latch.countDown();
                return null;
            }, "Init", 0));

            /* Run logic updates and rendering in a separate thread */
            Thread updateAndRenderThread = new Thread(this::gameLoop);
            updateAndRenderThread.setName("Render Thread");
            updateAndRenderThread.setPriority(Thread.MAX_PRIORITY);
            updateAndRenderThread.start();

            /* Process OS/window event messages in this main thread */
            Thread.currentThread().setName("Window-Processing Thread");
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

            /* Wait for the latch to signal that init render thread actions are done */
            latch.await();
            this.runWindowProcessLoop();

            /*
             * After the runWindowProcessLoop exited (because the window was closed),
             * wait for render thread to complete finalization.
             */
            updateAndRenderThread.join();

            this.window.dispose();
        } catch (InterruptedException e) {
            this.logger.fatal(e);
        }
    }

    /**
     * Loop in the main thread to only process OS/window event messages.
     */
    private void runWindowProcessLoop() {
        GLFW.glfwShowWindow(this.window.getWindowID());

        while (!this.window.shouldClose()) {
            GLFW.glfwWaitEvents();
        }
    }

    private void drainRunnables() {
        Iterator<DelayedRunnable> iterator = this.tasks.iterator();
        while (iterator.hasNext()) {
            DelayedRunnable dr = iterator.next();

            /* Check if we want to delay this runnable */
            if (dr.getDelay() > 0) {
                if (SkyEngine.get().getConfig().getDebugMode().equals(EngineConfig.DebugMode.FULL)) {
                    this.logger.debug("Delaying runnable [" + dr.getName() + "] for " + dr.getDelay() + " frames");
                }
                dr.reduceDelay();
                continue;
            }

            try {
                /* Remove from queue and execute */
                iterator.remove();
                dr.getRunnable().call();
            } catch (Exception e) {
                this.logger.fatal(e);
            }
        }
    }

    /**
     * This method closes the game
     */
    public void shutdown() {
        this.window.forceClose();
    }

    public EngineConfig getConfig() {
        return config;
    }

    public Window getWindow() {
        return window;
    }

    public Input getInput() {
        return input;
    }

    public Files getFiles() {
        return files;
    }

    public Queue<DelayedRunnable> getTasks() {
        return tasks;
    }

    public static SkyEngine get() {
        return instance;
    }
}
