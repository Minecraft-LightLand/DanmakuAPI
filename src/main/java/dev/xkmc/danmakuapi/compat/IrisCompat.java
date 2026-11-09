package dev.xkmc.danmakuapi.compat;

import net.minecraftforge.fml.ModList;

/**
 * Soft dependency on Iris. Iris is only referenced from a nested holder so that the class is
 * not loaded at all when Iris is absent.
 */
public final class IrisCompat {

	private static final boolean LOADED = ModList.get().isLoaded("iris");

	private IrisCompat() {
	}

	/**
	 * Whether the current render pass is Iris's shadow-map pass. Iris sets this flag around its
	 * entity rendering and clears it before the normal pass starts.
	 */
	public static boolean isShadowPass() {
		return LOADED && ShadowRendererHolder.isActive();
	}

	private static final class ShadowRendererHolder {

		private static boolean isActive() {
			try {
				var cls = Class.forName("net.irisshaders.iris.shadows.ShadowRenderer");
				return cls.getField("ACTIVE").getBoolean(null);
			} catch (Throwable e) {
				return false;
			}
		}

	}

}
