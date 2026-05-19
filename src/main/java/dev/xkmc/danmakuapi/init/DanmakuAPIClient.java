package dev.xkmc.danmakuapi.init;

import dev.xkmc.danmakuapi.content.virtual.ClientDanmakuCache;
import dev.xkmc.fastprojectileapi.render.ProjectileRenderHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = DanmakuAPI.MODID, bus = EventBusSubscriber.Bus.MOD)
public class DanmakuAPIClient {

	@SubscribeEvent
	public static void initClient(FMLClientSetupEvent event) {
		ProjectileRenderHelper.LIST.add(ClientDanmakuCache::get);
	}

}
