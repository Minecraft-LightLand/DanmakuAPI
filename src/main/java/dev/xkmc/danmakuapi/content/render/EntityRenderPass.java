package dev.xkmc.danmakuapi.content.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Tells apart the normal world entity pass from the extra entity pass Iris runs for its
 * shadow map.
 * <p>
 * Danmaku are not drawn by the entity renderer directly: they only record their transform in
 * {@link dev.xkmc.fastprojectileapi.render.ProjectileRenderHelper} and the whole batch is
 * flushed from {@code RenderLevelStageEvent}, i.e. only in the main pass. Iris renders entities
 * into a shadow map with its own {@link net.minecraft.client.renderer.RenderBuffers} and does
 * not fire that event, so anything queued during its shadow pass would survive until the main
 * pass and be drawn there with light-space matrices: every danmaku would appear a second time
 * at the wrong place. Such queue entries have to be dropped.
 */
public final class EntityRenderPass {

	private EntityRenderPass() {
	}

	/**
	 * Whether {@code buffer} belongs to the normal level pass rather than a secondary pass such
	 * as Iris's shadow map.
	 */
	public static boolean isMainPass(MultiBufferSource buffer) {
		var buffers = Minecraft.getInstance().renderBuffers();
		// Glowing entities are rendered through the outline source, which still belongs to the
		// main pass and is the same object for every entity in it.
		return buffer == buffers.bufferSource() || buffer == buffers.outlineBufferSource();
	}

}
