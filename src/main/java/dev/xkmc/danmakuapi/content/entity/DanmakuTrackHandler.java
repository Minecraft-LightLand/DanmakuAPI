package dev.xkmc.danmakuapi.content.entity;

import dev.xkmc.danmakuapi.api.IDanmakuEntity;
import dev.xkmc.danmakuapi.init.DanmakuAPI;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = DanmakuAPI.MODID, bus = EventBusSubscriber.Bus.FORGE)
public class DanmakuTrackHandler {

	@SubscribeEvent
	public static void onStartTracking(PlayerEvent.StartTracking event) {
		if (!(event.getTarget() instanceof IDanmakuEntity dan)) return;
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		boolean friendly = !IDanmakuEntity.canHurt(dan.self().getOwner(), player);
		DanmakuAPI.HANDLER.toClientPlayer(new DanmakuFriendlyToClient(dan.self().getId(), friendly), player);
	}

}
