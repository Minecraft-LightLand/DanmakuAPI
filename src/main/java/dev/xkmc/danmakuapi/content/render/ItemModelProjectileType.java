package dev.xkmc.danmakuapi.content.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.xkmc.fastprojectileapi.entity.SimplifiedProjectile;
import dev.xkmc.fastprojectileapi.render.ProjectileRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Directional danmaku rendering that draws the danmaku item's own baked model instead of a
 * hand-written quad, so the shape comes from the item model and can be swapped or made
 * three-dimensional without touching this class.
 * <p>
 * Still bulk rendered: every instance only records its transform and fading, and the model's
 * geometry is then appended to a single vertex buffer per frame, so a screen full of danmaku
 * still costs a single draw call instead of one per danmaku.
 * <p>
 * The model is laid flat in the glide plane and oriented by projectile yaw/pitch like
 * {@link FlatProjectileType}, so its texture top edge points along the flight direction and
 * elongated sprites always point where they fly, Touhou-style. An optional slow roll around
 * the flight axis banks the model out of the glide plane and makes paper-like danmaku flutter.
 * <p>
 * The model's sprites are read from the item atlas, so this needs the danmaku item itself
 * rather than a texture location; the item texture and the danmaku entity texture are the same
 * image, so nothing changes about which art is shown.
 *
 * @param item the danmaku item whose model is drawn
 * @param spin ticks per full roll around the flight axis; 0 disables rolling
 */
public record ItemModelProjectileType(Item item, DisplayType display, double spin)
		implements RenderableDanmakuType<ItemModelProjectileType, ItemModelProjectileType.Ins> {

	private static final long SEED = 42L;

	/** Ints per vertex in a baked quad, matching the block vertex format quads are baked in. */
	private static final int STRIDE = 8;

	/**
	 * Item models are authored in a [0,1] cube, so this centres the cube on the projectile and
	 * tips it over 90 degrees to drop the art into the glide plane. Generated item art sits at
	 * the middle of that cube, so it lands on the projectile rather than half a unit away.
	 * <p>
	 * Applied last, which puts it innermost: the flight rotations below are about the origin, so
	 * the model has to be centred before they see it.
	 */
	private static final Matrix4f FLAT = new Matrix4f().translate(-0.5F, -0.5F, -0.5F).rotateX((float) (Math.PI / 2));

	@Override
	public void start(MultiBufferSource buffer, List<Ins> list) {
		List<BakedQuad> quads = list.isEmpty() ? List.of() : getQuads();
		if (quads.isEmpty()) return;
		var vc = buffer.getBuffer(DanmakuRenderStates.itemModel(display));
		for (var ins : list) {
			for (var quad : quads) {
				ins.tex(vc, quad);
			}
		}
	}

	@Override
	public void create(Consumer<Ins> holder, ProjectileRenderer<?> r, SimplifiedProjectile e, PoseStack pose, float pTick) {
		pose.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(pTick, e.yRotO, e.getYRot())));
		pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(pTick, e.xRotO, e.getXRot())));
		if (spin > 0) {
			pose.mulPose(Axis.ZP.rotationDegrees((e.tickCount + pTick) * 360f / (float) spin));
		}
		pose.mulPose(FLAT);
		holder.accept(new Ins(new Matrix4f(pose.last().pose()), (float) Mth.clamp(r.fading(e), 0, 1)));
	}

	/**
	 * Collects the model's quads. Resolved per frame rather than cached so a resource reload
	 * cannot leave a stale model behind; this runs once per type per frame, not per danmaku.
	 */
	private List<BakedQuad> getQuads() {
		var renderer = Minecraft.getInstance().getItemRenderer();
		// through ItemRenderer rather than the model shaper so item overrides still apply
		BakedModel model = renderer.getModel(new ItemStack(item), null, null, 0);
		if (model.isCustomRenderer()) return List.of();
		RandomSource rand = RandomSource.create(SEED);
		List<BakedQuad> quads = new ArrayList<>();
		for (Direction direction : Direction.values()) {
			rand.setSeed(SEED);
			quads.addAll(model.getQuads(null, direction, rand, ModelData.EMPTY, null));
		}
		rand.setSeed(SEED);
		quads.addAll(model.getQuads(null, null, rand, ModelData.EMPTY, null));
		return quads;
	}

	/** Same face shading vanilla block models use, folded into the vertex color. */
	private static float shade(BakedQuad quad) {
		if (!quad.isShade()) return 1;
		return quad.getDirection().getAxis().isHorizontal() ? 0.6F : 1;
	}

	public record Ins(Matrix4f m4, float alpha) {

		/**
		 * Appends one baked quad, transformed into place.
		 * <p>
		 * This is {@link VertexConsumer#putBulkData} specialised to a
		 * {@link net.minecraft.client.renderer.vertex.DefaultVertexFormat#POSITION_TEX_COLOR}
		 * consumer, which does not use the lightmap, overlay or normal attributes it would
		 * otherwise compute. Dropping those also drops the per-quad memory stack it unpacks
		 * through, the normal transform, and the three vertex writes that land nowhere.
		 * <p>
		 * Positions go out through a fresh {@link Vector4f} per vertex rather than a shared
		 * scratch, the way {@link BulkDataWriter} does it: measured against the real
		 * {@link com.mojang.blaze3d.vertex.BufferBuilder} that is about twice as fast, because
		 * a shared static has to be written to memory while a local one stays in registers.
		 */
		void tex(VertexConsumer vc, BakedQuad quad) {
			int[] d = quad.getVertices();
			float shade = shade(quad);
			for (int i = 0; i < 4; i++) {
				int o = i * STRIDE;
				var pos = new Vector4f(Float.intBitsToFloat(d[o]), Float.intBitsToFloat(d[o + 1]),
						Float.intBitsToFloat(d[o + 2]), 1).mul(m4);
				vc.addVertex(pos.x(), pos.y(), pos.z())
						.setUv(Float.intBitsToFloat(d[o + 4]), Float.intBitsToFloat(d[o + 5]))
						.setColor(color(d[o + 3], shade));
			}
		}

		/**
		 * Baked color and shade as rgb, with fading on alpha.
		 * <p>
		 * Quads are baked with their color in little-endian abgr order, so red is the low byte;
		 * the result is handed to {@link VertexConsumer#setColor} which wants plain argb.
		 */
		private int color(int baked, float shade) {
			int a = (int) (alpha * (baked >>> 24));
			int r = (int) ((baked & 0xFF) * shade);
			int g = (int) ((baked >>> 8 & 0xFF) * shade);
			int b = (int) ((baked >>> 16 & 0xFF) * shade);
			return a << 24 | r << 16 | g << 8 | b;
		}

	}

}