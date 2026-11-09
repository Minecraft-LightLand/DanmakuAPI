package dev.xkmc.danmakuapi.init.data;

import com.tterrag.registrate.providers.RegistrateItemTagsProvider;
import dev.xkmc.danmakuapi.init.DanmakuAPI;
import dev.xkmc.danmakuapi.init.registrate.DanmakuItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class DanmakuTagGen {

	public static final TagKey<Item> CUSTOM_SPELL = item("custom_spell");
	public static final TagKey<Item> PRESET_SPELL = item("preset_spell");
	public static final TagKey<Item> DANMAKU = item("danmaku");
	public static final TagKey<Item> LASER = item("laser");
	public static final TagKey<Item> DANMAKU_SHOOTER = item("danmaku_shooter");

	public static TagKey<Item> item(String id) {
		return ItemTags.create(DanmakuAPI.loc(id));
	}

	public static void genItemTag(RegistrateItemTagsProvider pvd) {
		var danmaku = pvd.addTag(DANMAKU);
		for (var e : DanmakuItems.Bullet.values()) {
			danmaku.addTag(e.tag);
		}
		var laser = pvd.addTag(LASER);
		for (var e : DanmakuItems.Laser.values()) {
			laser.addTag(e.tag);
		}
		pvd.addTag(PRESET_SPELL);
		pvd.addTag(DANMAKU_SHOOTER).addTags(DANMAKU, LASER, CUSTOM_SPELL, PRESET_SPELL);
		pvd.addTag(ItemTags.create(new ResourceLocation("c", "hidden_from_recipe_viewers")))
				.addTags(DANMAKU, LASER);
	}

}
