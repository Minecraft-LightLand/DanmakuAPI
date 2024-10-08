package dev.xkmc.danmakuapi.presets.touhoulittlemaid;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.xkmc.danmakuapi.presets.youkai.GeneralYoukaiEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.fml.ModList;

public class TLMClientCompat {

	private static final boolean LOADED = ModList.get().isLoaded(TouhouLittleMaid.MOD_ID);

	public static boolean delegateRender(GeneralYoukaiEntity e, float yaw, float pTick, PoseStack pose, MultiBufferSource buffer, int light) {
		if (!LOADED || e.getModelId() == null) return false;
		return TLMRenderHandler.render(e, yaw, pTick, pose, buffer, light);
	}

}
