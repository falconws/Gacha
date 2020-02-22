package space.gorogoro.gacha;

import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/*
 * GachaListener
 * @license    LGPLv3
 * @copyright  Copyright gorogoro.space 2018
 * @author     kubotan
 * @see        <a href="http://blog.gorogoro.space">Kubotan's blog.</a>
 */
public class GachaListener implements Listener{
  private Gacha gacha;

  /**
   * Constructor of GachaListener.
   */
  public GachaListener(Gacha gacha) {
    try{
      this.gacha = gacha;
    } catch (Exception e){
      GachaUtility.logStackTrace(e);
    }
  }

  /**
   * チケット発行看板
   * 看板の1行目に[ticket]の文字列がある看板が作成された際の処理
   * @param event SignChangeEvent
   */
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onTicketSignBoard(SignChangeEvent event) {
    if(!event.getLine(0).toLowerCase().equals("[ticket]")) {
      return;
    }

    event.setLine(0, ChatColor.translateAlternateColorCodes(
            '&', gacha.getConfig().getString("sign-line1-prefix") + event.getLine(0)));

  }

  /**
   * プレイヤーが1行目に[ticket]の文字列がある看板を右クリックした際の処理
   * @param event PlayerInteractEvent
   */
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onRightClick(PlayerInteractEvent event) {
    if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
      return;
    }
    Block clickedBlock = event.getClickedBlock();
    Material material = clickedBlock.getType();
    if (material.equals(Material.OAK_SIGN) || material.equals(Material.OAK_WALL_SIGN)) {
      BlockState state = clickedBlock.getState();
      Sign sign = (Sign) state;
      // @TODO [ticket]が1行目にある看板限定だが、先頭にカラーコードがついているのでこのような暫定コード。もっとマシにリファクタリングすべき。
      if (sign.getLine(0).equals("§6§l[ticket]")) {
        GachaCommand command = gacha.getCommand();
        command.ticket(Gacha.getEconomy(), event.getPlayer());
      }
    }
  }

  /**
   * On sign change
   * @param SignChangeEvent event
   */
  @EventHandler(priority=EventPriority.HIGHEST)
  public void onSignChange(SignChangeEvent event) {
    try {
      if(!event.getLine(0).toLowerCase().equals("[gacha]")) {
        return;
      }

      if(!event.getPlayer().hasPermission("gacha.create")) {
        return;
      }

      Location signLoc = event.getBlock().getLocation();
      if(gacha.getDatabase().isGacha(signLoc)) {
        event.setCancelled(true);
        GachaUtility.sendMessage(event.getPlayer(), "It is already registered. To continue, please delete first.");
        return;
      }

      String gachaName = event.getLine(1);
      String gachaDisplayName = event.getLine(2);
      String gachaDetail = event.getLine(3);
      String worldName = signLoc.getWorld().getName();
      Integer x = signLoc.getBlockX();
      Integer y = signLoc.getBlockY();
      Integer z = signLoc.getBlockZ();
      Pattern p = Pattern.compile("^[0-9a-zA-Z_]+$");
      if(!p.matcher(gachaName).find()) {
        event.setCancelled(true);
        GachaUtility.sendMessage(event.getPlayer(), "Please enter the second line of the signboard with one-byte alphanumeric underscore.");
        return;
      }

      Integer gachaId = gacha.getDatabase().getGacha(gachaName, gachaDisplayName, gachaDetail, worldName, x, y, z);
      if(gachaId == null) {
        event.setCancelled(true);
        throw new Exception("Can not get gacha. gachaName=" + gachaName);
      }

      event.setLine(0, ChatColor.translateAlternateColorCodes('&', gacha.getConfig().getString("sign-line1-prefix") + gachaDisplayName));
      event.setLine(1, ChatColor.translateAlternateColorCodes('&', gacha.getConfig().getString("sign-line2-prefix") + gachaDetail));
      event.setLine(2, ChatColor.translateAlternateColorCodes('&', gacha.getConfig().getString("sign-line3")));
      event.setLine(3, ChatColor.translateAlternateColorCodes('&', gacha.getConfig().getString("sign-line4")));

    } catch (Exception e){
      GachaUtility.logStackTrace(e);
    }
  }

  /**
   * On player interact
   * @param PlayerInteractEvent event
   */
  @EventHandler(priority=EventPriority.HIGHEST)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
      return;
    }
    Block clickedBlock = event.getClickedBlock();
    Material material = clickedBlock.getType();
    if (material.equals(Material.OAK_SIGN) || material.equals(Material.OAK_WALL_SIGN)) {
      signProc(event);
    }else if(material.equals(Material.CHEST)) {
      chestProc(event);
    }
  }

  /**
   * Sign process.
   * @param PlayerInteractEvent event
   */
  private void signProc(PlayerInteractEvent event) {
    try {
      Sign sign = (Sign) event.getClickedBlock().getState();
      Player p = event.getPlayer();

      Location signLoc = sign.getLocation();
      if(!gacha.getDatabase().isGacha(signLoc)) {
        return;
      }
      event.setCancelled(true);

      ItemStack ticket = p.getInventory().getItemInMainHand();
      if( !ticket.getType().equals(Material.PAPER) ) {
        GachaUtility.sendMessage(p, ChatColor.translateAlternateColorCodes('&', gacha.getConfig().getString("hold-the-ticket")));
        return;
      }

      List<String> lores = ticket.getItemMeta().getLore();
      if( lores.size() != 3) {
        GachaUtility.sendMessage(p, ChatColor.translateAlternateColorCodes('&', gacha.getConfig().getString("hold-the-ticket")));
        return;
      }

      String ticketCode = GachaUtility.scanf(GachaCommand.FORMAT_TICKET_CODE, lores.get(2));
      if(!gacha.getDatabase().existsTicket(ticketCode)) {
        GachaUtility.sendMessage(p, ChatColor.translateAlternateColorCodes('&', gacha.getConfig().getString("not-found-ticket-code")));
        return;
      }

      Chest chest = gacha.getDatabase().getGachaChest(signLoc);
      if(chest == null) {
        GachaUtility.sendMessage(p, ChatColor.translateAlternateColorCodes('&', gacha.getConfig().getString("not-found-chest1")));
        GachaUtility.sendMessage(p, ChatColor.translateAlternateColorCodes('&', gacha.getConfig().getString("not-found-chest2")));
        return;
      }

      gacha.getDatabase().deleteTicket(ticketCode);
      p.getInventory().getItemInMainHand().setAmount(p.getInventory().getItemInMainHand().getAmount() - 1);

      Inventory iv = chest.getInventory();
      int pick = new Random().nextInt(iv.getSize());
      ItemStack pickItem = iv.getItem(pick);
      if(pickItem == null) {
        GachaUtility.sendMessage(p, ChatColor.translateAlternateColorCodes('&', gacha.getConfig().getString("not-found-pick")));
        return;
      }

      ItemStack sendItem = pickItem.clone();
      p.getInventory().addItem(sendItem);
      // GachaUtility.sendMessage(p, ChatColor.translateAlternateColorCodes('&', gacha.getConfig().getString("found-pick")));
			Bukkit.getServer().broadcastMessage(
					String.format("%sさんに%sが%s個当たりました！", p.getName(), sendItem.getType(), sendItem.getAmount()));
    } catch (Exception e){
      GachaUtility.logStackTrace(e);
    }
  }

  /**
   * Chest process.
   * @param PlayerInteractEvent event
   */
  private void chestProc(PlayerInteractEvent event) {
    try {
      Player p = event.getPlayer();
      if(!p.getType().equals(EntityType.PLAYER)){
        return;
      }

      if(!event.getClickedBlock().getType().equals(Material.CHEST)) {
        return;
      }

      if(!GachaUtility.isInPunch(p)) {
        return;
      } else {
        event.setCancelled(true);
      }

      String gachaName = GachaUtility.getGachaNameInPunch(p);
      GachaUtility.removePunch(p, gacha);
      if(gachaName == null) {
        return;
      }

      Location loc = event.getClickedBlock().getLocation();
      if(gacha.getDatabase().updateGachaChest(gachaName, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
        GachaUtility.sendMessage(p, "Updated. gacha_name=" + gachaName);
        return;
      }

    } catch (Exception e){
      GachaUtility.logStackTrace(e);
    }
  }
}
