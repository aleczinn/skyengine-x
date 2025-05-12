package de.skyengine.graphics.framebuffer;

import de.skyengine.core.EngineConfig;
import de.skyengine.core.SkyEngine;
import de.skyengine.core.io.IDisposable;
import de.skyengine.graphics.shader.Shader;
import de.skyengine.util.logging.LogManager;
import de.skyengine.util.logging.Logger;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.NVFramebufferMultisampleCoverage;

import static org.lwjgl.opengl.GL11C.GL_RGBA8;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.NVFramebufferMultisampleCoverage.glRenderbufferStorageMultisampleCoverageNV;

public class FrameBuffer implements IDisposable {

    private final Logger logger = LogManager.getLogger(FrameBuffer.class.getName());

    private final EngineConfig config;

    private int id;

    private int colorRbo;
    private int depthRbo;

    /** The number of coverage samples for a multisampled framebuffer, if useNvMultisampleCoverage is <code>true</code>. */
    private static final int COVERAGE_SAMPLES = 2;

    /** The number of color samples for a multisampled framebuffer, if is <code>true</code>. */
    private static final int COLOR_SAMPLES = 1;

    public FrameBuffer(EngineConfig config) {
        this.config = config;
    }

    public void create() {
        if (this.id != 0) {
            this.dispose();
        }

        boolean useNvMultisampleCoverage = SkyEngine.get().window().getCapabilities().GL_NV_framebuffer_multisample_coverage;

        this.id = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.id);

        // Color Buffer
        this.colorRbo = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, this.colorRbo);

        if (useNvMultisampleCoverage) {
            NVFramebufferMultisampleCoverage.glRenderbufferStorageMultisampleCoverageNV(GL30.GL_RENDERBUFFER, COVERAGE_SAMPLES, COLOR_SAMPLES, GL30.GL_RGBA8, this.config.getWindowWidth(), this.config.getWindowHeight());
        } else {
            GL30.glRenderbufferStorageMultisample(GL_RENDERBUFFER, COVERAGE_SAMPLES, GL_RGBA8, this.config.getWindowWidth(), this.config.getWindowHeight());
        }
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_RENDERBUFFER, this.colorRbo);

        // Depth Buffer
        this.depthRbo = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, this.depthRbo);
        if (useNvMultisampleCoverage) {
            NVFramebufferMultisampleCoverage.glRenderbufferStorageMultisampleCoverageNV(GL_RENDERBUFFER, COVERAGE_SAMPLES, COLOR_SAMPLES, GL_DEPTH_COMPONENT32F, this.config.getWindowWidth(), this.config.getWindowHeight());
        } else {
            GL30.glRenderbufferStorageMultisample(GL30.GL_RENDERBUFFER, COVERAGE_SAMPLES, GL30.GL_DEPTH_COMPONENT32F, this.config.getWindowWidth(), this.config.getWindowHeight());
        }
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, depthRbo);

        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

        this.logger.debug("Create framebuffer with id " + this.id);
    }

    public void bind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.id);
    }

    public void unbind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void dispose() {
        this.logger.debug("delete framebuffer with id " + this.id);

        GL30.glDeleteFramebuffers(this.id);
        GL30.glDeleteRenderbuffers(new int[] {this.colorRbo, this.depthRbo});
    }
}
