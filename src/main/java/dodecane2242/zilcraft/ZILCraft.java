package dodecane2242.zilcraft;

import java.math.BigDecimal;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.firestack.laksaj.crypto.KeyTools;
import com.firestack.laksaj.jsonrpc.HttpProvider;
import com.firestack.laksaj.utils.Validation;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.NumericPrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Single;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import co.aikar.commands.annotation.Values;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

@CommandAlias("zilcraft|zc|zil")
@Description("ZILCraft commands")
@CommandPermission("zilcraft")
public class ZILCraft extends BaseCommand {
    static App plugin = App.getPlugin();
    PaperCommandManager commandManager = App.getCommandManager();
    static public Map<String, String> apiURLMap = Stream
            .of(new String[][] { { "mainnet", "https://api.zilliqa.com/" },
                    { "testnet", "https://dev-api.zilliqa.com/" }, })
            .collect(Collectors.toMap(data -> data[0], data -> data[1]));
    Map<String, String> nameMap = Stream
            .of(new String[][] { { "mainnet", "Zilliqa Mainnet" }, { "testnet", "Developer Testnet" }, })
            .collect(Collectors.toMap(data -> data[0], data -> data[1]));
    Map<String, Integer> chainIDMap = Stream.of(new Object[][] { { "mainnet", 1 }, { "testnet", 333 }, })
            .collect(Collectors.toMap(data -> (String) data[0], data -> (Integer) data[1]));
    String network = plugin.getConfig().getString("network");
    String apiURL = apiURLMap.get(network);
    String networkName = nameMap.get(network);
    Integer chainID = chainIDMap.get(network);
    HttpProvider provider = new HttpProvider(apiURL);
    static Set<String> tokenNames = new HashSet<>();
    static Map<String, String> mainnetTokenAddresses = new HashMap<>();
    static Map<String, String> testnetTokenAddresses = new HashMap<>();
    static Map<String, String> tokenFormatting = new HashMap<>();

    private void reload() {
        plugin.reloadConfig();
        network = plugin.getConfig().getString("network");
        apiURL = apiURLMap.get(network);
        networkName = nameMap.get(network);
        chainID = chainIDMap.get(network);
        provider = new HttpProvider(apiURL);
        loadTokens();
        Bukkit.broadcastMessage("§3§l§eZILCraft is now using §b" + networkName);
    }

    static public void loadTokens() {
        plugin.reloadConfig();
        tokenNames.clear();
        mainnetTokenAddresses.clear();
        testnetTokenAddresses.clear();
        tokenFormatting.clear();
        tokenNames = plugin.getConfig().getConfigurationSection("tokens").getKeys(false);
        for(String name:tokenNames){
            mainnetTokenAddresses.put(name,plugin.getConfig().getString("tokens."+name+".mainnet"));
            testnetTokenAddresses.put(name,plugin.getConfig().getString("tokens."+name+".testnet"));
            tokenFormatting.put(name,plugin.getConfig().getString("tokens."+name+".formatting").replace("&", "§"));
        }
        tokenFormatting.put("ZIL","§3");
    }

    @Subcommand("help")
    @HelpCommand
    @Description("Show help for ZILCraft commands")
    @Syntax("")
    public void doHelp(CommandSender sender, CommandHelp help) {
        sender.sendMessage("§3§lZILCraft Help");
        help.showHelp();
    }

    @Subcommand("about")
    @Description("Show information about ZILCraft")
    public void onZCTest(CommandSender sender) {
        sender.sendMessage("§3§lZILCraft Version §f"+plugin.getDescription().getVersion());
        sender.sendMessage("§eAuthor: §6Dodecane");
        sender.sendMessage("§eContact me on Twitter at §6@dodecane2242");
        sender.sendMessage("§eGitHub repo: §9§nhttps://github.com/Dodecane/ZILCraft");
    }

    @Subcommand("reload|r")
    @Description("Reload ZILCraft config and user accounts")
    @CommandPermission("zilcraft.reload")
    public void onZCReload(CommandSender sender) {
        reload();
        WalletUtil.loadData();
        sender.sendMessage("§aZILCraft config and user accounts reloaded");
    }

    @Subcommand("save|s")
    @Description("Manually save ZILCraft config and user accounts")
    @CommandPermission("zilcraft.save")
    public void onZCSave(CommandSender sender) {
        plugin.saveConfig();
        WalletUtil.saveData();
        sender.sendMessage("§aZILCraft config and user accounts saved");
    }

