package dev.xkmc.danmakuapi.init.registrate;

import com.mojang.serialization.MapCodec;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.xkmc.danmakuapi.api.DanmakuBullet;
import dev.xkmc.danmakuapi.api.DanmakuLaser;
import dev.xkmc.danmakuapi.content.custom.data.HomingSpellFormData;
import dev.xkmc.danmakuapi.content.custom.data.RingSpellFormData;
import dev.xkmc.danmakuapi.content.custom.data.SpellDataHolder;
import dev.xkmc.danmakuapi.content.item.CustomSpellItem;
import dev.xkmc.danmakuapi.content.item.DanmakuItem;
import dev.xkmc.danmakuapi.content.item.LaserItem;
import dev.xkmc.danmakuapi.content.particle.DanmakuPoofParticleOptions;
import dev.xkmc.danmakuapi.content.render.DisplayType;
import dev.xkmc.danmakuapi.init.DanmakuAPI;
import dev.xkmc.danmakuapi.init.data.DanmakuTagGen;
import dev.xkmc.l2core.init.reg.simple.DCReg;
import dev.xkmc.l2core.init.reg.simple.DCVal;
import dev.xkmc.l2core.init.reg.simple.Val;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.Locale;

public class DanmakuItems {

	public enum Bullet implements DanmakuBullet {
		CIRCLE(1, 4, DisplayType.SOLID),
		BALL(1, 4, DisplayType.SOLID),
		MENTOS(2, 6, DisplayType.SOLID),
		BUBBLE(4, 8, DisplayType.ADDITIVE),
		BUTTERFLY(1, 4, DisplayType.TRANSPARENT),
		SPARK(1, 4, DisplayType.SOLID),
		STAR(2, 6, DisplayType.TRANSPARENT),
		CARD(1, 4, DisplayType.SOLID),
		DAGGER(1, 6, DisplayType.SOLID),
		;

		public final String name;
		public final TagKey<Item> tag;
		public final float size;
		private final int damage;
		private final DisplayType display;

		Bullet(float size, int damage, DisplayType display) {
			this.size = size;
			this.damage = damage;
			this.display = display;
			name = name().toLowerCase(Locale.ROOT);
			tag = DanmakuTagGen.item("danmaku/" + name);
		}

		public ItemEntry<DanmakuItem> get(DyeColor color) {
			return DanmakuItems.DANMAKU[ordinal()][color.ordinal()];
		}

		public int damage() {
			return damage;
		}

		public boolean bypass() {
			return size > 1;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public DisplayType display() {
			return display;
		}

	}

	public enum Laser implements DanmakuLaser {
		LASER(1, 1, 4), PENCIL(1, 1.75f, 4);

		public final String name;
		public final TagKey<Item> tag;
		public final float size, visualLength;
		private final int damage;

		Laser(float size, float visualLength, int damage) {
			this.size = size;
			this.visualLength = visualLength;
			this.damage = damage;
			name = name().toLowerCase(Locale.ROOT);
			tag = DanmakuTagGen.item("laser/" + name);
		}

		public ItemEntry<LaserItem> get(DyeColor color) {
			return DanmakuItems.LASER[ordinal()][color.ordinal()];
		}

		public int damage() {
			return damage;
		}

		public boolean setupLength() {
			return this != LASER;
		}

		public float visualLength() {
			return visualLength;
		}

	}

	private static final ItemEntry<DanmakuItem>[][] DANMAKU;
	private static final ItemEntry<LaserItem>[][] LASER;

	public static final ItemEntry<CustomSpellItem> CUSTOM_SPELL_RING, CUSTOM_SPELL_HOMING;

	public static final Val<ParticleType<DanmakuPoofParticleOptions>> POOF;

	private static final DCReg DC = DCReg.of(DanmakuAPI.REG);
	public static final DCVal<SpellDataHolder> SPELL_DATA = DC.reg("spell_data", SpellDataHolder.class, true);

	static {

		CUSTOM_SPELL_RING = DanmakuAPI.REGISTRATE
				.item("custom_spell_ring", p -> new CustomSpellItem(p.stacksTo(1), false, RingSpellFormData.FLOWER))
				.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/spell/custom_spell")))
				.tag(DanmakuTagGen.CUSTOM_SPELL)
				.register();

		CUSTOM_SPELL_HOMING = DanmakuAPI.REGISTRATE
				.item("custom_spell_homing", p -> new CustomSpellItem(p.stacksTo(1), true, HomingSpellFormData.RING))
				.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/spell/custom_spell")))
				.tag(DanmakuTagGen.CUSTOM_SPELL)
				.register();

		DANMAKU = new ItemEntry[Bullet.values().length][DyeColor.values().length];
		for (var t : Bullet.values()) {
			for (var e : DyeColor.values()) {
				var ent = DanmakuAPI.REGISTRATE
						.item(e.getName() + "_" + t.name + "_danmaku", p -> new DanmakuItem(p.rarity(Rarity.RARE), t, e, t.size))
						.model((ctx, pvd) -> pvd.generated(ctx, pvd.modLoc("item/bullet/" + t.name + "/" + e.getName())))
						.tag(t.tag)
						.register();
				DANMAKU[t.ordinal()][e.ordinal()] = ent;
			}
		}

		LASER = new ItemEntry[Laser.values().length][DyeColor.values().length];
		for (var t : Laser.values()) {
			for (var e : DyeColor.values()) {
				var ent = DanmakuAPI.REGISTRATE
						.item(e.getName() + "_" + t.name, p -> new LaserItem(p.rarity(Rarity.RARE), t, e, 1))
						.model((ctx, pvd) -> pvd.generated(ctx,
								pvd.modLoc("item/laser/" + t.name),
								pvd.modLoc("item/laser/" + t.name + "_overlay")))
						.color(() -> () -> (stack, i) -> ((LaserItem) stack.getItem()).getDanmakuColor(stack, i))
						.tag(t.tag)
						.register();
				LASER[t.ordinal()][e.ordinal()] = ent;
			}
		}

		POOF = new Val.Registrate<>(DanmakuAPI.REGISTRATE.simple("danmaku_poof", Registries.PARTICLE_TYPE,
				() -> new ParticleType<>(false) {
					@Override
					public MapCodec<DanmakuPoofParticleOptions> codec() {
						return DanmakuPoofParticleOptions.CODEC;
					}

					@Override
					public StreamCodec<? super RegistryFriendlyByteBuf, DanmakuPoofParticleOptions> streamCodec() {
						return DanmakuPoofParticleOptions.STREAM_CODEC;
					}
				}));
	}

	public static void register() {
	}

}
