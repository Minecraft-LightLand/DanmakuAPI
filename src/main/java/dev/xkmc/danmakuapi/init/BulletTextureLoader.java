package dev.xkmc.danmakuapi.init;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Builds the per-dye entity textures from the packed index and palette images, so the jar
 * ships one raster plus one palette per shape instead of sixteen finished PNGs.
 * <p>
 * {@code textures/bullet_index/<shape>.png} stores the class index of every pixel as a grey
 * level (palette entry {@code i} is {@code (i, i, i)}), and
 * {@code textures/bullet_palette/<shape>.png} is {@code classCount} columns by sixteen rows of
 * RGBA, one row per {@link DyeColor} ordinal. The rows carry alpha because several shapes fade
 * their edges and the alpha differs between dyes.
 * <p>
 * bubble is deliberately not packed: its glow has far too many distinct colours to share one
 * palette. Its textures stay as ordinary files.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = DanmakuAPI.MODID, bus = EventBusSubscriber.Bus.MOD)
public class BulletTextureLoader implements PreparableReloadListener {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Shapes whose entity art is packed. dagger is absent because that danmaku is drawn from
	 * its item model, and bubble because it cannot share a palette.
	 */
	private static final String[] SHAPES = {"ball", "butterfly", "card", "circle", "mentos", "spark", "star"};

	private record Built(ResourceLocation location, NativeImage image) {
	}

	@SubscribeEvent
	public static void register(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener(new BulletTextureLoader());
	}

	@Override
	public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager,
										  ProfilerFiller prepareProfiler, ProfilerFiller reloadProfiler,
										  Executor background, Executor game) {
		return CompletableFuture
				.supplyAsync(() -> build(manager), background)
				.thenCompose(barrier::wait)
				.thenAcceptAsync(BulletTextureLoader::upload, game);
	}

	private static List<Built> build(ResourceManager manager) {
		List<Built> list = new ArrayList<>();
		for (String shape : SHAPES) {
			NativeImage index = read(manager, "textures/bullet_index/" + shape + ".png");
			NativeImage palette = read(manager, "textures/bullet_palette/" + shape + ".png");
			if (index == null || palette == null) {
				if (index != null) index.close();
				if (palette != null) palette.close();
				continue;
			}
			try {
				buildShape(shape, index, palette, list);
			} finally {
				index.close();
				palette.close();
			}
		}
		return list;
	}

	private static void buildShape(String shape, NativeImage index, NativeImage palette, List<Built> list) {
		int width = index.getWidth();
		int height = index.getHeight();
		int classes = palette.getWidth();
		if (palette.getHeight() < DyeColor.values().length) {
			LOGGER.warn("Palette for {} has {} rows, expected {}", shape, palette.getHeight(), DyeColor.values().length);
			return;
		}
		for (DyeColor dye : DyeColor.values()) {
			int row = dye.ordinal();
			NativeImage image = new NativeImage(width, height, false);
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int cls = index.getPixelRGBA(x, y) & 0xFF;
					image.setPixelRGBA(x, y, cls < classes ? palette.getPixelRGBA(cls, row) : 0);
				}
			}
			list.add(new Built(ResourceLocation.fromNamespaceAndPath(DanmakuAPI.MODID,
					"textures/entity/bullet/" + shape + "/" + dye.getName() + ".png"), image));
		}
	}

	private static NativeImage read(ResourceManager manager, String path) {
		ResourceLocation location = ResourceLocation.fromNamespaceAndPath(DanmakuAPI.MODID, path);
		Optional<Resource> resource = manager.getResource(location);
		if (resource.isEmpty()) {
			LOGGER.warn("Missing packed danmaku texture {}", location);
			return null;
		}
		try (InputStream in = resource.get().open()) {
			return NativeImage.read(in);
		} catch (IOException e) {
			LOGGER.error("Failed to read packed danmaku texture {}", location, e);
			return null;
		}
	}

	private static void upload(List<Built> built) {
		TextureManager manager = Minecraft.getInstance().getTextureManager();
		for (Built entry : built) {
			manager.register(entry.location(), new DynamicTexture(entry.image()));
		}
	}

}
