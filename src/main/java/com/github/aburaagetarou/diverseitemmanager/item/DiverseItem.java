package com.github.aburaagetarou.diverseitemmanager.item;

import com.github.aburaagetarou.diverseitemmanager.util.ItemPersistentUtils;
import org.bukkit.inventory.ItemStack;

/**
 * WIP:このプラグインでアイテムを管理できるようにしたい
 */
public class DiverseItem extends DiverseItemImpl {

	/**
	 * アイテムを作成
	 * @param item 基となるアイテム
	 * @return アイテム
	 */
	public static DiverseItem get(ItemStack item) {

		// BOX,IDを取得
		int box = ItemPersistentUtils.getLong(item, "Item.Box").intValue();
		int id  = ItemPersistentUtils.getLong(item, "Item.Id" ).intValue();

		// アイテムを作成
		DiverseItem divItem = new DiverseItem(new ItemId(box, id));
		divItem.setItem(item);

		// アイテムを生成して返す
		return divItem;
	}

	/**
	 * コンストラクタ
	 */
	public DiverseItem(ItemId itemId) {
		super(itemId);
	}
}
