package dev.xkmc.danmakuapi.content.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.xkmc.fastprojectileapi.entity.SimplifiedProjectile;
import dev.xkmc.fastprojectileapi.render.ProjectileRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.List;
import java.util.function.Consumer;

/**
 * Directional "flat" danmaku rendering: two crossed quads sharing the flight axis,
 * oriented by projectile yaw/pitch like {@link ButterflyProjectileType}, instead of
 * camera-facing billboards like {@link SimpleProjectileType}.
 * <p>
 * The texture top edge points along the flight direction, so elongated sprites
 * (talisman cards, thrown daggers) always point where they fly, Touhou-style.
 * An optional slow roll around the flight axis makes paper-like danmaku flutter.
 *
 * @param spin ticks per full roll around the flight axis; 0 disables rolling
 */
public record FlatProjectileType(ResourceLocation tex, DisplayType display, double spin)
		implements RenderableDanmakuType<FlatProjectileType, FlatProjectileType.Ins> {

	@Override
	public void start(MultiBufferSource buffer, List<Ins> list) {
		BulkDataWriter vc = new BulkDataWriter(buffer.getBuffer(DanmakuRenderStates.danmaku(tex, display())), list.size());
		for (var e : list) {
			e.tex(vc);
		}
		vc.flush();
	}

	@Override
	public void create(Consumer<Ins> holder, ProjectileRenderer<?> r, SimplifiedProjectile e, PoseStack pose, float pTick) {
		pose.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(pTick, e.yRotO, e.getYRot())));
		pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(pTick, e.xRotO, e.getXRot())));
		if (spin > 0) {
			pose.mulPose(Axis.ZP.rotationDegrees((e.tickCount + pTick) * 360f / (float) spin));
		}
		var m4 = new Matrix4f(pose.last().pose());
		int col = DanmakuRenderStates.fading(display, -1, r, e);
		holder.accept(new Ins(m4, col));
	}

	public record Ins(Matrix4f m4, int color) {

		public void tex(BulkDataWriter vc) {
			// quad A in the ZX plane (horizontal fin)
			vertex(vc, m4, 0.5f, 0, 0.5f, 1, 0, color);
			vertex(vc, m4, 0.5f, 0, -0.5f, 1, 1, color);
			vertex(vc, m4, -0.5f, 0, -0.5f, 0, 1, color);
			vertex(vc, m4, -0.5f, 0, 0.5f, 0, 0, color);
			// quad B in the ZY plane (vertical fin), same UVs so both show the full sprite
			vertex(vc, m4, 0, 0.5f, 0.5f, 1, 0, color);
			vertex(vc, m4, 0, -0.5f, 0.5f, 1, 1, color);
			vertex(vc, m4, 0, -0.5f, -0.5f, 0, 1, color);
			vertex(vc, m4, 0, 0.5f, -0.5f, 0, 0, color);
		}

		private static void vertex(BulkDataWriter vc, Matrix4f m4, float x, float y, float z, float u, float v, int color) {
			vc.addVertex(m4, x, y, z, u, v, color);
		}

	}

}
