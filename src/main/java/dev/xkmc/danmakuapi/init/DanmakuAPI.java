package dev.xkmc.danmakuapi.init;

import com.mojang.logging.LogUtils;
import com.tterrag.registrate.providers.ProviderType;
import dev.xkmc.danmakuapi.content.custom.screen.SpellSetToServer;
import dev.xkmc.danmakuapi.content.virtual.DanmakuToClientPacket;
import dev.xkmc.danmakuapi.content.virtual.EraseDanmakuToClient;
import dev.xkmc.danmakuapi.init.data.*;
import dev.xkmc.danmakuapi.init.registrate.DanmakuEntities;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import dev.xkmc.l2core.init.reg.registrate.L2Registrate;
import dev.xkmc.l2core.init.reg.registrate.SimpleEntry;
import dev.xkmc.l2core.init.reg.simple.Reg;
import dev.xkmc.l2serial.network.PacketHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.slf4j.Logger;

@Mod(DanmakuAPI.MODID)
@EventBusSubscriber(modid = DanmakuAPI.MODID, bus = EventBusSubscriber.Bus.MOD)
public class DanmakuAPI {

	public static final String MODID = "danmaku_api";
	public static final Logger LOGGER = LogUtils.getLogger();
	public static final Reg REG = new Reg(MODID);
	public static final L2Registrate REGISTRATE = new L2Registrate(MODID);
	public static final PacketHandler HANDLER = new PacketHandler(MODID, 1,
			e -> e.create(SpellSetToServer.class, PacketHandler.NetDir.PLAY_TO_SERVER),
			e -> e.create(DanmakuToClientPacket.class, PacketHandler.NetDir.PLAY_TO_CLIENT),
			e -> e.create(EraseDanmakuToClient.class, PacketHandler.NetDir.PLAY_TO_CLIENT)
	);

	public static final SimpleEntry<CreativeModeTab> TAB =
			REGISTRATE.buildModCreativeTab("danmaku", "Spellcards & Danmaku",
					e -> e.icon(() -> DanmakuItems.Bullet.CIRCLE.get(DyeColor.RED).asStack()));

	public DanmakuAPI() {
		DanmakuItems.register();
		DanmakuEntities.register();
		DanmakuConfig.init();
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void gatherData(GatherDataEvent event) {
		REGISTRATE.addDataGenerator(ProviderType.LANG, DanmakuLang::genLang);
		REGISTRATE.addDataGenerator(ProviderType.RECIPE, DanmakuRecipeGen::genRecipes);
		REGISTRATE.addDataGenerator(ProviderType.ITEM_TAGS, DanmakuTagGen::genItemTag);
		new DanmakuDamageTypes(REGISTRATE).generate();
	}

	public static ResourceLocation loc(String id) {
		return ResourceLocation.fromNamespaceAndPath(MODID, id);
	}

}
