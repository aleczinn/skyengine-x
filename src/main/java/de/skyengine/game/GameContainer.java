package de.skyengine.game;

import de.skyengine.core.input.Input;
import de.skyengine.core.io.IDisposable;
import de.skyengine.core.io.IRenderable;
import de.skyengine.core.io.IResizeable;
import de.skyengine.core.io.IUpdatable;
import de.skyengine.game.world.World;

public class GameContainer implements IUpdatable, IRenderable, IResizeable, IDisposable {

    private World world;

    public GameContainer() {
        this.world = new World("world");
    }

    @Override
    public void update(Input input) {

    }

    @Override
    public void render(float partialTick) {

    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void dispose() {

    }

    public World getWorld() {
        return world;
    }
}
