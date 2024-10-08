package dev.xkmc.danmakuapi.content.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xkmc.danmakuapi.init.data.DanmakuConfig;
import dev.xkmc.fastprojectileapi.entity.SimplifiedProjectile;
import dev.xkmc.fastprojectileapi.render.ProjectileRenderHelper;
import dev.xkmc.fastprojectileapi.render.ProjectileRenderer;
import dev.xkmc.fastprojectileapi.render.RenderableProjectileType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

public record DoubleLayerLaserType(ResourceLocation inner, ResourceLocation outer, int color)
		implements RenderableProjectileType<DoubleLayerLaserType, DoubleLayerLaserType.Ins> {

	@Override
	public void start(MultiBufferSource buffer, Iterable<Ins> list) {
		boolean ADDITIVE = DanmakuConfig.CLIENT.laserRenderAdditive.get();
		boolean INVERT = DanmakuConfig.CLIENT.laserRenderInverted.get();
		double tran = DanmakuConfig.CLIENT.laserTransparency.get();
		int col = (color & 0xffffff) | (((int) (tran * 255.9)) << 24);
		int add = (int) ((color & 0xff) * tran) |
				(int) ((color >> 8 & 0xff) * tran) << 8 |
				(int) ((color >> 16 & 0xff) * tran) << 16 | 0xff000000;
		VertexConsumer vc;
		vc = buffer.getBuffer(DanmakuRenderStates.laser(inner));
		for (var e : list) {
			e.texInner(vc, -1);
		}
		if (ADDITIVE) {
			vc = buffer.getBuffer(DanmakuRenderStates.additive(outer));
			for (var e : list) {
				e.texOuter(false, vc, add);
			}
		}
		if (INVERT) {
			vc = buffer.getBuffer(DanmakuRenderStates.transparent(outer));
			for (var e : list) {
				e.texOuter(true, vc, col);
			}
		}
		if (!ADDITIVE && !INVERT) {
			vc = buffer.getBuffer(DanmakuRenderStates.transparent(outer));
			for (var e : list) {
				e.texOuter(false, vc, col);
			}
		}
	}

	@Override
	public void create(ProjectileRenderer r, SimplifiedProjectile e, PoseStack pose, float pTick) {
		PoseStack.Pose mat = pose.last();
		ProjectileRenderHelper.add(this, new Ins(mat));
	}

	public record Ins(PoseStack.Pose pose) {

		public void texInner(VertexConsumer vc, int color) {
			float v0 = -0.167f, v1 = 0.167f;
			renderPart(false, vc, color, 0, 1,
					v0, v0, v0, v1, v1, v0, v1, v1,
					0, 1, 0, 1);
		}

		public void texOuter(boolean invert, VertexConsumer vc, int color) {
			float v0 = -0.5f, v1 = 0.5f;
			renderPart(invert, vc, color, 0, 1,
					v0, v0, v0, v1, v1, v0, v1, v1,
					0, 1, 0, 1);
		}

		private void renderPart(boolean invert, VertexConsumer vc, int color, int pMinY, int pMaxY, float pX0, float pZ0, float pX1, float pZ1, float pX2, float pZ2, float pX3, float pZ3, float pMinU, float pMaxU, float pMinV, float pMaxV) {
			renderQuad(invert, vc, color, pMinY, pMaxY, pX0, pZ0, pX1, pZ1, pMinU, pMaxU, pMinV, pMaxV);
			renderQuad(invert, vc, color, pMinY, pMaxY, pX3, pZ3, pX2, pZ2, pMinU, pMaxU, pMinV, pMaxV);
			renderQuad(invert, vc, color, pMinY, pMaxY, pX1, pZ1, pX3, pZ3, pMinU, pMaxU, pMinV, pMaxV);
			renderQuad(invert, vc, color, pMinY, pMaxY, pX2, pZ2, pX0, pZ0, pMinU, pMaxU, pMinV, pMaxV);
		}

		private void renderQuad(boolean invert, VertexConsumer pConsumer, int color, int pMinY, int pMaxY, float pMinX, float pMinZ, float pMaxX, float pMaxZ, float pMinU, float pMaxU, float pMinV, float pMaxV) {
			if (invert) {
				addVertex(pConsumer, color, pMaxY, pMaxX, pMaxZ, pMinU, pMinV);
				addVertex(pConsumer, color, pMinY, pMaxX, pMaxZ, pMinU, pMaxV);
				addVertex(pConsumer, color, pMinY, pMinX, pMinZ, pMaxU, pMaxV);
				addVertex(pConsumer, color, pMaxY, pMinX, pMinZ, pMaxU, pMinV);
			} else {
				addVertex(pConsumer, color, pMaxY, pMinX, pMinZ, pMaxU, pMinV);
				addVertex(pConsumer, color, pMinY, pMinX, pMinZ, pMaxU, pMaxV);
				addVertex(pConsumer, color, pMinY, pMaxX, pMaxZ, pMinU, pMaxV);
				addVertex(pConsumer, color, pMaxY, pMaxX, pMaxZ, pMinU, pMinV);

			}
		}

		private void addVertex(VertexConsumer vc, int color, int pY, float pX, float pZ, float pU, float pV) {
			vc.addVertex(pose, pX, pY, pZ)
					.setUv(pU, pV)
					.setColor(color);
		}

	}
}
