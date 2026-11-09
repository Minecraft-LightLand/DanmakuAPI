package dev.xkmc.danmakuapi.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class DanmakuUseEvent extends Event {

	private final Player player;
	private final ItemStack stack;
	private int cooldown;
	private boolean consume;

	public DanmakuUseEvent(Player player, ItemStack stack, int cooldown) {
		this.player = player;
		this.stack = stack;
		this.cooldown = cooldown;
		consume = !player.getAbilities().instabuild;
	}

	public ItemStack getStack() {
		return stack;
	}

	public Player getPlayer() {
		return player;
	}

	public int getCooldown() {
		return cooldown;
	}

	public void setCooldown(int cooldown) {
		this.cooldown = cooldown;
	}

	public boolean consume() {
		return consume;
	}

	public void setConsume(boolean consume) {
		this.consume = consume;
	}

}
