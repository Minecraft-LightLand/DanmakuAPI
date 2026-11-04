package dev.xkmc.danmakuapi.content.render;

import dev.xkmc.danmakuapi.compat.IrisCompat;

/**
 * Tells apart the normal world entity pass from the extra entity pass Iris runs for its
 * shadow map.
 * <p>
 * Danmaku are not drawn by the entity renderer directly: they only record their transform in
 * {@link dev.xkmc.fastprojectileapi.render.ProjectileRenderHelper} and the whole batch is
 * flushed from {@code RenderLevelStageEvent}, i.e. only in the main pass. Iris renders entities
 * into a shadow map separately and does not fire that event, so anything queued during its
 * shadow pass would survive until the main pass and be drawn there with light-space matrices:
 * every danmaku would appear a second time at the wrong place. Such queue entries have to be
 * dropped.
 */
public final class EntityRenderPass {

	private EntityRenderPass() {
	}

	/**
	 * Whether the entity renderer is being called for the normal level pass rather than a
	 * secondary pass such as Iris's shadow map.
	 */
	public static boolean isMainPass() {
		return !IrisCompat.isShadowPass();
	}

}
