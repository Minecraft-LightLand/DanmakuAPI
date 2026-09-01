package dev.xkmc.danmakuapi.content.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.danmakuapi.api.GrazeHelper;
import dev.xkmc.danmakuapi.content.item.DanmakuItem;
import dev.xkmc.danmakuapi.init.data.DanmakuConfig;
import dev.xkmc.fastprojectileapi.entity.SimplifiedProjectile;
import dev.xkmc.fastprojectileapi.render.ProjectileRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class ItemBulletRenderer<T extends ItemBulletEntity> extends EntityRenderer<T> implements ProjectileRenderer<T> {

	public ItemBulletRenderer(EntityRendererProvider.Context pContext) {
		super(pContext);
	}

	protected int getBlockLightLevel(T e, BlockPos pPos) {
		return e.fullBright() ? 15 : super.getBlockLightLevel(e, pPos);
	}

	@Override
	public double fading(SimplifiedProjectile e) {
		double selfFading = DanmakuConfig.CLIENT.selfDanmakuFading.get();
		if (entityRenderDispatcher.camera.getEntity() == e.getOwner()) {
			double dist = entityRenderDispatcher.camera.getPosition().distanceTo(e.position());
			if (e instanceof ItemBulletEntity ibe && ibe.getItem().getItem() instanceof DanmakuItem item)
				selfFading = item.modifyFading(selfFading);
			return Math.min((dist - 2) / 12, 1) * selfFading;
		}
		double fading = DanmakuConfig.CLIENT.farDanmakuFading.get();
		double global = GrazeHelper.globalInvulTime > 0 ? selfFading : 1;
		if (fading == 0) return global;
		double dist = entityRenderDispatcher.camera.getPosition().distanceTo(e.position());
		double start = DanmakuConfig.CLIENT.fadingStart.get();
		double end = DanmakuConfig.CLIENT.fadingEnd.get();
		if (dist < start) return global;
		return (1 - Math.min((dist - start) / (end - start), 1) * fading) * global;
	}

	public boolean shouldRender(T e, Frustum frustum, double camx, double camy, double camz) {
		Entity cam = this.entityRenderDispatcher.camera.getEntity();
		if (e.getOwner() != cam || e.tickCount >= 40) return true;
		double dh = e.getBbHeight() / 2;
		double dist = cam.getEyePosition().distanceToSqr(e.position().add(0, dh, 0));
		double dy = Math.abs(cam.getEyeY() - e.getY() - dh);
		return dist > 12 || dy > 0.1 + dh * 2 && dist > 4;
	}

	@Override
	public Quaternionf cameraOrientation() {
		return entityRenderDispatcher.cameraOrientation();
	}

	public void render(T e, float yaw, float pTick, PoseStack pose, MultiBufferSource buffer, int light) {
		render(e, pTick, pose);
	}

	@Override
	public Vec3 getRenderOffset(T e, float f) {
		return new Vec3(0, e.getBbHeight() / 2, 0);
	}

	@Override
	public void render(T e, float pTick, PoseStack pose) {
		if (!(e.getItem().getItem() instanceof DanmakuItem danmaku)) return;
		pose.pushPose();
		float scale = e.scale();
		pose.scale(scale, scale, scale);
		danmaku.getTypeForRender().create(this, e, pose, pTick);
		pose.popPose();
	}

	public ResourceLocation getTextureLocation(T pEntity) {
		return TextureAtlas.LOCATION_BLOCKS;
	}

}