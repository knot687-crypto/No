package dev.xeys.visuals.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.system.MemoryStack;

/**
 * A uniform buffer split into equally sized, correctly aligned slots. All slots are rewritten in one
 * upload per use, then bound individually per render pass.
 */
public final class UniformSlots implements AutoCloseable {
	private final String label;
	private final int blockSize;
	private final int slotCount;
	private int stride;
	private GpuBuffer buffer;

	public UniformSlots(String label, int blockSize, int slotCount) {
		this.label = label;
		this.blockSize = blockSize;
		this.slotCount = slotCount;
	}

	private void ensure() {
		if (this.buffer == null || this.buffer.isClosed()) {
			int align = Math.max(RenderSystem.getDevice().getUniformOffsetAlignment(), 16);
			this.stride = (this.blockSize + align - 1) / align * align;
			this.buffer = RenderSystem.getDevice().createBuffer(() -> this.label, GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
				(long) this.stride * this.slotCount);
		}
	}

	/** Writes all slots. The writer receives (slot index, builder positioned at that slot). */
	public void write(int usedSlots, SlotWriter writer) {
		this.ensure();
		int count = Math.min(usedSlots, this.slotCount);
		try (MemoryStack stack = MemoryStack.stackPush()) {
			ByteBuffer data = stack.calloc(this.stride * count);
			for (int i = 0; i < count; i++) {
				// slice() resets the byte order to big-endian; std140 data must be native order.
				ByteBuffer slot = data.slice(i * this.stride, this.blockSize).order(ByteOrder.nativeOrder());
				writer.write(i, Std140Builder.intoBuffer(slot));
			}
			data.position(0);
			RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.buffer.slice(0, (long) this.stride * count), data);
		}
	}

	public GpuBufferSlice slot(int index) {
		this.ensure();
		return this.buffer.slice((long) index * this.stride, this.blockSize);
	}

	@Override
	public void close() {
		if (this.buffer != null) {
			this.buffer.close();
			this.buffer = null;
		}
	}

	@FunctionalInterface
	public interface SlotWriter {
		void write(int slot, Std140Builder builder);
	}
}
