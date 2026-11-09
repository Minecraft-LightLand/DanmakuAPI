package dev.xkmc.danmakuapi.api;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import dev.xkmc.danmakuapi.content.item.LaserItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

public interface DanmakuLaser {

	int damage();

	ItemProviderEntry<? extends LaserItem> get(DyeColor color);

	default boolean setupLength() {
		return false;
	}

	default float visualLength() {
		return 1;
	}

}
