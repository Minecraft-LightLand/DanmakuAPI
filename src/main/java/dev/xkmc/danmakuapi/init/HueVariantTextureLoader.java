package dev.xkmc.danmakuapi.init;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Generates the chromatic {@code entity/bullet/bubble} textures from the red one at load time.
 * <p>
 * The bubble art is a radial glow whose bright dyes are the same image with the hue rotated: for
 * blue, light blue, lime, magenta, orange, pink, purple and yellow the result matches the shipped
 * texture to within 1/255 in RGB. The alpha is not simply copied, though: the source art is the
 * red alpha composited {@code layers} times over itself, which only matters on the anti-aliased
 * rim (the rest of the image is fully opaque or fully transparent). Both are reproduced here, so
 * only {@code red.png} and the seven dyes that are not hue rotations (brown, cyan, green, black,
 * gray, light gray, white) need to ship.
 * <p>
 * The textures are standalone: the danmaku render types bind them directly through
 * {@code TextureStateShard}, so this registers {@link DynamicTexture}s rather than atlas sprites.
 */
public class HueVariantTextureLoader extends SimplePreparableReloadListener<@Nullable NativeImage> {

	private final ResourceLocation prefix;
	private final String base;

	public HueVariantTextureLoader(ResourceLocation prefix, String base, Variant... variants) {
		this.prefix = prefix;
		this.base = base;
		this.variants = variants;
	}

	/**
	 * @param hue    rotation from red, in degrees
	 * @param layers how many times the base alpha was composited onto itself
	 */
	public record Variant(String name, double hue, int layers) {

	}

	private final Variant[] variants;

	/**
	 * Rotates the hue of every pixel and replays the alpha compositing of the source art.
	 */
	private static NativeImage rotate(NativeImage base, Variant variant) {
		int width = base.getWidth();
		int height = base.getHeight();
		NativeImage out = new NativeImage(width, height, false);
		double shift = variant.hue() / 360.0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int abgr = base.getPixelRGBA(x, y);
				int alpha = abgr >>> 24;
				int b = (abgr >>> 16) & 0xFF;
				int g = (abgr >>> 8) & 0xFF;
				int r = abgr & 0xFF;
				int rgb = rotateHue(r, g, b, shift);
				out.setPixelRGBA(x, y, (compositeAlpha(alpha, variant.layers()) << 24) | rgb);
			}
		}
		return out;
	}

	/**
	 * @return the rotated colour packed as the ABGR int {@link NativeImage#setPixelRGBA} wants,
	 * minus the alpha byte
	 */
	private static int rotateHue(int r, int g, int b, double shift) {
		double rf = r / 255.0;
		double gf = g / 255.0;
		double bf = b / 255.0;
		double max = Math.max(rf, Math.max(gf, bf));
		double min = Math.min(rf, Math.min(gf, bf));
		double sat = max == 0 ? 0 : (max - min) / max;
		double hue;
		if (max == min) {
			hue = 0;
		} else if (max == rf) {
			hue = (gf - bf) / (max - min) / 6.0;
		} else if (max == gf) {
			hue = (2.0 + (bf - rf) / (max - min)) / 6.0;
		} else {
			hue = (4.0 + (rf - gf) / (max - min)) / 6.0;
		}
		hue += shift;
		hue -= Math.floor(hue);

		double sector = Math.floor(hue * 6.0);
		double f = hue * 6.0 - sector;
		double p = max * (1.0 - sat);
		double q = max * (1.0 - f * sat);
		double t = max * (1.0 - (1.0 - f) * sat);
		double nr, ng, nb;
		switch ((int) sector % 6) {
			case 0 -> {
				nr = max;
				ng = t;
				nb = p;
			}
			case 1 -> {
				nr = q;
				ng = max;
				nb = p;
			}
			case 2 -> {
				nr = p;
				ng = max;
				nb = t;
			}
			case 3 -> {
				nr = p;
				ng = q;
				nb = max;
			}
			case 4 -> {
				nr = t;
				ng = p;
				nb = max;
			}
			default -> {
				nr = max;
				ng = p;
				nb = q;
			}
		}
		return (clamp(nb) << 16) | (clamp(ng) << 8) | clamp(nr);
	}

	private static int clamp(double value) {
		return (int) Math.round(Math.clamp(value, 0.0, 1.0) * 255.0);
	}

	/**
	 * Source-over compositing of the base alpha onto itself {@code layers} times, rounded after
	 * every pass. This is what the source art did, so it is what makes the rim pixel-exact.
	 */
	private static int compositeAlpha(int alpha, int layers) {
		int out = alpha;
		for (int i = 1; i < layers; i++) {
			out = (int) Math.round(out + alpha * (255.0 - out) / 255.0);
		}
		return out;
	}

	private ResourceLocation wrapLoc(String id) {
		return prefix.withSuffix(id + ".png");
	}

	@Override
	@Nullable
	protected NativeImage prepare(ResourceManager manager, ProfilerFiller profiler) {
		var baseLoc = wrapLoc(base);
		Optional<Resource> res = manager.getResource(baseLoc);
		if (res.isEmpty()) {
			DanmakuAPI.LOGGER.error("Missing bubble base texture {}, cannot generate dyed variants", baseLoc);
			return null;
		}
		try (InputStream in = res.get().open()) {
			return NativeImage.read(in);
		} catch (IOException e) {
			DanmakuAPI.LOGGER.error("Failed to read bubble base texture {}", baseLoc, e);
			return null;
		}
	}

	@Override
	protected void apply(@Nullable NativeImage base, ResourceManager manager, ProfilerFiller profiler) {
		if (base == null) return;
		try (base) {
			var textures = Minecraft.getInstance().getTextureManager();
			for (Variant variant : variants) {
				textures.register(wrapLoc(variant.name()), new DynamicTexture(rotate(base, variant)));
			}
		}
	}


}
