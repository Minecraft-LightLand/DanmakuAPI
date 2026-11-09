package dev.xkmc.danmakuapi.content.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DustParticleBase;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.LightTexture;

public class DanmakuPoofParticle extends DustParticleBase<DanmakuPoofParticleOptions> {

	protected DanmakuPoofParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, DanmakuPoofParticleOptions options, SpriteSet sprites) {
		super(level, x, y, z, xSpeed, ySpeed, zSpeed, options, sprites);
		hasPhysics = false;
	}

	public int getLightColor(float pTick) {
		return LightTexture.FULL_BRIGHT;
	}

	public static class Provider implements ParticleProvider<DanmakuPoofParticleOptions> {
		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		public Particle createParticle(DanmakuPoofParticleOptions type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			return new DanmakuPoofParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, type, this.sprites);
		}
	}

}