    @Subcommand("balance|bal")
    @CommandPermission("zilcraft.balance")
    public class Balance extends BaseCommand {
        @Default
        @CatchUnknown
        @Description("Retrieve token balances of account or address")
        @CommandCompletion("@account_list")
        @Syntax("<account name | address>")
        public void onZCBalance(Player sender, @Single String arg) throws Exception {
            String username = sender.getName();
            Map<String, String> accounts = WalletUtil.getUserAccounts(username);
            if (accounts.containsKey(arg)) {
                String balance = WalletUtil.getZILBalance(accounts.get(arg), provider).toPlainString();
                sender.sendMessage("§3§lShowing token balances for account §b" + arg);
                sender.sendMessage(balance + " §3ZIL");         
                for(String name:tokenNames){
                    String tokenAddress = "";
                    if(network.equals("mainnet")){
                        tokenAddress = mainnetTokenAddresses.get(name);
                    }else{
                        tokenAddress = testnetTokenAddresses.get(name);
                    }
                    BigDecimal tokenBalance = WalletUtil.getTokenBalance(accounts.get(arg), tokenAddress, provider);
                    String formatting = tokenFormatting.get(name);
                    sender.sendMessage(tokenBalance.toPlainString() + " "+formatting+name);
                }
                Map<String, String> userTokens = WalletUtil.getUserTokens(username, network);
                for (Map.Entry<String, String> entry : userTokens.entrySet()) {
                    String name = entry.getKey();
                    String tokenAddress = entry.getValue();
                    BigDecimal tokenBalance = WalletUtil.getTokenBalance(accounts.get(arg), tokenAddress, provider);
                    sender.sendMessage(tokenBalance.toPlainString() + " §6"+name);
                }
            } else if (Validation.isBech32(arg)) {
                String balance = WalletUtil.getZILBalance(arg, provider).toPlainString();
                TextComponent addressCopy = new TextComponent("§3" + arg);
                addressCopy.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Copy address to clipboard")));
                addressCopy.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, arg));
                sender.sendMessage("§3§lShowing token balances for address");
                sender.spigot().sendMessage(addressCopy);
                sender.sendMessage(balance + " §3ZIL");
                for(String name:tokenNames){
                    String tokenAddress = "";
                    if(network.equals("mainnet")){
                        tokenAddress = mainnetTokenAddresses.get(name);
                    }else{
                        tokenAddress = testnetTokenAddresses.get(name);
                    }
                    BigDecimal tokenBalance = WalletUtil.getTokenBalance(arg, tokenAddress, provider);
                    String formatting = tokenFormatting.get(name).replace("&", "§");
                    sender.sendMessage(tokenBalance.toPlainString() + " "+formatting+name);
                }
                Map<String, String> userTokens = WalletUtil.getUserTokens(username, network);
                for (Map.Entry<String, String> entry : userTokens.entrySet()) {
                    String name = entry.getKey();
                    String tokenAddress = entry.getValue();
                    BigDecimal tokenBalance = WalletUtil.getTokenBalance(arg, tokenAddress, provider);
                    sender.sendMessage(tokenBalance.toPlainString() + " §6"+name);
                }
            } else {
                sender.sendMessage("§cInvalid account name or Zilliqa address");
            }
        }
    }

    @Subcommand("network|net")
    @CommandPermission("zilcraft.network")
    public class Network extends BaseCommand {
        @Default
        @Description("Show information about connected network")
        public void onZCNetwork(CommandSender sender) {
            sender.sendMessage("§3§lNetwork Information");
            sender.sendMessage("§eNetwork: §b" + networkName);
            sender.sendMessage("§eAPI URL: §9§n" + apiURL);
        }

        @Subcommand("set")
        @Description("Change network")
        @CommandPermission("zilcraft.network.set")
        @Syntax("<mainnet | testnet>")
        @CommandCompletion("@network_list")
        public void onZCNetworkSet(CommandSender sender, @Values("@network_list") String arg) {
            if (arg.equals(network)) {
                sender.sendMessage("§cAlready on " + networkName);
            } else {
                plugin.getConfig().set("network", arg);
                plugin.saveConfig();
                reload();
                sender.sendMessage("§aSuccessfully switched to §b" + networkName);
            }
        }
    }

    @Subcommand("unlock|ul")
    @CommandPermission("zilcraft.unlock")
    public class Unlock extends BaseCommand {
        @Default
        @Description("Unlock account")
        @Syntax("<account name>")
        @CommandCompletion("@locked_account_list")
        public void onZCUnlock(Player sender, @Values("@locked_account_list") String name) {
            sender.sendMessage("§6§lAccount unlocking wizard");
            sender.sendMessage("§eType §acancel§e to abort at any point");
            Prompt askPassword = new ValidatingPrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    return "§bEnter password for " + name;
                }

                @Override
                protected boolean isInputValid(ConversationContext context, String input) {
                    return WalletUtil.unlockAccount(sender, name, input);
                }

                @Override
                protected String getFailedValidationText(ConversationContext context, String input) {
                    return "§cIncorrect password";
                }

                @Override
                protected Prompt acceptValidatedInput(ConversationContext context, String input) {
                    sender.spigot().sendMessage(
                            new ComponentBuilder("§aAccount §b" + name + " §asuccessfully unlocked").create());
                    sender.spigot()
                            .sendMessage(new ComponentBuilder("§eYour active account is now §b" + name).create());
                    return Prompt.END_OF_CONVERSATION;
                }
            };
            class onCancel implements ConversationAbandonedListener {
                @Override
                public void conversationAbandoned(ConversationAbandonedEvent event) {
                    if (!event.gracefulExit()) {
                        event.getContext().getForWhom().sendRawMessage("§7§oAccount unlocking cancelled");
                    }
                }
            }
            ConversationFactory factory = new ConversationFactory(plugin).withFirstPrompt(askPassword)
                    .withEscapeSequence("cancel").withLocalEcho(false).withModality(true)
                    .addConversationAbandonedListener(new onCancel()).withTimeout(120);
            factory.buildConversation(sender).begin();
        }
    }

    @Subcommand("lock|l")
    @CommandPermission("zilcraft.lock")
    public class Lock extends BaseCommand {
        @Default
        @Description("Lock all accounts")
        public void onZCUnlock(Player sender) {
            if (WalletUtil.lockAccounts(sender)) {
                sender.sendMessage("§aAll accounts successfully locked");
            }
        }
    }

    @Subcommand("account|acc")
    @CommandPermission("zilcraft.account")
    public class WalletCommands extends BaseCommand {
        @Subcommand("create")
        @Description("Create new account")
        @CommandPermission("zilcraft.account.create")
        public void onZCAccountCreate(Player sender) throws Exception {
            sender.sendMessage("§6§lAccount creation wizard");
            sender.sendMessage("§eType §acancel§e to abort at any point");
            Prompt askAccountName = new ValidatingPrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    return "§bEnter name for account";
                }

                @Override
                protected boolean isInputValid(ConversationContext context, String input) {
                    return !WalletUtil.getUserAccounts(sender.getName()).containsKey(input);
                }

                @Override
                protected String getFailedValidationText(ConversationContext context, String input) {
                    return "§cAccount name already in use";
                }

                @Override
                protected Prompt acceptValidatedInput(ConversationContext context, String input) {
                    try {
                        input=input.replace(" ", "_");
                        createAccount(input, (String) context.getSessionData("password"), (String) context.getSessionData("key"));
                    } catch (Exception e) {
                        e.printStackTrace();
                        sender.spigot().sendMessage(new ComponentBuilder("§cAccount creation failed").create());
                    }
                    return Prompt.END_OF_CONVERSATION;
                }

                public void createAccount(String name, String password, String privateKey) throws Exception {
                    String bech32address = WalletUtil.addAccount(sender, name, password, privateKey);
                    TextComponent createAccountSuccess = new TextComponent("§aAccount §b" + name + " §asuccessfully created with address");
                    TextComponent copyAddressToClipboard = new TextComponent("§3" + bech32address+" ");
                    copyAddressToClipboard.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Copy address to clipboard")));
                    copyAddressToClipboard.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, bech32address));
                    TextComponent viewBlockLink = new TextComponent("§9§nViewBlock");
                    String footer = "";
                    if (network.equals("testnet")) {
                        footer = "?network=testnet";
                    }
                    viewBlockLink.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("View on ViewBlock")));
                    viewBlockLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,"https://viewblock.io/zilliqa/address/"+bech32address+footer));
                    copyAddressToClipboard.addExtra(viewBlockLink);
                    sender.spigot().sendMessage(createAccountSuccess);
                    sender.spigot().sendMessage(copyAddressToClipboard);
                    sender.spigot().sendMessage(new ComponentBuilder("§eYour active account is now §b" + name).create());
                }
            };
            Prompt askAccountPassword = new StringPrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    return "§bEnter password for new account";
                };

                @Override
                public Prompt acceptInput(ConversationContext context, String input) {
                    context.setSessionData("password", input);
                    return askAccountName;
                };
            };
            Prompt confirmPrivateKey = new ValidatingPrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    return "§bConfirm private key to continue";
                }

                @Override
                protected boolean isInputValid(ConversationContext context, String input) {
                    if (!input.equals(context.getSessionData("key"))) {
                        return false;
                    }
                    return true;
                }

                @Override
                protected String getFailedValidationText(ConversationContext context, String input) {
                    return "§cIncorrect private key";
                }

                @Override
                protected Prompt acceptValidatedInput(ConversationContext context, String input) {
                    return askAccountPassword;
                }
            };
            Prompt showPrivateKey = new MessagePrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    try {
                        context.setSessionData("key", getPrivateKey());
                    } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
                        e.printStackTrace();
                        sender.spigot().sendMessage(new ComponentBuilder("§cFailed to generate private key").create());
                    }
                    return "§4§lNever disclose your private key. Anyone with this key can gain access to your account forever.";
                }

                private String getPrivateKey() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
                    sender.sendMessage("");
                    String privateKey = KeyTools.generatePrivateKey();
                    TextComponent showKey = new TextComponent("§7" + privateKey);
                    showKey.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Copy private key to clipboard")));
                    showKey.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, privateKey));
                    sender.spigot().sendMessage(new ComponentBuilder("§eYour private key:").create());
                    sender.spigot().sendMessage(showKey);
                    sender.spigot().sendMessage(new ComponentBuilder("§eTips:").create());
                    sender.spigot().sendMessage(new ComponentBuilder("§6- Store this key in a password manager.").create());
                    sender.spigot().sendMessage(new ComponentBuilder("§6- Write this key on a piece of paper and store in a secure location. If you want even more security, write it down on multiple pieces of paper and store each in 2 - 3 different locations.").create());
                    sender.spigot().sendMessage(new ComponentBuilder("§6- Memorize this key (you can try).").create());
                    sender.spigot().sendMessage(new ComponentBuilder("§6- Copy this key and keep it stored safely on an external encrypted hard drive or storage medium.").create());
                    sender.spigot().sendMessage(new ComponentBuilder("§eYour private key makes it easy to back up and restore your account.").create());
                    return privateKey;
                }

                @Override
                protected Prompt getNextPrompt(ConversationContext context) {
                    return confirmPrivateKey;
                }
            };
            class onCancel implements ConversationAbandonedListener {
                @Override
                public void conversationAbandoned(ConversationAbandonedEvent event) {
                    if (!event.gracefulExit()) {
                        event.getContext().getForWhom().sendRawMessage("§7§oAccount creation cancelled");
                    }
                }
            }
            ConversationFactory factory = new ConversationFactory(plugin).withFirstPrompt(showPrivateKey)
                    .withEscapeSequence("cancel").withLocalEcho(false).withModality(true)
                    .addConversationAbandonedListener(new onCancel()).withTimeout(120);
            factory.buildConversation(sender).begin();
        }

        @Subcommand("import|imp")
        @Description("Import existing account")
        @CommandPermission("zilcraft.account.import")
        @Syntax("<privatekey | mnemonic>")
        @CommandCompletion("privatekey|mnemonic")
        public void onZCAccountImport(Player sender, @Values("privatekey|mnemonic") String type) {
            sender.sendMessage("§6§lAccount import wizard");
            sender.sendMessage("§eType §acancel§e to abort at any point");
            if (type.equals("privatekey")) {
                Prompt askAccountName = new ValidatingPrompt() {
                    @Override
                    public String getPromptText(ConversationContext context) {
                        return "§bEnter name for account";
                    }

                    @Override
                    protected boolean isInputValid(ConversationContext context, String input) {
                        return !WalletUtil.getUserAccounts(sender.getName()).containsKey(input);
                    }

                    @Override
                    protected String getFailedValidationText(ConversationContext context, String input) {
                        return "§cAccount name already in use";
                    }

                    @Override
                    protected Prompt acceptValidatedInput(ConversationContext context, String input) {
                        try {
                            input=input.replace(" ", "_");
                            addAccount(input, (String) context.getSessionData("password"), (String) context.getSessionData("key"));
                        } catch (Exception e) {
                            e.printStackTrace();
                            sender.spigot().sendMessage(new ComponentBuilder("§cFailed to import account").create());
                        }
                        return Prompt.END_OF_CONVERSATION;
                    }

                    public void addAccount(String name, String password, String privateKey) throws Exception {
                        String bech32address = WalletUtil.addAccount(sender, name, password, privateKey);
                        TextComponent addAccountSuccess = new TextComponent(
                                "§aAccount §b" + name + " §asuccessfully imported with address");
                        TextComponent copyAddressToClipboard = new TextComponent("§3" + bech32address+" ");
                        copyAddressToClipboard.setHoverEvent(
                                new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Copy address to clipboard")));
                        copyAddressToClipboard
                                .setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, bech32address));
                        TextComponent viewBlockLink = new TextComponent("§9§nViewBlock");
                        String footer = "";
                        if (network.equals("testnet")) {
                            footer = "?network=testnet";
                        }
                        viewBlockLink.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("View on ViewBlock")));
                        viewBlockLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,"https://viewblock.io/zilliqa/address/"+bech32address+footer));
                        copyAddressToClipboard.addExtra(viewBlockLink);
                        sender.spigot().sendMessage(addAccountSuccess);
                        sender.spigot().sendMessage(copyAddressToClipboard);
                        sender.spigot().sendMessage(new ComponentBuilder("§eYour active account is now §b" + name).create());
                    }
                };
                Prompt askAccountPassword = new StringPrompt() {
                    @Override
                    public String getPromptText(ConversationContext context) {
                        return "§bEnter new password for account";
                    };

                    @Override
                    public Prompt acceptInput(ConversationContext context, String input) {
                        context.setSessionData("password", input);
                        return askAccountName;
                    };
                };
                Prompt askPrivateKey = new ValidatingPrompt() {
                    @Override
                    public String getPromptText(ConversationContext context) {
                        return "§bEnter private key of account";
                    }

                    @Override
                    protected boolean isInputValid(ConversationContext context, String input) {
                        String error = WalletUtil.isValidKey(input);
                        context.setSessionData("error", error);
                        if (error.equals("valid")) {
                            return true;
                        }
                        return false;
                    }

                    @Override
                    protected String getFailedValidationText(ConversationContext context, String input) {
                        return "§c" + context.getSessionData("error");
                    }

                    @Override
                    protected Prompt acceptValidatedInput(ConversationContext context, String input) {
                        context.setSessionData("key", input);
                        return askAccountPassword;
                    }
                };
                class onCancel implements ConversationAbandonedListener {
                    @Override
                    public void conversationAbandoned(ConversationAbandonedEvent event) {
                        if (!event.gracefulExit()) {
                            event.getContext().getForWhom().sendRawMessage("§7§oAccount addition cancelled");
                        }
                    }
                }
                ConversationFactory factory = new ConversationFactory(plugin).withFirstPrompt(askPrivateKey)
                        .withEscapeSequence("cancel").withLocalEcho(false).withModality(true)
                        .addConversationAbandonedListener(new onCancel()).withTimeout(120);
                factory.buildConversation(sender).begin();
            } else {
                sender.sendMessage("§eWork in progress, closing wizard");
                //TODO handle adding by mnemonic
            }
        }

        @Subcommand("remove|rm")
        @Description("Remove account(s)")
        @CommandPermission("zilcraft.account.remove")
        @Syntax("<account name | all>")
        @CommandCompletion("@account_list|all")
        public void onZCAccountRemove(Player sender, @Values("@account_list|all") String name) {
            if(name.equals(WalletUtil.getActiveAccount(sender.getName()))){
                sender.spigot().sendMessage(new ComponentBuilder("§cCannot remove active account §b"+name).create());
                sender.spigot().sendMessage(new ComponentBuilder("§cEither change active account with §a/zilcraft account set <account name> §cor remove all accounts with §a/zilcraft account remove all").create());
                return;
            }
            sender.sendMessage("§6§lAccount removal wizard");
            sender.sendMessage("§eType §acancel§e to abort at any point");
            if (name.equals("all")) {
                sender.sendMessage("§cAll accounts will be removed");
            }
            else{
                sender.sendMessage("§cAccount §b" + name + " §cwill be removed");
            }
            Prompt confirmRemoveAccount = new ValidatingPrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    if (name.equals("all")) {
                        return "§4§lType §cI am removing all of my accounts§4§l to confirm";
                    }
                    return "§4§lType name of account to confirm";
                }

                @Override
                protected boolean isInputValid(ConversationContext context, String input) {
                    if (name.equals("all")) {
                        if (input.equals("I am removing all of my accounts")) {
                            return true;
                        }
                        return false;
                    } else if (input.equals(name)) {
                        return true;
                    }
                    return false;
                }

                @Override
                protected String getFailedValidationText(ConversationContext context, String input) {
                    if (name.equals("all")) {
                        return "§cIncorrect input    §eType §acancel§e to abort";
                    }
                    return "§cIncorrect name    §eType §acancel§e to abort";
                }

                @Override
                protected Prompt acceptValidatedInput(ConversationContext context, String input) {
                    if (name.equals("all")) {
                        if (WalletUtil.removeAllAccounts(sender)) {
                            sender.spigot()
                                    .sendMessage(new ComponentBuilder("§aAll accounts successfully removed").create());
                        }
                    } else if (WalletUtil.removeAccount(sender, input)) {
                        sender.spigot().sendMessage(
                                new ComponentBuilder("§aAccount §b" + name + " §asuccessfully removed").create());
                    }
                    return Prompt.END_OF_CONVERSATION;
                }
            };
            class onCancel implements ConversationAbandonedListener {
                @Override
                public void conversationAbandoned(ConversationAbandonedEvent event) {
                    if (!event.gracefulExit()) {
                        event.getContext().getForWhom().sendRawMessage("§7§oAccount removal cancelled");
                    }
                }
            }
            ConversationFactory factory = new ConversationFactory(plugin).withFirstPrompt(confirmRemoveAccount)
                    .withEscapeSequence("cancel").withLocalEcho(false).withModality(true)
                    .addConversationAbandonedListener(new onCancel()).withTimeout(120);
            factory.buildConversation(sender).begin();
        }

        @Subcommand("set")
        @Description("Set active account")
        @CommandPermission("zilcraft.account.set")
        @Syntax("<account name>")
        @CommandCompletion("@account_list")
        public void onZCAccountSet(Player sender, @Values("@account_list") String name) {
            if (WalletUtil.setDefaultAccount(sender, name)) {
                sender.sendMessage("§aActive account successfully set to §b" + name);
            }
        }

        @Subcommand("rename")
        @Description("Rename account")
        @CommandPermission("zilcraft.account.rename")
        @Syntax("<account name> <new account name>")
        @CommandCompletion("@account_list")
        public void onZCAccountRename(Player sender, @Values("@account_list") String oldName, String newName) {
            newName = newName.replace(" ", "_");
            if(WalletUtil.getUserAccounts(sender.getName()).containsKey(newName)){
                sender.sendMessage("§cAccount name already in use");
            }else{
                if (WalletUtil.renameAccount(sender, oldName, newName)) {
                    sender.sendMessage("§aAccount successfully renamed to §b" + newName);
                }
            }
        }

        @Subcommand("list")
        @Description("List accounts")
        @CommandPermission("zilcraft.account.list")
        public void onZCAccountList(Player sender) throws Exception {
            sender.sendMessage("§3§lListing accounts for §f" + sender.getDisplayName());
            Map<String, String> accounts = WalletUtil.getUserAccounts(sender.getName());
            Boolean hasLockedAccounts = false;
            if (accounts.isEmpty()) {
                sender.sendMessage("§eNo accounts created or added");
                sender.sendMessage("§eGet started with §a/zilcraft account create§e or §a/zilcraft account add");
            }
            for (Map.Entry<String, String> entry : accounts.entrySet()) {
                String accountName = entry.getKey();
                String accountAddress = entry.getValue();
                TextComponent name = new TextComponent("§b" + accountName + " ");
                if (!WalletUtil.isUnlocked(accountAddress)) {
                    TextComponent locked = new TextComponent("§c[Locked] ");
                    locked.addExtra(name);
                    name = locked;
                    hasLockedAccounts = true;
                }
                if (WalletUtil.getActiveAddress(sender.getName()).equals(accountAddress)) {
                    TextComponent active = new TextComponent("§a[Active] ");
                    active.addExtra(name);
                    name = active;
                }
                TextComponent address = new TextComponent("§3" + accountAddress+" ");
                address.setHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Copy address to clipboard")));
                address.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, accountAddress));
                name.addExtra(address);
                String footer = "";
                if (network.equals("testnet")) {
                    footer = "?network=testnet";
                }
                TextComponent viewBlockLink = new TextComponent("§9§nViewBlock");
                viewBlockLink.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("View on ViewBlock")));
                viewBlockLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,"https://viewblock.io/zilliqa/address/"+accountAddress+footer));
                name.addExtra(viewBlockLink);
                sender.spigot().sendMessage(name);
            }
            if (hasLockedAccounts) {
                sender.sendMessage("§eUnlock accounts with §a/zilcraft unlock <account name>");
            }
        }
    }

    @Subcommand("send|sn")
    @CommandPermission("zilcraft.send")
    public class Send extends BaseCommand {
        @Default
        @CatchUnknown
        @Description("Send tokens to a player or address")
        @CommandCompletion("@players")
        @Syntax("<username | address>")
        public void onZCSend(Player sender, @Single String arg) throws Exception {
            String username = sender.getName();
            String senderBech32address = WalletUtil.getActiveAddress(username);
            String activeAccountName = WalletUtil.getActiveAccount(username);            
            Boolean isOnline = Bukkit.getServer().getPlayer(arg) != null;
            Map<String, String> userTokens = WalletUtil.getUserTokens(username, network);
            Prompt confirmTransaction = new ValidatingPrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    String format = (String) context.getSessionData("format");
                    BigDecimal amount = (BigDecimal) context.getSessionData("amount");
                    String tokenName = (String) context.getSessionData("token");
                    if((boolean) context.getSessionData("isAccount")){
                        sender.spigot().sendMessage(new ComponentBuilder(amount.toPlainString()+" "+format+tokenName+" §ewill be sent from your active account §b"+activeAccountName+" §eto §f"+arg+"§e's active account with a maximum gas cost of §f"+((BigDecimal)context.getSessionData("gas")).toString()+" §3ZIL").create());
                    } else {
                        sender.spigot().sendMessage(new ComponentBuilder(amount.toPlainString()+" "+format+tokenName+" §ewill be sent from your active account §b"+activeAccountName+" §eto §3"+arg+"§ with a maximum gas cost of §f"+((BigDecimal)context.getSessionData("gas")).toString()+" §3ZIL").create());
                    }                    
                    return "§4§lType §cconfirm §4§lto confirm transaction or type §acancel §4§lto abort";
                }

                @Override
                protected boolean isInputValid(ConversationContext context, String input) {
                    if (input.equals("confirm")) {
                        return true;
                    }
                    return false;
                }

                @Override
                protected String getFailedValidationText(ConversationContext context, String input) {
                    return "§cInvalid input";
                }

                @Override
                protected Prompt acceptValidatedInput(ConversationContext context, String input) {
                    String format = (String) context.getSessionData("format");
                    BigDecimal amount = (BigDecimal) context.getSessionData("amount");
                    String tokenName = (String) context.getSessionData("token");
                    String receiverBech32address = "";
                    if((boolean) context.getSessionData("isAccount")){
                        receiverBech32address = WalletUtil.getActiveAddress(arg);
                    }
                    else{
                        receiverBech32address = arg;
                    }
                    try {
                        HttpProvider.CreateTxResult tx = null;
                        if(tokenName.equals("ZIL")){
                            tx = WalletUtil.sendZIL(sender, receiverBech32address, amount, (BigDecimal)context.getSessionData("gas"), provider, chainID);
                        }
                        else if (!userTokens.containsKey(tokenName)){
                            if(network.equals("mainnet")){
                                tx = WalletUtil.sendToken(sender, receiverBech32address, (BigDecimal)context.getSessionData("amount"), (BigDecimal)context.getSessionData("gas"), mainnetTokenAddresses.get(tokenName), provider, chainID);
                            }else{
                                tx = WalletUtil.sendToken(sender, receiverBech32address, (BigDecimal)context.getSessionData("amount"), (BigDecimal)context.getSessionData("gas"), testnetTokenAddresses.get(tokenName), provider, chainID);
                            }
                        }else{
                            tx = WalletUtil.sendToken(sender, receiverBech32address, (BigDecimal)context.getSessionData("amount"), (BigDecimal)context.getSessionData("gas"), userTokens.get(tokenName), provider, chainID);
                        }
                        String txID = "0x"+tx.getTranID();
                        TextComponent pleaseWait = new TextComponent("§ePlease allow a few minutes for the transaction to be confirmed on the blockchain");
                        TextComponent submitTransactionSuccess = new TextComponent("§aTransaction successfully submitted with hash");
                        TextComponent copyHashToClipboard = new TextComponent("§7"+txID+" ");
                        copyHashToClipboard.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Copy transaction hash to clipboard")));
                        copyHashToClipboard.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, txID));
                        TextComponent viewBlockLink = new TextComponent("§9§nViewBlock");
                        String footer = "";
                        if (network.equals("testnet")) {
                            footer = "?network=testnet";
                        }
                        viewBlockLink.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("View on ViewBlock")));
                        viewBlockLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,"https://viewblock.io/zilliqa/tx/"+txID+footer));
                        copyHashToClipboard.addExtra(viewBlockLink);
                        sender.spigot().sendMessage(submitTransactionSuccess);
                        sender.spigot().sendMessage(copyHashToClipboard);
                        sender.spigot().sendMessage(pleaseWait);
                        if(isOnline){
                            Player receiver = Bukkit.getPlayer(arg);
                            TextComponent informTransaction = new TextComponent(sender.getDisplayName()+" §ahas submitted a transaction to send §f"+amount.toPlainString()+" "+format+tokenName+" §ato your active account §b"+WalletUtil.getActiveAccount(arg)+" ");
                            informTransaction.addExtra(viewBlockLink);
                            receiver.spigot().sendMessage(informTransaction);
                            receiver.spigot().sendMessage(pleaseWait);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        sender.spigot().sendMessage(new ComponentBuilder("§cTransaction failed").create());
                    }
                    return Prompt.END_OF_CONVERSATION;
                }
            };
            Prompt askAmount = new NumericPrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    String tokenName = (String) context.getSessionData("token");                    
                    BigDecimal balance = new BigDecimal("0");
                    if(tokenName.equals("ZIL")){
                        try {
                            balance = ((BigDecimal)context.getSessionData("ZILbalance")).subtract((BigDecimal)context.getSessionData("gas")).setScale(12);
                            context.setSessionData("balance", balance);
                        } catch (Exception e) {
                            e.printStackTrace();
                            sender.spigot().sendMessage(new ComponentBuilder("§cSomething went wrong").create());
                        }
                    }else if(!userTokens.containsKey(tokenName)){
                        try {
                            if(network.equals("mainnet")){
                                balance = WalletUtil.getTokenBalance(senderBech32address, mainnetTokenAddresses.get(tokenName), provider);
                            }else{
                                balance = WalletUtil.getTokenBalance(senderBech32address, testnetTokenAddresses.get(tokenName), provider);
                            }                            
                            context.setSessionData("balance", balance);
                        } catch (Exception e) {
                            e.printStackTrace();
                            sender.spigot().sendMessage(new ComponentBuilder("§cSomething went wrong").create());
                        }
                    }else{
                        try {
							balance = WalletUtil.getTokenBalance(senderBech32address, userTokens.get(tokenName), provider);
						} catch (Exception e) {
                            e.printStackTrace();
                            sender.spigot().sendMessage(new ComponentBuilder("§cSomething went wrong").create());
                        }                        
                    }
                    String format = (String) context.getSessionData("format");
                    TextComponent availableBalance = new TextComponent("§eAvailable "+format+tokenName+" §ebalance: §f"+balance.toPlainString());
                    TextComponent useMaxBalance = new TextComponent(" §9§nMax");
                    useMaxBalance.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, balance.toPlainString()));
                    availableBalance.addExtra(useMaxBalance);
                    sender.spigot().sendMessage(availableBalance);
                    return "§bEnter amount of "+format+tokenName+" §bto send";
                };
                @Override
                protected boolean isNumberValid(ConversationContext context, Number input) {
                    return input.doubleValue()>0 && new BigDecimal(input.toString()).compareTo((BigDecimal)context.getSessionData("balance"))<=0;
                }
                @Override
                protected String getFailedValidationText(ConversationContext context, Number input){
                    return "§cInvalid token amount";
                }
                @Override
                protected Prompt acceptValidatedInput(ConversationContext context, Number input) {
                    BigDecimal amount = new BigDecimal("0");
                    String tokenName = (String) context.getSessionData("token");
                    if (tokenName.equals("ZIL")){
                        amount = new BigDecimal(input.toString()).setScale(12);
                    }else if(!userTokens.containsKey(tokenName)){
                        try {
                            Integer decimals = 0;
                            if(network.equals("mainnet")){
                                decimals = WalletUtil.getDecimals(mainnetTokenAddresses.get(tokenName), provider);
                            }else{
                                decimals = WalletUtil.getDecimals(testnetTokenAddresses.get(tokenName), provider);
                            }                            
                            amount = new BigDecimal(input.toString()).setScale(decimals);
						} catch (Exception e) {
                            e.printStackTrace();
                            sender.spigot().sendMessage(new ComponentBuilder("§cSomething went wrong").create());
						}
                    }else{
                        try {
                            Integer decimals = 0;
                            decimals = WalletUtil.getDecimals(userTokens.get(tokenName), provider);                          
                            amount = new BigDecimal(input.toString()).setScale(decimals);
						} catch (Exception e) {
                            e.printStackTrace();
                            sender.spigot().sendMessage(new ComponentBuilder("§cSomething went wrong").create());
						}
                    }
                    context.setSessionData("amount", amount);                            
                    return confirmTransaction;
                };
            };
            Prompt askGasAmount = new NumericPrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    String tokenName=(String) context.getSessionData("token");
                    BigDecimal balance = new BigDecimal("0");
                    try {
                        balance = WalletUtil.getZILBalance(senderBech32address, provider);
                    } catch (Exception e) {
                        e.printStackTrace();
                        sender.spigot().sendMessage(new ComponentBuilder("§cFailed to fetch §3ZIL §cbalance").create());
                    }
                    context.setSessionData("ZILbalance", balance);
                    TextComponent availableZILBalance = new TextComponent("§eAvailable §3ZIL §ebalance: §f"+balance.toPlainString());
                    sender.spigot().sendMessage(availableZILBalance);
                    if(tokenName.equals("ZIL")){
                        context.setSessionData("minimum", new BigDecimal("0.002"));
                        TextComponent availableBalance = new TextComponent("§eSuggested gas amounts (Minimum §f0.002 §3ZIL§e)");
                        TextComponent average = new TextComponent("§bAverage§f-§9§n0.002§3 ZIL  ");
                        average.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "0.002"));
                        TextComponent fast = new TextComponent("§dFast§f-§9§n0.004§3 ZIL  ");
                        fast.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "0.004"));
                        TextComponent fantastic = new TextComponent("§6Fantastic§f-§9§n0.006§3 ZIL");
                        fantastic.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "0.006"));
                        average.addExtra(fast);
                        average.addExtra(fantastic);
                        sender.spigot().sendMessage(availableBalance);
                        sender.spigot().sendMessage(average);  
                    }
                    else{
                        context.setSessionData("minimum", new BigDecimal("9"));
                        TextComponent availableBalance = new TextComponent("§eSuggested gas amounts (Minimum §f9 §3ZIL§e)");
                        TextComponent average = new TextComponent("§bAverage§f-§9§n9§3 ZIL  ");
                        average.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "9"));
                        TextComponent fast = new TextComponent("§dFast§f-§9§n18§3 ZIL  ");
                        fast.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "18"));
                        TextComponent fantastic = new TextComponent("§6Fantastic§f-§9§n27§3 ZIL");
                        fantastic.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "27"));
                        average.addExtra(fast);
                        average.addExtra(fantastic);
                        sender.spigot().sendMessage(availableBalance);
                        sender.spigot().sendMessage(average);
                    }
                    return "§bEnter maximum amount of gas (in §3ZIL§b) to use for sending tokens";
                };
                @Override
                protected boolean isNumberValid(ConversationContext context, Number input) {
                    BigDecimal inputNumber = new BigDecimal(input.toString());
                    return inputNumber.compareTo((BigDecimal)context.getSessionData("minimum"))>=0 && inputNumber.compareTo((BigDecimal)context.getSessionData("ZILbalance"))<=0;
                }
                @Override
                protected String getFailedValidationText(ConversationContext context, Number input){
                    return "§cInvalid gas amount";
                }
                @Override
                protected Prompt acceptValidatedInput(ConversationContext context, Number input) {    
                    BigDecimal gas = new BigDecimal(input.toString());
                    context.setSessionData("gas", gas);
                    return askAmount;
                };
            };
            Prompt askTokenName = new ValidatingPrompt() {
                @Override
                public String getPromptText(ConversationContext context) {
                    String tokenList = "";
                    for(String name:tokenNames){
                        tokenList = tokenList+", "+tokenFormatting.get(name)+name;
                    }
                    for (String name:userTokens.keySet()) {
                        tokenList = tokenList+", §6"+name;
                    }
                    sender.spigot().sendMessage(new ComponentBuilder("§eAvailable token names: §3ZIL"+tokenList).create());
                    return "§bEnter name of token to send";
                }
                @Override
                protected boolean isInputValid(ConversationContext context, String input){
                    return input.equals("ZIL")||tokenNames.contains(input)||userTokens.keySet().contains(input);
                }
                @Override
                protected String getFailedValidationText(ConversationContext context, String input){
                    return "§cInvalid token name";
                }
                @Override
                protected Prompt acceptValidatedInput(ConversationContext context, String input) {
                    context.setSessionData("token", input);
                    if(tokenFormatting.containsKey(input)){
                        context.setSessionData("format", tokenFormatting.get(input));
                    }else{
                        context.setSessionData("format", "§6");
                    }                    
                    return askGasAmount;
                }
            };
            class onCancel implements ConversationAbandonedListener {
                @Override
                public void conversationAbandoned(ConversationAbandonedEvent event) {
                    if(!event.gracefulExit()){
                        event.getContext().getForWhom().sendRawMessage("§7§oToken sending cancelled");
                    }                    
                }
            }
            if (!WalletUtil.isUnlocked(senderBech32address)) {
                sender.sendMessage("§cPlease unlock your active account with §a/zilcraft unlock "+activeAccountName);
            } else if(username.equals(arg)||WalletUtil.getActiveAddress(username).equals(arg)) {
                sender.sendMessage("§cYou can't send tokens to your active account");
            } else if (WalletUtil.getUserList().contains(arg)) {
                if (WalletUtil.getUserAccounts(arg).isEmpty()) {
                    if (isOnline) {
                        Player receiver = Bukkit.getPlayer(arg);
                        receiver.sendMessage("§f" + sender.getDisplayName()
                                + " §eis trying to send you tokens but you do not have an account, create one with §a/zilcraft account create");
                    }
                    sender.sendMessage("§f" + arg
                            + " §edoes not have any accounts, let them know they can get started with §a/zilcraft account create");
                } else {
                    sender.sendMessage("§6§lToken sending wizard");
                    sender.sendMessage("§eType §acancel§e to abort at any point");
                    sender.sendMessage("§eTokens will be sent from your active account §b" + activeAccountName + " §eto §f" + arg + "§e's active account");
                    if (!isOnline) {
                        sender.sendMessage("§f" + arg + " §eis offline, make sure you are sending to the correct player");
                    }
                    Map<Object,Object> init = new HashMap<>();
                    init.put("isAccount",true);
                    ConversationFactory factory = new ConversationFactory(plugin).withFirstPrompt(askTokenName)
                    .withEscapeSequence("cancel").withLocalEcho(false).withModality(true).withInitialSessionData(init)
                    .addConversationAbandonedListener(new onCancel()).withTimeout(120);
                    factory.buildConversation(sender).begin();
                }
            } else if (Validation.isBech32(arg)) {
                sender.sendMessage("§6§lToken sending wizard");
                sender.sendMessage("§eType §acancel§e to abort at any point");
                sender.sendMessage("§eTokens will be sent from your active account §b" + activeAccountName + " §eto §3" + arg);
                Map<Object,Object> init = new HashMap<>();
                init.put("isAccount",false);
                ConversationFactory factory = new ConversationFactory(plugin).withFirstPrompt(askTokenName)
                .withEscapeSequence("cancel").withLocalEcho(false).withModality(true).withInitialSessionData(init)
                .addConversationAbandonedListener(new onCancel()).withTimeout(120);
                factory.buildConversation(sender).begin();
            } else {
                sender.sendMessage("§cInvalid account name or Zilliqa address");
            }
        }
    }

    @Subcommand("token|tkn")
    @CommandPermission("zilcraft.token")
    public class Token extends BaseCommand {
        @Subcommand("add")
        @Description("Add token by contract address")
        @CommandPermission("zilcraft.token.add")
        @Syntax("<mainnet | testnet> <contract address>")
        @CommandCompletion("@full_network_list")
        public void onZCTokenAdd(Player sender, @Values("@full_network_list") String net, @Single String contractAddress) {
            if(Validation.isBech32(contractAddress)){
                try {
                    String name = WalletUtil.addToken(sender, net, contractAddress);
                    sender.sendMessage("§aToken §6"+name+" §asuccessfully added");
                } catch (Exception e) {
                    sender.sendMessage("§cInvalid token contract address");
                }
            }else{
                sender.sendMessage("§cInvalid token contract address");
            }
        }

        @Subcommand("remove")
        @Description("Remove added token(s)")
        @CommandPermission("zilcraft.token.remove")
        @Syntax("<mainnet | testnet> <token name(s)>")
        @CommandCompletion("@full_network_list")
        public void onZCTokenRemove(Player sender, @Values("@full_network_list") String net, String[] names) throws Exception {
            for(String name:names){
                if(WalletUtil.removeToken(sender, network, name)){
                    sender.sendMessage("§aToken §6"+name+" §asuccessfully removed");
                }else{
                    sender.sendMessage("§cInvalid token name (case sensitive)");
                }
            }
        }

        @Subcommand("list")
        @Description("List added tokens")
        @CommandPermission("zilcraft.token.list")
        public void onZCTokenList(Player sender) throws Exception {
            String username = sender.getName();
            Map<String,String> mainnetTokens = WalletUtil.getUserTokens(username, "mainnet");
            Map<String,String> testnetTokens = WalletUtil.getUserTokens(username, "testnet");
            sender.sendMessage("§3§lListing added tokens");
            sender.sendMessage("§b"+nameMap.get("mainnet")+" §etokens");
            if (mainnetTokens.isEmpty()) {
                sender.sendMessage("§eNo §b"+nameMap.get("mainnet")+" §etokens added");
            }
            for (Map.Entry<String, String> entry : mainnetTokens.entrySet()) {
                String tokenName = entry.getKey();
                String tokenAddress = entry.getValue();
                TextComponent name = new TextComponent("§6" + tokenName + " ");
                TextComponent address = new TextComponent("§3" + tokenAddress+" ");
                address.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Copy token address to clipboard")));
                address.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, tokenAddress));
                name.addExtra(address);
                TextComponent viewBlockLink = new TextComponent("§9§nViewBlock");
                viewBlockLink.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("View on ViewBlock")));
                viewBlockLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,"https://viewblock.io/zilliqa/address/"+tokenAddress));
                name.addExtra(viewBlockLink);
                sender.spigot().sendMessage(name);
            }
            sender.sendMessage("§b"+nameMap.get("testnet")+" §etokens");
            if (testnetTokens.isEmpty()) {
                sender.sendMessage("§eNo §b"+nameMap.get("testnet")+" §etokens added");
            }
            for (Map.Entry<String, String> entry : testnetTokens.entrySet()) {
                String tokenName = entry.getKey();
                String tokenAddress = entry.getValue();
                TextComponent name = new TextComponent("§6" + tokenName + " ");
                TextComponent address = new TextComponent("§3" + tokenAddress+" ");
                address.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Copy token address to clipboard")));
                address.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, tokenAddress));
                name.addExtra(address);
                TextComponent viewBlockLink = new TextComponent("§9§nViewBlock");
                viewBlockLink.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("View on ViewBlock")));
                viewBlockLink.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,"https://viewblock.io/zilliqa/address/"+tokenAddress+"?network=testnet"));
                name.addExtra(viewBlockLink);
                sender.spigot().sendMessage(name);
            }
        }
    }
}