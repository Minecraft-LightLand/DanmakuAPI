package dev.xkmc.danmakuapi.init;

import com.mojang.logging.LogUtils;
import com.tterrag.registrate.providers.ProviderType;
import dev.xkmc.danmakuapi.content.custom.screen.SpellSetToServer;
import dev.xkmc.danmakuapi.content.entity.DanmakuFriendlyToClient;
import dev.xkmc.danmakuapi.content.virtual.DanmakuToClientPacket;
import dev.xkmc.danmakuapi.content.virtual.EraseDanmakuToClient;
import dev.xkmc.danmakuapi.init.data.*;
import dev.xkmc.danmakuapi.init.registrate.DanmakuEntities;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.l2library.base.L2Registrate;
import dev.xkmc.l2library.serial.config.PacketHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import org.slf4j.Logger;

@Mod(DanmakuAPI.MODID)
@Mod.EventBusSubscriber(modid = DanmakuAPI.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DanmakuAPI {

	public static final String MODID = "danmaku_api";
	public static final Logger LOGGER = LogUtils.getLogger();
	public static final L2Registrate REGISTRATE = new L2Registrate(MODID);
	public static final PacketHandler HANDLER = new PacketHandler(new ResourceLocation(MODID, "main"), 1,
			e -> e.create(SpellSetToServer.class, NetworkDirection.PLAY_TO_SERVER),
			e -> e.create(DanmakuToClientPacket.class, NetworkDirection.PLAY_TO_CLIENT),
			e -> e.create(EraseDanmakuToClient.class, NetworkDirection.PLAY_TO_CLIENT),
			e -> e.create(DanmakuFriendlyToClient.class, NetworkDirection.PLAY_TO_CLIENT)
	);

	public DanmakuAPI() {
		DanmakuItems.register();
		DanmakuEntities.register();
		DanmakuConfig.init();
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void gatherData(GatherDataEvent event) {
		REGISTRATE.addDataGenerator(ProviderType.LANG, DanmakuLang::genLang);
		REGISTRATE.addDataGenerator(ProviderType.ITEM_TAGS, DanmakuTagGen::genItemTag);
		new DanmakuDamageTypes(event.getGenerator().getPackOutput(),
				event.getLookupProvider(), event.getExistingFileHelper()).generate(true, event.getGenerator());
	}

	public static ResourceLocation loc(String id) {
		return new ResourceLocation(MODID, id);
	}

}
