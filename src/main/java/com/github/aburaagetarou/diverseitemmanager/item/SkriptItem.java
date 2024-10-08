package com.github.aburaagetarou.diverseitemmanager.item;

import com.github.aburaagetarou.diverseitemmanager.util.ItemPersistentUtils;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Skriptで生成されたアイテム
 */
public class SkriptItem extends DiverseItemImpl{

	/**
	 * アイテムを作成
	 * @param item 基となるアイテム
	 * @return アイテム
	 */
	public static SkriptItem get(ItemStack item) {

		// チェック
		if(item == null || item.getType() == Material.AIR) return null;

		ItemId itemId;
		try {

			// NBTから変換
			NBTItem nbt = new NBTItem(item);
			NBTCompound comp = nbt.getCompound("PublicBukkitValues");
			int box = comp.getInteger("skript:item.box");
			int id = comp.getInteger("skript:item.id");

			itemId = new ItemId(box, id);
		}
		catch (Exception e) {
			itemId = new ItemId(0, 0);
		}

		// アイテムを作成
		SkriptItem divItem = new SkriptItem(itemId);
		divItem.setItem(item);

		// アイテムを生成して返す
		return divItem;
	}

	/**
	 * コンストラクタ
	 */
	public SkriptItem(ItemId itemId) {
		super(itemId);
	}
}
