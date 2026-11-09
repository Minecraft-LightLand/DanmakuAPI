package dev.xkmc.danmakuapi.content.custom.forms;

import dev.xkmc.danmakuapi.content.spell.item.ItemSpell;
import dev.xkmc.l2serial.serialization.SerialClass;

@SerialClass
public abstract class ISpellForm<D> extends ItemSpell {

	public abstract void init(D data);

}
