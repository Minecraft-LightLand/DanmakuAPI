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
 * Directional "oriented cross" danmaku rendering: two copies of the flat quad from
 * {@link FlatProjectileType}, the second rotated 90 degrees around the flight axis so it is
 * perpendicular to the first and the pair forms a cross along the direction of travel.
 * <p>
 * Like the flat type, both quads are laid in the glide plane and oriented by projectile
 * yaw/pitch, with the texture top edge pointing along the flight direction. Instead of
 * disappearing when seen edge-on, the cross keeps a textured silhouette from every angle.
 * The optional slow roll around the flight axis spins the whole cross and keeps the
 * paper-like flutter.
 *
 * @param spin ticks per full roll around the flight axis; 0 disables rolling
 */
public record CrossProjectileType(ResourceLocation tex, DisplayType display, double spin)
		implements RenderableDanmakuType<CrossProjectileType, CrossProjectileType.Ins> {

	/** Quarter turn around the flight axis that makes the second quad perpendicular to the first. */
	private static final float CROSS_ANGLE = 90f;

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
		// rotate about the flight axis (local +Z), which keeps the top edge pointing forward
		holder.accept(new Ins(new Matrix4f(m4).rotateZ((float) Math.toRadians(CROSS_ANGLE)), col));
	}

	public record Ins(Matrix4f m4, int color) {

		public void tex(BulkDataWriter vc) {
			// single quad in the ZX glide plane (horizontal when flying level);
			// image top points along flight (+Z)
			vertex(vc, m4, 0.5f, 0, 0.5f, 1, 0, color);
			vertex(vc, m4, 0.5f, 0, -0.5f, 1, 1, color);
			vertex(vc, m4, -0.5f, 0, -0.5f, 0, 1, color);
			vertex(vc, m4, -0.5f, 0, 0.5f, 0, 0, color);
		}

		private static void vertex(BulkDataWriter vc, Matrix4f m4, float x, float y, float z, float u, float v, int color) {
			vc.addVertex(m4, x, y, z, u, v, color);
		}

	}

}
