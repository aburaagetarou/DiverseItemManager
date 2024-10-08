package com.github.aburaagetarou.diverseitemmanager.container.enderchest;

import com.github.aburaagetarou.diverseitemmanager.DiverseItemManager;
import com.github.aburaagetarou.diverseitemmanager.item.IDiverseItem;
import com.github.aburaagetarou.diverseitemmanager.item.ItemUtil;
import de.tr7zw.nbtapi.*;
import jdk.jfr.internal.LogLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class EnderchestManager {

	// 保存するNBTのフィルター
	public static List<String> nbtFilter;
	public static int maxPage;
	public static short defaultRow;
	public static Component defaultTitle;

	// エンダーチェスト
	public static Map<UUID, Enderchest> enderchest = new HashMap<>();

	/**
	 * テーブル作成
	 */
	public static boolean createTable() {

		// オープン
		try(Connection con = DiverseItemManager.dataSource.getConnection()) {

			// SQL文作成
			StringBuilder sql = new StringBuilder();
			sql.append(" CREATE TABLE IF NOT EXISTS EnderchestSync ( ");
			sql.append("     owner       nvarchar(40)   NOT NULL     ");
			sql.append("   , status      smallint       NOT NULL     ");
			sql.append(" )                                           ");
			PreparedStatement stmtSync = con.prepareStatement(sql.toString());

			// SQL文実行
			stmtSync.executeUpdate();
			stmtSync.close();

			// SQL文作成
			sql = new StringBuilder();
			sql.append(" CREATE TABLE IF NOT EXISTS EnderchestHead ( ");
			sql.append("     owner       nvarchar(40)   NOT NULL     ");
			sql.append("   , page        smallint       NOT NULL     ");
			sql.append("   , row         smallint       NOT NULL     ");
			sql.append("   , name        nvarchar(30)                ");
			sql.append("   , usable      smallint       NOT NULL     ");
			sql.append("   , PRIMARY KEY(owner, page)                ");
			sql.append(" )                                           ");
			PreparedStatement stmtHead = con.prepareStatement(sql.toString());

			// SQL文実行
			stmtHead.executeUpdate();
			stmtHead.close();

			// SQL文作成
			sql = new StringBuilder();
			sql.append(" CREATE TABLE IF NOT EXISTS EnderchestBody ( ");
			sql.append("     owner       nvarchar(40)   NOT NULL     ");
			sql.append("   , page        smallint       NOT NULL     ");
			sql.append("   , slot        smallint       NOT NULL     ");
			sql.append("   , material    nvarchar(40)                ");
			sql.append("   , amount      smallint                    ");
			sql.append("   , nbt         text                        ");
			sql.append("   , PRIMARY KEY(owner, page, slot)          ");
			sql.append(" )                                           ");
			PreparedStatement stmtBody = con.prepareStatement(sql.toString());

			// SQL文実行
			stmtBody.executeUpdate();
			stmtBody.close();

			return true;
		}
		catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * エンダーチェストをすべて読み込み
	 * @param uuid 所有者
	 */
	public static boolean checkSync(UUID uuid) {

		// UUIDを文字列に変換
		String owner = uuid.toString();

		// オープン
		try (Connection con = DiverseItemManager.dataSource.getConnection()) {

			// SQL文作成
			StringBuilder sql = new StringBuilder();
			sql.append(" SELECT status         ");
			sql.append(" FROM   EnderchestSync ");
			sql.append(" WHERE  owner = ?      ");
			PreparedStatement stmt = con.prepareStatement(sql.toString());

			// 条件セット
			stmt.setNString(1, owner);

			// SQL文実行
			ResultSet result = stmt.executeQuery();

			// データ読み込み
			boolean isSync = false;
			if (result.next()) {
				isSync = true;
			}

			result.close();
			stmt.close();

			return isSync;
		}
		catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * エンダーチェストをすべて読み込み
	 * @param uuid 所有者
	 */
	public static void loadEnderchest(UUID uuid) {

		// UUIDを文字列に変換
		String owner = uuid.toString();

		// オープン
		try(Connection con = DiverseItemManager.dataSource.getConnection()) {

			// SQL文作成
			StringBuilder sql = new StringBuilder();
			sql.append(" SELECT page           ");
			sql.append("      , row            ");
			sql.append("      , name           ");
			sql.append("      , usable         ");
			sql.append(" FROM   EnderchestHead ");
			sql.append(" WHERE  owner = ?      ");
			sql.append(" ORDER BY page         ");
			PreparedStatement stmtHead = con.prepareStatement(sql.toString());

			// 条件セット
			stmtHead.setNString(1, owner);

			// SQL文実行
			ResultSet resultHead = stmtHead.executeQuery();

			// クリア
			enderchest.remove(uuid);

			Enderchest ec = new Enderchest();

			// データ読み込み
			int page;
			short row;
			String name;
			int usable;
			while(resultHead.next()) {
				page = resultHead.getInt("page");
				row = resultHead.getShort("row");
				name = resultHead.getString("name");
				usable = resultHead.getInt("usable");	// ※未使用

				// ページ追加
				Component title = LegacyComponentSerializer.legacyAmpersand().deserialize(name);
				ec.addPage(page, row, title);
			}

			// クローズ
			resultHead.close();
			stmtHead.close();

			// ページが作成されていない場合、初期化
			for(int i = 0; i < maxPage; i++) {
				if(!ec.hasPage(i)) {
					ec.addPage(i, defaultRow, defaultTitle.append(LegacyComponentSerializer.legacyAmpersand().deserialize(" " + i)));
				}
			}

			// SQL文作成
			sql = new StringBuilder();
			sql.append(" SELECT page           ");
			sql.append("      , slot           ");
			sql.append("      , material       ");
			sql.append("      , amount         ");
			sql.append("      , nbt            ");
			sql.append(" FROM   EnderchestBody ");
			sql.append(" WHERE  owner = ?      ");
			sql.append(" ORDER BY page         ");
			sql.append("        , slot         ");
			PreparedStatement stmtBody = con.prepareStatement(sql.toString());

			// 条件セット
			stmtBody.setNString(1, owner);

			// SQL文実行
			ResultSet resultBody = stmtBody.executeQuery();

			// データ読み込み
			int slot;
			String nbt;
			String material;
			short amount;
			while(resultBody.next()) {
				page = resultBody.getInt("page");
				slot = resultBody.getInt("slot");
				material = resultBody.getString("material");
				amount = resultBody.getShort("amount");
				nbt = resultBody.getString("nbt");

				// ページが存在する場合
				if(ec.hasPage(page)) {

					// 旧形式対応
					NBTItem nbtItem;
					if(material == null) {

						// NBTからアイテムを作成
						ItemStack dataItem = NBTItem.convertNBTtoItem(new NBTContainer(nbt));
						nbtItem = new NBTItem(dataItem);
					}
					else {
						ItemStack dataItem = new ItemStack(Material.valueOf(material));
						dataItem.setAmount(amount);
						nbtItem = new NBTItem(dataItem);
						nbtItem.mergeCompound(new NBTContainer(nbt));
					}
					ItemStack rawItem = nbtItem.getItem();
					IDiverseItem item;

					// DIVERSEアイテムを使用する場合
					item = ItemUtil.getItem(rawItem);

					// アイテムセット
					if (item != null) {
						ec.getPage(page).setItem(slot, item);
					}
				}
			}

			// クローズ
			resultBody.close();
			stmtBody.close();

			// エンダーチェスト追加
			enderchest.put(uuid, ec);
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * エンダーチェストを開く
	 * @param player 対象
	 */
	public static void open(Player player) {
		if(enderchest.containsKey(player.getUniqueId())) {
			player.closeInventory();
			enderchest.get(player.getUniqueId()).show(player, 0);
		}
	}

	/**
	 * 他人のエンダーチェストを開く
	 * @param target 他人
	 * @param player 対象者
	 */
	public static void open(OfflinePlayer target, Player player) {
		if(enderchest.containsKey(target.getUniqueId())) {
			player.closeInventory();
			enderchest.get(target.getUniqueId()).show(player, 0);
		}
		else {
			// 読み込んで表示
			Bukkit.getScheduler().runTaskAsynchronously(DiverseItemManager.getInstance(), () -> {
				loadEnderchest(target.getUniqueId());

				if(enderchest.containsKey(target.getUniqueId())) {
					Bukkit.getScheduler().runTaskLater(DiverseItemManager.getInstance(), () ->
						enderchest.get(target.getUniqueId()).show(player, 0)
					, 1);
				}
				else {
					player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&c対象者のエンダーチェストは存在しません。"));
				}
			});
		}
	}

	/**
	 * 全データを保存
	 */
	public static void saveEnderchestAll() {

		if(enderchest.size() == 0) return;

		// 全データを保存
		Set<UUID> keySet = new HashSet<>(enderchest.keySet());
		for(UUID key : keySet) {
			if(!saveEnderChest(key)) return;

			// オフラインの場合はアンロード
			if(!Bukkit.getOfflinePlayer(key).isOnline()) {
				enderchest.remove(key);
			}
		}
	}

	/**
	 * エンダーチェストを保存
	 * @param uuid 所持者
	 * @return 成否
	 */
	public static boolean saveEnderChest(UUID uuid) {

		// UUIDを文字列化
		String owner = uuid.toString();

		Connection con = null;
		try {
			// エンダーチェストが存在しなければ保存しない
			if(!enderchest.containsKey(uuid)) return false;

			// オープン
			con = DiverseItemManager.dataSource.getConnection();

			// トランザクション開始
			con.setAutoCommit(false);

			Enderchest ec = enderchest.get(uuid);
			if( ec == null ) {
				con.rollback();
				return false;
			}

			// SQL文作成
			StringBuilder sql = new StringBuilder();
			sql.append(" INSERT INTO EnderchestSync ( ");
			sql.append("         owner                ");
			sql.append("       , status               ");
			sql.append(" ) VALUES (                   ");
			sql.append("        ?                     ");
			sql.append("      , ?                     ");
			sql.append(" )                            ");
			PreparedStatement stmtSync = con.prepareStatement(sql.toString());

			stmtSync.setNString(1, owner);
			stmtSync.setShort(2, (short) 1);

			// SQL文実行
			int upd = stmtSync.executeUpdate();

			// クローズ
			stmtSync.close();

			// ロールバック・コミット
			if (upd != 1) {
				con.rollback();
				return false;
			}

			// EC削除
			if( !deleteEnderChest(con, owner) ) {
				con.rollback();
				return false;
			}

			// データ登録
			for(short i = 0; i < ec.getPageCount(); i++) {

				// ページ取得
				EnderchestPage page = ec.getPage(i);

				// SQL文作成
				sql = new StringBuilder();
				sql.append(" INSERT INTO EnderchestHead ( ");
				sql.append("         owner                ");
				sql.append("       , page                 ");
				sql.append("       , row                  ");
				sql.append("       , name                 ");
				sql.append("       , usable               ");
				sql.append(" ) VALUES (                   ");
				sql.append("        ?                     ");
				sql.append("      , ?                     ");
				sql.append("      , ?                     ");
				sql.append("      , ?                     ");
				sql.append("      , ?                     ");
				sql.append(" )                            ");
				PreparedStatement stmt = con.prepareStatement(sql.toString());

				// 条件セット
				stmt.setNString(1, owner);
				stmt.setShort(2, i);
				stmt.setShort(3, page.row);
				stmt.setNString(4, LegacyComponentSerializer.legacyAmpersand().serialize(page.title));
				stmt.setShort(5, (short)1);

				// SQL文実行
				int count = stmt.executeUpdate();

				// クローズ
				stmt.close();

				// ロールバック・コミット
				if (count != 1) {
					con.rollback();
					return false;
				}

				// アイテムデータ登録
				// SQL文作成
				StringBuilder sqlBody = new StringBuilder();
				sqlBody.append(" INSERT INTO EnderchestBody ( ");
				sqlBody.append("         owner                ");
				sqlBody.append("       , page                 ");
				sqlBody.append("       , slot                 ");
				sqlBody.append("       , material             ");
				sqlBody.append("       , amount               ");
				sqlBody.append("       , nbt                  ");
				sqlBody.append(" ) VALUES (                   ");
				sqlBody.append("        ?                     ");
				sqlBody.append("      , ?                     ");
				sqlBody.append("      , ?                     ");
				sqlBody.append("      , ?                     ");
				sqlBody.append("      , ?                     ");
				sqlBody.append("      , ?                     ");
				sqlBody.append(" )                            ");
				PreparedStatement stmtBody = con.prepareStatement(sqlBody.toString());
				for( short j = 0; j < (page.row * 9); j++ ) {

					// アイテムを得る
					ItemStack item = page.getInventory().getItem(j);
					if(item == null || item.getType() == Material.AIR) continue;
					NBTItem nbtItem = new NBTItem(item);
					NBTContainer save = new NBTContainer();
					for(String key : nbtFilter) {
						int index;
						NBTCompound nbt = nbtItem;
						List<String> keyLevels = new ArrayList<>();
						while((index = key.indexOf(';')) > 0) {
							keyLevels.add(key.substring(0, index));
							key = key.substring(index + 1);
						}
						for(String keyLevel : keyLevels) {
							if(nbt.getType(keyLevel) != NBTType.NBTTagCompound) {
								nbt = null;
								break;
							}
							nbt = nbt.getCompound(keyLevel);
						}
						if(nbt == null) continue;
						NBTCompound target = save;
						for(String keyLevel : keyLevels) {
							target = target.addCompound(keyLevel);
						}
						switch (nbt.getType(key)) {
							case NBTTagString:
								target.setString(key, nbt.getString(key));
								break;

							case NBTTagByte:
								target.setByte(key, nbt.getByte(key));
								break;

							case NBTTagShort:
								target.setShort(key, nbt.getShort(key));
								break;

							case NBTTagInt:
								target.setInteger(key, nbt.getInteger(key));
								break;

							case NBTTagLong:
								target.setLong(key, nbt.getLong(key));
								break;

							case NBTTagFloat:
								target.setFloat(key, nbt.getFloat(key));
								break;

							case NBTTagDouble:
								target.setDouble(key, nbt.getDouble(key));
								break;

							case NBTTagByteArray:
								target.setByteArray(key, nbt.getByteArray(key));
								break;

							case NBTTagIntArray:
								target.setIntArray(key, nbt.getIntArray(key));
								break;

							case NBTTagCompound:
								target.addCompound(key).mergeCompound(nbt.getCompound(key));
								break;
						}
					}

					// 条件セット
					stmtBody.setNString(1, owner);
					stmtBody.setShort(2, i);
					stmtBody.setShort(3, j);
					stmtBody.setString(4, item.getType().name());
					stmtBody.setShort(5, (short)item.getAmount());
					stmtBody.setNString(6, save.getCompound().toString());

					// SQL文実行
					stmtBody.addBatch();
				}

				// SQL文実行
				stmtBody.executeLargeBatch();

				// クローズ
				stmtBody.close();
			}

			// 登録完了
			con.commit();

			return true;
		}
		catch (SQLException e) {
			DiverseItemManager.getInstance().getLogger().log(Level.WARNING, "", e);

			try {
				// ロールバック
				if(con != null)	con.rollback();
			}
			catch (SQLException e2) {
				e2.printStackTrace();
			}
			return false;
		}
		finally {
			try {
				// トランザクション終了
				if (con != null) {

					// SQL文作成
					StringBuilder sql = new StringBuilder();
					sql.append(" DELETE FROM EnderchestSync ");
					sql.append(" WHERE  owner = ?           ");
					PreparedStatement stmtSync = con.prepareStatement(sql.toString());

					// 条件セット
					stmtSync.setNString(1, owner);

					// SQL文実行
					int count = stmtSync.executeUpdate();

					// クローズ
					stmtSync.close();

					// ロールバック・コミット
					if (count != 1) {
						con.rollback();
					}

					con.setAutoCommit(true);
					con.close();
				}
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * エンダーチェストを削除
	 * @param con DB接続
	 * @param owner 所有者
	 */
	private static boolean deleteEnderChest(Connection con, String owner) {

		try {

			// SQL文作成
			StringBuilder sql = new StringBuilder();
			sql.append(" DELETE FROM EnderchestHead ");
			sql.append(" WHERE  owner = ?           ");
			PreparedStatement stmtHead = con.prepareStatement(sql.toString());

			// 条件セット
			stmtHead.setNString(1, owner);

			// SQL文実行
			int count = stmtHead.executeUpdate();

			// クローズ
			stmtHead.close();

			// SQL文作成
			sql = new StringBuilder();
			sql.append(" DELETE FROM EnderchestBody ");
			sql.append(" WHERE  owner = ?           ");
			PreparedStatement stmtBody = con.prepareStatement(sql.toString());

			// 条件セット
			stmtBody.setNString(1, owner);

			// SQL文実行
			stmtBody.executeUpdate();

			// クローズ
			stmtBody.close();
			return true;
		}
		catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
}
