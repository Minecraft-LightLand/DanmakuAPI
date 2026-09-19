package dev.xkmc.danmakuapi.content.virtual;

import dev.xkmc.danmakuapi.init.DanmakuAPI;
import dev.xkmc.fastprojectileapi.entity.SimplifiedProjectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;

import java.util.List;

public class DanmakuManager {

	public static void send(LivingEntity user, List<SimplifiedProjectile> proj) {
		if (proj.isEmpty()) return;
		if (user.level() instanceof ServerLevel sl) {
			// each tracking player gets its own hurt-bitset, so friendly
			// danmaku can render as self danmaku on their client
			for (ServerPlayer sp : sl.getChunkSource().chunkMap.getPlayers(new ChunkPos(user.blockPosition()), false)) {
				DanmakuAPI.HANDLER.toClientPlayer(DanmakuToClientPacket.of(user.registryAccess(), proj, sp), sp);
			}
			return;
		}
		DanmakuAPI.HANDLER.toTrackingPlayers(DanmakuToClientPacket.of(user.registryAccess(), proj), user);
	}

	public static void erase(LivingEntity user, SimplifiedProjectile proj, boolean kill) {
		DanmakuAPI.HANDLER.toTrackingPlayers(EraseDanmakuToClient.of(proj, kill), user);
	}

}
