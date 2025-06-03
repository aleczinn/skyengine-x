package de.skyengine.game.world.chunk;

public class ChunkPosition {

    private final int x;
    private final int z;

    public ChunkPosition(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public int getWorldX() {
        return this.x << Chunk.CHUNK_SHIFT_WIDTH;
    }

    public int getWorldZ() {
        return this.z << Chunk.CHUNK_SHIFT_WIDTH;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (getClass() != o.getClass()) return false;

        ChunkPosition other = (ChunkPosition) o;
        if (other.x != this.x) return false;
        return other.z == this.z;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.x;
        result = prime * result + this.z;
        return result;
    }

    @Override
    public String toString() {
        return String.format("ChunkPosition={x=%s, z=%s}", this.x, this.z);
    }
}
