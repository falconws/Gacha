package space.gorogoro.gacha;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;

import net.milkbowl.vault.economy.Economy;


/*
 * Gacha
 * @license    LGPLv3
 * @copyright  Copyright gorogoro.space 2018
 * @author     kubotan
 * @see        <a href="http://blog.gorogoro.space">Kubotan's blog.</a>
 */
public class Gacha extends JavaPlugin{
  private GachaDatabase database;
  private GachaCommand command;
  private GachaListener listener;
  private static final Logger log = Logger.getLogger("Minecraft");
  private static Economy econ = null;

  /**
   * Get GachaDatabase instance.
   */
  public GachaDatabase getDatabase() {
    return database;
  }

  /**
   * Get GachaCommand instance.
   */
  public GachaCommand getCommand() {
    return command;
  }

  /**
   * Get GachaListener instance.
   */
  public GachaListener getListener() {
    return listener;
  }

  /**
   * JavaPlugin method onEnable.
   */
  @Override
	public void onEnable() {
		try {
			getLogger().log(Level.INFO, "The Plugin Has Been Enabled!");

			// If there is no setting file, it is created
			if (!getDataFolder().exists()) {
				getDataFolder().mkdir();
			}

			File configFile = new File(getDataFolder(), "config.yml");
			if (!configFile.exists()) {
				saveDefaultConfig();
			}

			// copy languge template
			File configFile2 = new File(getDataFolder(), "config_jp.yml");
			if (!configFile2.exists()) {
				saveDefaultConfig();
			}

			// Initialize the database.
			database = new GachaDatabase(this);
			database.initialize();

			// Register event listener.
			PluginManager pm = getServer().getPluginManager();
			HandlerList.unregisterAll(this); // clean up
			listener = new GachaListener(this);
			pm.registerEvents(listener, this);

			// Instance prepared of GachaCommand.
			command = new GachaCommand(this);

		} catch (Exception e) {
			GachaUtility.logStackTrace(e);
		}

		/* setupEconomy */
		if (!setupEconomy()) {
			log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	public static Economy getEconomy() {
		return econ;
	}

  /**
   * JavaPlugin method onCommand.
   *
   * @return boolean true:Success false:Display the usage dialog set in plugin.yml
   */
  public boolean onCommand( CommandSender sender, Command commandInfo, String label, String[] args) {
    boolean hideUseageFlag = true;  // true:Success false:Display the usage dialog set in plugin.yml
    try{
      if(!commandInfo.getName().equals("gacha")) {
        return hideUseageFlag;
      }

      if(args.length <= 0) {
        return hideUseageFlag;
      }
      String subCommand = args[0];

      command.initialize(sender, args);
      switch(subCommand) {
        case "list":
          if(sender.hasPermission("gacha.list")) {
            hideUseageFlag = command.list();
          }
          break;

        case "modify":
          if(sender.hasPermission("gacha.modify")) {
            hideUseageFlag = command.modify();
          }
          break;

        case "delete":
          if(sender.hasPermission("gacha.delete")) {
            hideUseageFlag = command.delete();
          }
          break;

        case "ticket":
          if(sender.hasPermission("gacha.ticket")) {
            command.ticket(econ, (Player)sender);
            hideUseageFlag = true;
          }
          break;

        case "enable":
          if(sender.isOp()) {
            hideUseageFlag = command.enable();
          }
          break;

        case "reload":
          if(sender.isOp()) {
            hideUseageFlag = command.reload();
          }
          break;

        case "disable":
          if(sender.isOp()) {
            hideUseageFlag = command.disable();
          }
          break;

        default:
          hideUseageFlag = false;
      }
    }catch(Exception e){
      GachaUtility.logStackTrace(e);
    }finally{
      command.finalize();
    }
    return hideUseageFlag;
  }

  /**
   * JavaPlugin method onDisable.
   */
  @Override
  public void onDisable(){
    try{
      database.finalize();
      command.finalize();

      // Unregister all event listener.
      HandlerList.unregisterAll(this);

      getLogger().log(Level.INFO, "The Plugin Has Been Disabled!");
    } catch (Exception e){
      GachaUtility.logStackTrace(e);
    }
  }
}
