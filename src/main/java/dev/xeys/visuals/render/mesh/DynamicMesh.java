package dev.xeys.visuals.render.mesh;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

/**
 * Vertices in the POSITION_TEX_COLOR layout, packed on the CPU into one reusable native buffer and
 * uploaded to one reusable GPU buffer. Rebuilding every frame allocates nothing once the buffers have
 * grown to fit.
 */
public final class DynamicMesh implements AutoCloseable {
	/** Bytes per vertex: position (3 floats), uv (2 floats), color (4 bytes). */
	public static final int STRIDE = 24;

	private final String label;
	private ByteBuffer data;
	private @Nullable GpuBuffer buffer;
	private int vertices;

	public DynamicMesh(String label, int initialVertices) {
		this.label = label;
		this.data = MemoryUtil.memAlloc(Math.max(4, initialVertices) * STRIDE);
	}

	public DynamicMesh begin() {
		this.data.clear();
		this.vertices = 0;
		return this;
	}

	public DynamicMesh vertex(float x, float y, float z, float u, float v, int argb) {
		if (this.data.remaining() < STRIDE) {
			int used = this.data.position();
			this.data = MemoryUtil.memRealloc(this.data, this.data.capacity() * 2);
			this.data.limit(this.data.capacity()).position(used);
		}
		this.data.putFloat(x).putFloat(y).putFloat(z).putFloat(u).putFloat(v);
		this.data.put((byte) (argb >> 16)).put((byte) (argb >> 8)).put((byte) argb).put((byte) (argb >>> 24));
		this.vertices++;
		return this;
	}

	public int vertexCount() {
		return this.vertices;
	}

	/** Uploads what was built since {@link #begin()}; returns the buffer to draw from, or null when empty. */
	public @Nullable GpuBuffer upload() {
		int bytes = this.vertices * STRIDE;
		if (bytes == 0) {
			return null;
		}
		if (this.buffer == null || this.buffer.isClosed() || this.buffer.size() < bytes) {
			if (this.buffer != null) {
				this.buffer.close();
			}
			this.buffer = RenderSystem.getDevice().createBuffer(() -> this.label, GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
				Math.max(bytes, this.data.capacity()));
		}
		this.data.flip();
		RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(0, bytes), this.data);
		this.data.limit(this.data.capacity()).position(bytes);
		return this.buffer;
	}

	/** The buffer from the last {@link #upload()}, if any. */
	public @Nullable GpuBuffer buffer() {
		return this.buffer;
	}

	@Override
	public void close() {
		if (this.buffer != null) {
			this.buffer.close();
			this.buffer = null;
		}
		MemoryUtil.memFree(this.data);
	}
}
