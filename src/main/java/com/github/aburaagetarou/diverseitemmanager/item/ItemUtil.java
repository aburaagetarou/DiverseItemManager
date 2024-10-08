package com.github.aburaagetarou.diverseitemmanager.item;

import com.github.aburaagetarou.diverseitemmanager.DiverseItemManager;
import com.github.aburaagetarou.diverseitemmanager.ItemMode;
import de.tr7zw.nbtapi.NBTCompound;
import org.bukkit.inventory.ItemStack;

public class ItemUtil {

	/**
	 * モード別のアイテムを取得する
	 * @param item 基のアイテム
	 * @return アイテム
	 */
	public static IDiverseItem<NBTCompound> getItem(ItemStack item) {
		if(DiverseItemManager.getItemMode() == ItemMode.DIVERSE_SKRIPT) {
			return SkriptItem.get(item);
		}
		return null;
	}
}
