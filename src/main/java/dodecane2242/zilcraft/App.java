package dodecane2242.zilcraft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import co.aikar.commands.PaperCommandManager;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class App extends JavaPlugin {
    private static App plugin;
    private static PaperCommandManager commandManager;

    @Override
    public void onEnable() {
        plugin = this;
        getLogger().info(" ________  ___  ___       ________  ________  ________  ________ _________   ");
        getLogger().info("|\\_____  \\|\\  \\|\\  \\     |\\   ____\\|\\   __  \\|\\   __  \\|\\  _____|\\___   ___\\ ");
        getLogger().info(" \\|___/  /\\ \\  \\ \\  \\    \\ \\  \\___|\\ \\  \\|\\  \\ \\  \\|\\  \\ \\  \\__/\\|___ \\  \\_| ");
        getLogger().info("     /  / /\\ \\  \\ \\  \\    \\ \\  \\    \\ \\   _  _\\ \\   __  \\ \\   __\\    \\ \\  \\  ");
        getLogger().info("    /  /_/__\\ \\  \\ \\  \\____\\ \\  \\____\\ \\  \\\\  \\\\ \\  \\ \\  \\ \\  \\_|     \\ \\  \\ ");
        getLogger().info("   |\\________\\ \\__\\ \\_______\\ \\_______\\ \\__\\\\ _\\\\ \\__\\ \\__\\ \\__\\       \\ \\__\\");
        getLogger().info("    \\|_______|\\|__|\\|_______|\\|_______|\\|__|\\|__|\\|__|\\|__|\\|__|        \\|__|");
        getServer().getPluginManager().registerEvents(new WalletUtil(), this);
        registerCommands();
        saveDefaultConfig();
        ZILCraft.loadTokens();    
        WalletUtil.loadData();
        commandManager.getCommandCompletions().registerAsyncCompletion("network_list", c -> {
            if(getConfig().getString("network").equals("mainnet")){
                return Arrays.asList("testnet");
            }
            return Arrays.asList("mainnet");
        });
        commandManager.getCommandCompletions().registerAsyncCompletion("full_network_list", c -> {
            return Arrays.asList("mainnet","testnet");
        });
        commandManager.getCommandCompletions().registerAsyncCompletion("locked_account_list", c -> {
            CommandSender sender = c.getSender();
            if(sender instanceof Player){
                Player player = (Player) sender;
                String username = player.getName();
                Map<String, String> accounts = WalletUtil.getUserAccounts(username);
                List<String> lockedAccounts = new ArrayList<>();
                for (Map.Entry<String, String> entry : accounts.entrySet()) {
                    if(!WalletUtil.isUnlocked(entry.getValue())){
                        lockedAccounts.add(entry.getKey());
                    }
                }
                return lockedAccounts;
            }
            return null;
        });
        commandManager.getCommandCompletions().registerAsyncCompletion("unlocked_account_list", c -> {
            CommandSender sender = c.getSender();
            if(sender instanceof Player){
                Player player = (Player) sender;
                String username = player.getName();
                Map<String, String> accounts = WalletUtil.getUserAccounts(username);
                List<String> unlockedAccounts = new ArrayList<>();
                for (Map.Entry<String, String> entry : accounts.entrySet()) {
                    if(WalletUtil.isUnlocked(entry.getValue())){
                        unlockedAccounts.add(entry.getKey());
                    }
                }
                return unlockedAccounts;
            }
            return null;
        });
        commandManager.getCommandCompletions().registerAsyncCompletion("account_list", c -> {
            CommandSender sender = c.getSender();
            if(sender instanceof Player){
                Player player = (Player) sender;
                String username = player.getName();
                return WalletUtil.getUserAccounts(username).keySet();
            }
            return null;
        });
    }

    private void registerCommands() {
        commandManager = new PaperCommandManager(this);
        commandManager.enableUnstableAPI("help");
        commandManager.setDefaultExceptionHandler((command, registeredCommand, sender, args, t) -> {
            getLogger().warning("Error occurred while executing command " + command.getName());
            return false;
        });
        commandManager.registerCommand(new ZILCraft());
    }

    @Override
    public void onDisable() {
        getLogger().info("ZILCraft is now disabled");
        WalletUtil.saveData();
    }

    public static App getPlugin() {
        return plugin;
    }

    public static PaperCommandManager getCommandManager() {
        return commandManager;
    }
}