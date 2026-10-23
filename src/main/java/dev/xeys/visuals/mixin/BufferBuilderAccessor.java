package dev.xeys.visuals.mixin;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes {@code BufferBuilder#beginElement} so custom vertex elements (shape parameters, clip rects)
 * can be written straight into the vertex memory without any intermediate allocation.
 */
@Mixin(BufferBuilder.class)
public interface BufferBuilderAccessor {
	@Invoker("beginElement")
	long xeys$beginElement(VertexFormatElement element);
}
