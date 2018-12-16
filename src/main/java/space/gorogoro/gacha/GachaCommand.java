package space.gorogoro.gacha;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
/*
 * FrameGuardCommand
 * @license    LGPLv3
 * @copyright  Copyright gorogoro.space 2018
 * @author     kubotan
 * @see        <a href="http://blog.gorogoro.space">Kubotan's blog.</a>
 */
public class GachaCommand {
  private Gacha gacha;
  private CommandSender sender;
  private String[] args;
  protected static final String META_CHEST = "gacha.chest";
  protected static final String FORMAT_TICKET_CODE = "GACHA CODE:%s";

  /**
   * Constructor of GachaCommand.
   * @param Gacha gacha
   */
	public GachaCommand(Gacha gacha) {
		try {
			this.gacha = gacha;
		} catch (Exception e) {
			GachaUtility.logStackTrace(e);
		}
	}

  /**
   * Initialize
   * @param CommandSender CommandSender
   * @param String[] Argument
   */
  public void initialize(CommandSender sender, String[] args){
    try{
      this.sender = sender;
      this.args = args;
    } catch (Exception e){
      GachaUtility.logStackTrace(e);
    }
  }

  /**
   * Finalize
   */
  public void finalize() {
    try{
      this.sender = null;
      this.args = null;
    } catch (Exception e){
      GachaUtility.logStackTrace(e);
    }
  }

  /**
   * Processing of command list.
   * @return boolean true:Success false:Failure
   */
  public boolean list() {
    List<String> glist = gacha.getDatabase().list();
    if(glist.size() <= 0) {
      GachaUtility.sendMessage(sender, "Record not found.");
      return true;
    }

    for(String msg: glist) {
      GachaUtility.sendMessage(sender, msg);
    }
    return true;
  }

  /**
   * Processing of command modify.
   * @return boolean true:Success false:Failure
   */
  public boolean modify() {
    if(args.length != 2) {
      return false;
    }

    if(!(sender instanceof Player)) {
      return false;
    }

    String gachaName = args[1];
    if(gacha.getDatabase().getGacha(gachaName) == null) {
      GachaUtility.sendMessage(sender, "Record not found. gacha_name=" + gachaName);
      return true;
    }
    GachaUtility.setPunch((Player)sender, gacha, gachaName);
    GachaUtility.sendMessage(sender, "Please punching(right click) a chest of gachagacha. gacha_name=" + gachaName);
    return true;
  }

  /**
   * Processing of command delete.
   * @return boolean true:Success false:Failure
   */
  public boolean delete() {
    if(args.length != 2) {
      return false;
    }

    String gachaName = args[1];
    if(gacha.getDatabase().deleteGacha(gachaName)) {
      GachaUtility.sendMessage(sender, "Deleted. gacha_name=" + gachaName);
      return true;
    }
    return false;
  }

  /**
   * Processing of command ticket.
   * @return boolean true:Success false:Failure
   */
	public boolean ticket(Economy econ) {

		if (args.length != 2) {
			return false;
		}
		Player player;
		String playerName = args[1];
		/* プレーヤ名が@pだったら */
		if ("@p".equals(playerName)) {
			player = (Player) sender;
			playerName = sender.getName();

			Inventory inv = player.getInventory();
			int slot = inv.firstEmpty();
			if ( slot == -1) { // 空のスロットがない
				sender.sendMessage(String.format("エラー：イベントリを空けてください！"));
				return false;
			}

			/* 現在のお金を表示 */
			/* エラー */
			sender.sendMessage(String.format("現在の現金 %s", econ.format(econ.getBalance(player))));
			/* 現在のお金が1000以上あったら */
			if( econ.has(player,1000)) {
				EconomyResponse r = econ.withdrawPlayer(player, 1000);
				if(r.transactionSuccess()) {
	                sender.sendMessage(String.format("お買い上げありがとうございます！$1000頂きました！"));
	            } else {
	                sender.sendMessage(String.format("An error occured: %s", r.errorMessage));
	                return false;
	            }
			}else {
				sender.sendMessage(String.format("$1000持っていません！"));
				return false;

			}
		/* コンソールからだったらまたはOPからだったら */
		}else if((sender instanceof ConsoleCommandSender) || sender.isOp()) {
			if(args.length != 2) {
			      return false;
			    }

			    playerName = args[1];
			    player = gacha.getServer().getPlayer(playerName);
			    if(player == null) {
			      return false;
			}
			
			
			
		}else {
			return false;
		}
		/*
		 * 名前でチケットの受取は使わない Player player = gacha.getServer().getPlayer(playerName);
		 * if(player == null) { return false; }
		 */
		/* チケットの発券機能 */

		int emptySlot = player.getInventory().firstEmpty();
		if (emptySlot == -1) {
			// not empty
			return false;
		}

		String ticketCode = gacha.getDatabase().getTicket();
		if (ticketCode == null) {
			GachaUtility.sendMessage(sender, "Failure generate ticket code.");
			return false;
		}

		ItemStack ticket = new ItemStack(Material.PAPER, 1);
		ItemMeta im = ticket.getItemMeta();
		im.setDisplayName(
				ChatColor.translateAlternateColorCodes('&', gacha.getConfig().getString("ticket-display-name")));
		ArrayList<String> lore = new ArrayList<String>();
		lore.add(ChatColor.translateAlternateColorCodes('&', gacha.getConfig().getString("ticket-lore1")));
		lore.add(ChatColor.translateAlternateColorCodes('&', gacha.getConfig().getString("ticket-lore2")));
		lore.add(String.format(FORMAT_TICKET_CODE, ticketCode));
		im.setLore(lore);
		ticket.setItemMeta(im);
		player.getInventory().setItem(emptySlot, ticket);

		GachaUtility.sendMessage(sender, "Issue a ticket. player_name=" + playerName);
		return true;
		
	}

  /**
   * Processing of command reload.
   * @return boolean true:Success false:Failure
   */
  public boolean reload() {
    gacha.reloadConfig();
    GachaUtility.sendMessage(sender, "reloaded.");
    return true;
  }

  /**
   * Processing of command enable.
   * @return boolean true:Success false:Failure
   */
  public boolean enable() {
    gacha.onEnable();
    GachaUtility.sendMessage(sender, "enabled.");
    return true;
  }

  /**
   * Processing of command fgdisable.
   * @return boolean true:Success false:Failure
   */
  public boolean disable() {
    gacha.onDisable();
    GachaUtility.sendMessage(sender, "disabled.");
    return true;
  }
}
