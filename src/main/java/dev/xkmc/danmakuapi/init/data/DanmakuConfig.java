package dev.xkmc.danmakuapi.init.data;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

public class DanmakuConfig {

	public static class Client {

		public final ForgeConfigSpec.BooleanValue laserRenderAdditive;
		public final ForgeConfigSpec.BooleanValue laserRenderInverted;
		public final ForgeConfigSpec.DoubleValue laserTransparency;
		public final ForgeConfigSpec.DoubleValue farDanmakuFading;
		public final ForgeConfigSpec.DoubleValue selfDanmakuFading;
		public final ForgeConfigSpec.DoubleValue fadingStart;
		public final ForgeConfigSpec.DoubleValue fadingEnd;
		public final ForgeConfigSpec.IntValue powerInfoXAnchor;
		public final ForgeConfigSpec.IntValue powerInfoXOffset;
		public final ForgeConfigSpec.IntValue powerInfoYAnchor;
		public final ForgeConfigSpec.IntValue powerInfoYOffset;


		Client(ForgeConfigSpec.Builder builder) {
			laserRenderAdditive = builder.comment("Additive laser layer").define("laserRenderAdditive", true);
			laserRenderInverted = builder.comment("Inverted layer layer").define("laserRenderInverted", true);
			laserTransparency = builder.comment("Laser outer layer transparency").defineInRange("laserTransparency", 0.5, 0, 1);
			farDanmakuFading = builder.defineInRange("farDanmakuFading", 0.5d, 0, 1);
			selfDanmakuFading = builder.defineInRange("selfDanmakuFading", 0.5d, 0, 1);
			fadingStart = builder.defineInRange("fadingStart", 8d, 0, 128);
			fadingEnd = builder.defineInRange("fadingEnd", 64d, 0, 128);
			powerInfoXAnchor = builder.defineInRange("powerInfoXAnchor", 1, -1, 1);
			powerInfoXOffset = builder.defineInRange("powerInfoXOffset", -8, -1000, 1000);
			powerInfoYAnchor = builder.defineInRange("powerInfoYAnchor", 0, -1, 1);
			powerInfoYOffset = builder.defineInRange("powerInfoYOffset", 0, -1000, 1000);
		}

	}

	public static class Server {

		public final ForgeConfigSpec.IntValue playerDanmakuCooldown;
		public final ForgeConfigSpec.IntValue playerLaserCooldown;
		public final ForgeConfigSpec.IntValue playerSpellCooldown;
		public final ForgeConfigSpec.IntValue playerLaserDuration;

		public final ForgeConfigSpec.IntValue customSpellMaxDuration;
		public final ForgeConfigSpec.IntValue ringSpellDanmakuPerItemCost;
		public final ForgeConfigSpec.IntValue homingSpellDanmakuPerItemCost;

		Server(ForgeConfigSpec.Builder builder) {
			builder.push("danmaku_battle");
			playerDanmakuCooldown = builder.comment("Player item cooldown for using danmaku")
					.defineInRange("playerDanmakuCooldown", 20, 5, 1000);
			playerLaserCooldown = builder.comment("Player item cooldown for using laser")
					.defineInRange("playerLaserCooldown", 80, 5, 1000);
			playerSpellCooldown = builder.comment("Player item cooldown for using spellcard")
					.defineInRange("playerSpellCooldown", 40, 5, 1000);
			playerLaserDuration = builder.comment("Player laser duration")
					.defineInRange("playerLaserDuration", 100, 5, 1000);
			builder.pop();

			builder.push("custom_spell");
			customSpellMaxDuration = builder.comment("Max duration of custom spell allowed")
					.defineInRange("customSpellMaxDuration", 60, 1, 1000);
			ringSpellDanmakuPerItemCost = builder.comment("Ring Spell: Max number of bullet allowed per item cost")
					.defineInRange("ringSpellDanmakuPerItemCost", 32, 1, 1024);
			homingSpellDanmakuPerItemCost = builder.comment("Homing Spell: Max number of bullet allowed per item cost")
					.defineInRange("homingSpellDanmakuPerItemCost", 8, 1, 1024);
			builder.pop();
		}

	}

	public static final ForgeConfigSpec CLIENT_SPEC;
	public static final Client CLIENT;

	public static final ForgeConfigSpec SERVER_SPEC;
	public static final Server SERVER;

	static {
		final Pair<Client, ForgeConfigSpec> client = new ForgeConfigSpec.Builder().configure(Client::new);
		CLIENT_SPEC = client.getRight();
		CLIENT = client.getLeft();

		final Pair<Server, ForgeConfigSpec> server = new ForgeConfigSpec.Builder().configure(Server::new);
		SERVER_SPEC = server.getRight();
		SERVER = server.getLeft();
	}

	public static void init() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SERVER_SPEC);
	}

}
