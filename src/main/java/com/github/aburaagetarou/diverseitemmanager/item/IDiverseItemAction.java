package com.github.aburaagetarou.diverseitemmanager.item;

import java.util.function.Consumer;

public interface IDiverseItemAction {

	/**
	 * 動作を実行
	 * @param item 実行対象アイテム
	 */
	void run(IDiverseItem item);
}
