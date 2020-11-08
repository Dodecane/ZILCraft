package dodecane2242.zilcraft;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.firestack.laksaj.account.Account;
import com.firestack.laksaj.account.Wallet;
import com.firestack.laksaj.blockchain.Contract;
import com.firestack.laksaj.blockchain.Contract.State;
import com.firestack.laksaj.contract.CallParams;
import com.firestack.laksaj.contract.ContractFactory;
import com.firestack.laksaj.contract.Value;
import com.firestack.laksaj.crypto.KDFType;
import com.firestack.laksaj.crypto.KeyTools;
import com.firestack.laksaj.crypto.Schnorr;
import com.firestack.laksaj.crypto.Signature;
import com.firestack.laksaj.jsonrpc.HttpProvider;
import com.firestack.laksaj.transaction.Transaction;
import com.firestack.laksaj.transaction.TransactionFactory;
import com.firestack.laksaj.utils.Bech32;
import com.google.gson.JsonParser;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class WalletUtil implements Listener {
    private static BigDecimal qa = new BigDecimal("1000000000000");
    private static Map<String, Map<String, Account>> userWallets = new HashMap<>();
    private static Map<String, Map<String, String>> userAccounts = new HashMap<>();
    private static Map<String, String> addressKeystores = new HashMap<>();
    private static Map<String, String> activeAccounts = new HashMap<>();
    private static Map<String, String> publicKeys = new HashMap<>();
    private static Map<String, Map<String, String>> mainnetUserTokens = new HashMap<>();
    private static Map<String, Map<String, String>> testnetUserTokens = new HashMap<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String username = event.getPlayer().getName();
        if (!userWallets.containsKey(username)) {
            Map<String, Account> newWallet = new HashMap<>();
            userWallets.put(username, newWallet);
        }
        if (!userAccounts.containsKey(username)) {
            Map<String, String> newUserAccounts = new HashMap<>();
            userAccounts.put(username, newUserAccounts);
        }
        if (!activeAccounts.containsKey(username)){
            activeAccounts.put(username,"");
        }
        if (!mainnetUserTokens.containsKey(username)) {
            Map<String, String> newUserTokens = new HashMap<>();
            mainnetUserTokens.put(username, newUserTokens);
        }
        if (!testnetUserTokens.containsKey(username)) {
            Map<String, String> newUserTokens = new HashMap<>();
            testnetUserTokens.put(username, newUserTokens);
        }
    }

    public static String addAccount(Player sender, String name, String password, String privateKey) throws Exception {
        String username = sender.getName();
        Map<String, Account> wallet = userWallets.get(username);
        Account account = new Account(privateKey);
        wallet.put(name,account);
        Map<String, String> accounts = userAccounts.get(username);
        String bech32address = Bech32.toBech32Address(KeyTools.getAddressFromPrivateKey(privateKey));
        accounts.put(name, bech32address);
        String keystore = account.toFile(privateKey, password, KDFType.PBKDF2);
        addressKeystores.put(bech32address, keystore);
        publicKeys.put(bech32address,account.getPublicKey());
        activeAccounts.put(username,name);
        saveData();
        return bech32address;
    }

    public static boolean unlockAccount(Player sender, String name, String password) {
        String username = sender.getName();
        Map<String, Account> wallet = userWallets.get(username);
        Map<String, String> accounts = userAccounts.get(username);
        String bech32address = accounts.get(name);
        String keystoreString = addressKeystores.get(bech32address);       
        try {
            Account account = Account.fromFile(keystoreString, password);
            wallet.put(name,account);
            activeAccounts.put(username,name);
            String privateKey = KeyTools.decryptPrivateKey(keystoreString, password);
            publicKeys.put(bech32address,KeyTools.getPublicKeyFromPrivateKey(privateKey,false));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean lockAccounts(Player sender) {
        String username = sender.getName();
        Map<String, Account> newWallet = userWallets.get(username);
        userWallets.put(username, newWallet);
        Map<String, String> accounts = userAccounts.get(username);
        for (Map.Entry<String, String> entry : accounts.entrySet()) {
            publicKeys.remove(entry.getValue());
        }
        return true;
    }

    public static boolean setDefaultAccount(Player sender, String name) {
        String username = sender.getName();
        activeAccounts.put(username,name);
        saveData();
        return true;
    }

    public static boolean renameAccount(Player sender, String oldName, String newName) {
        String username = sender.getName();
        Map<String, Account> wallet = userWallets.get(username);
        Map<String, String> accounts = userAccounts.get(username);
        wallet.put(newName,wallet.remove(oldName));
        accounts.put(newName,accounts.remove(oldName));
        activeAccounts.put(username,newName);
        saveData();
        return true;
    }

    public static boolean removeAccount(Player sender, String name) {
        String username = sender.getName();
        Map<String, Account> wallet = userWallets.get(username);
        Map<String, String> accounts = userAccounts.get(username);
        if(activeAccounts.get(username).equals(name)){
            return false;
        }
        String bech32address = accounts.get(name);
        wallet.remove(name);
        accounts.remove(name);
        addressKeystores.remove(bech32address);
        publicKeys.remove(bech32address);
        saveData();
        return true;
    }

    public static boolean removeAllAccounts(Player sender) {
        String username = sender.getName();
        Map<String, Account> newWallet = new HashMap<>();
        userWallets.put(username, newWallet);
        activeAccounts.put(username,"");
        Map<String, String> accounts = userAccounts.get(username);
        addressKeystores.keySet().removeAll(accounts.values());
        publicKeys.keySet().removeAll(accounts.values());
        accounts.clear();
        saveData();
        return true;
    }

    public static String addToken(Player sender, String network, String contractAddress) throws Exception {
        String username = sender.getName();
        String tokenName = "";
        if(network.equals("mainnet")){
            HttpProvider mainnetProvider = new HttpProvider(ZILCraft.apiURLMap.get(network));
            Map<String, String> tokenList = mainnetUserTokens.get(username);
            tokenName = getTokenName(contractAddress, mainnetProvider);
            tokenList.put(tokenName,contractAddress);
        } else {
            HttpProvider testnetProvider = new HttpProvider(ZILCraft.apiURLMap.get(network));
            Map<String, String> tokenList = testnetUserTokens.get(username);
            tokenName = getTokenName(contractAddress, testnetProvider);
            tokenList.put(tokenName,contractAddress);
        }      
        saveData();
        return tokenName;
    }

    public static Boolean removeToken(Player sender, String network, String name) throws Exception {
        String username = sender.getName();
        if(!getUserTokens(username, network).containsKey(name)){
            return false;
        }
        if(network.equals("mainnet")){
            Map<String, String> tokenList = mainnetUserTokens.get(username);
            tokenList.remove(name);
        }else{
            Map<String, String> tokenList = testnetUserTokens.get(username);
            tokenList.remove(name);
        }
        saveData();
        return true;
    }

    public static HttpProvider.CreateTxResult sendZIL(Player sender, String receiverBech32address, BigDecimal amount, BigDecimal gas, HttpProvider provider, Integer chainID) throws Exception {
        String username = sender.getName();
        String senderBech32address = getActiveAddress(username);
        String senderPublicKey = publicKeys.get(senderBech32address);
        Transaction transaction = Transaction.builder()
        .version(String.valueOf(Wallet.pack(chainID, 1)))
        .toAddr(Bech32.fromBech32Address(receiverBech32address))
        .senderPubKey(senderPublicKey)
        .amount(amount.multiply(qa).setScale(0).toPlainString())
        .gasPrice(gas.multiply(qa).setScale(0).toPlainString())
        .gasLimit("1")
        .code("")
        .data("")
        .provider(provider)
        .build();
        Account account = userWallets.get(username).get(getActiveAccount(username));
        transaction = sign(transaction,account,provider);
        HttpProvider.CreateTxResult result = TransactionFactory.createTransaction(transaction);
        return result;
    }

    public static HttpProvider.CreateTxResult sendToken(Player sender, String receiverBech32address, BigDecimal amount, BigDecimal gas, String contractBech32address, HttpProvider provider, Integer chainID) throws Exception {
        String username = sender.getName();
        String senderBech32address = getActiveAddress(username);
        String senderPublicKey = publicKeys.get(senderBech32address);
        String receiverAddress = "0x"+Bech32.fromBech32Address(receiverBech32address);
        Account account = userWallets.get(username).get(getActiveAccount(username));
        List<Value> init = Arrays.asList();
        List<State> temp = Arrays.asList();
        Wallet throwawayWallet = new Wallet();
        ContractFactory factory = ContractFactory.builder().provider(provider).signer(throwawayWallet).build();
        CustomContract contract = new CustomContract(factory, "", "", Bech32.fromBech32Address(contractBech32address), (Value[]) init.toArray(), temp);
        contract.setAccount(account);
        Integer nonce = Integer.valueOf(factory.getProvider().getBalance(account.getAddress()).getResult().getNonce());
        CallParams params = CallParams.builder()
        .nonce(String.valueOf(nonce + 1))
        .version(String.valueOf(Wallet.pack(chainID, 1)))
        .gasPrice(gas.multiply(qa).divide(new BigDecimal("4500")).setScale(0).toPlainString())
        .gasLimit("4500")
        .senderPubKey(senderPublicKey)
        .amount("0")
        .build();
        Integer decimals = getDecimals(contractBech32address, provider);
        List<Value> values = Arrays.asList(Value.builder().vname("to").type("ByStr20").value(receiverAddress).build(), Value.builder().vname("amount").type("Uint128").value(amount.multiply(new BigDecimal("10").pow(decimals)).setScale(0).toPlainString()).build());
        HttpProvider.CreateTxResult result = contract.callWithoutConfirm("Transfer", (Value[]) values.toArray(), params);
        return result;
    }

    public static Transaction sign(Transaction tx, Account signer, HttpProvider provider) throws IOException {
        HttpProvider.BalanceResult result;
        if (Objects.isNull(signer)) {
            throw new IllegalArgumentException("Account does not exist");
        }
        if (Objects.isNull(tx.getNonce()) || tx.getNonce().isEmpty()) {
            try {
                result = provider.getBalance(signer.getAddress()).getResult();
                tx.setNonce(String.valueOf(Integer.valueOf(result.getNonce()) + 1));
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to get nonce", e);
            }
        }
        tx.setSenderPubKey(signer.getPublicKey());
        byte[] message = tx.bytes();
        Signature signature = Schnorr.sign(signer.getKeys(), message);
        tx.setSignature(signature.toString().toLowerCase());
        return tx;
    }

    public static BigDecimal getZILBalance(String bech32address, HttpProvider provider) throws Exception {
        String address = Bech32.fromBech32Address(bech32address);
        HttpProvider.BalanceResult balanceResult = provider.getBalance(address).getResult();
        BigDecimal balance = (new BigDecimal(balanceResult.getBalance())).divide(qa).setScale(12);
        return balance;
    }

    public static BigDecimal getTokenBalance(String bech32address, String contractBech32address, HttpProvider provider) throws Exception {
        String address = "0x"+Bech32.fromBech32Address(bech32address).toLowerCase();
        String contractAddress = Bech32.fromBech32Address(contractBech32address);
        String smartContractState = provider.getSmartContractState(contractAddress);
        Integer decimals = getDecimals(contractBech32address, provider);
        JsonParser parser = new JsonParser();
        try {
            String balance = parser.parse(smartContractState).getAsJsonObject().getAsJsonObject("result").getAsJsonObject("balances").get(address).getAsString();
            return new BigDecimal(balance).divide(new BigDecimal("10").pow(decimals)).setScale(decimals);
        } catch (Exception e) {
            return new BigDecimal("0").setScale(decimals);
        }
    }

    public static Integer getDecimals(String contractBech32address, HttpProvider provider) throws Exception {
        String contractAddress = Bech32.fromBech32Address(contractBech32address);
        List<Contract.State> smartContractInit = provider.getSmartContractInit(contractAddress).getResult();
        Integer decimals = 0;
        for(Contract.State s: smartContractInit){
            if(s.getVname().equals("decimals")){
                decimals = Integer.parseInt((String)s.getValue());
            }
        }
        return decimals;
    }

    public static String getTokenName(String contractBech32address, HttpProvider provider) throws Exception {
        String contractAddress = Bech32.fromBech32Address(contractBech32address);
        List<Contract.State> smartContractInit = provider.getSmartContractInit(contractAddress).getResult();
        String name = "";
        for(Contract.State s: smartContractInit){
            if(s.getVname().equals("symbol")){
                name = (String)s.getValue();
            }
        }
        return name;
    }

    public static Map<String, String> getUserTokens(String username, String network) throws Exception {
        if(network.equals("mainnet")){
            return mainnetUserTokens.get(username);
        }else{
            return testnetUserTokens.get(username);
        }
    }

    public static String isValidKey(String privateKey) {
        try {
            if (addressKeystores.containsKey(Bech32.toBech32Address(KeyTools.getAddressFromPrivateKey(privateKey)))) {
                return "Account already added/created";
            }
            Wallet tempWallet = new Wallet();
            tempWallet.addByPrivateKey(privateKey);
        } catch (Exception e) {
            e.printStackTrace();
            return "Invalid private key";
        }
        return "valid";
    }

    public static Boolean isUnlocked(String address) {
        return publicKeys.containsKey(address);
    }

    public static Map<String, String> getUserAccounts(String username) {
        return userAccounts.get(username);
    }

    public static String getKeystore(String address) {
        return addressKeystores.get(address);
    }

    public static String getActiveAddress(String username) {        
        return userAccounts.get(username).get(activeAccounts.get(username));
    }

    public static String getActiveAccount(String username) {
        return activeAccounts.get(username);
    }

    public static Set<String> getUserList() {
        return userAccounts.keySet();
    }

    static class CustomData implements Serializable{
        private static final long serialVersionUID = -1210268165497124636L;
        private Map<String, Map<String, String>> addressMap;
        private Map<String,String> keystoreMap;
        private Map<String,String> activeMap;
        private Map<String, Map<String, String>> mainMap;
        private Map<String, Map<String, String>> testMap;
        public CustomData(Map<String,Map<String,String>> data1, Map<String,String> data2, Map<String,String> data3, Map<String,Map<String,String>> data4, Map<String,Map<String,String>> data5){
            addressMap=data1;
            keystoreMap=data2;
            activeMap=data3;
            mainMap=data4;
            testMap=data5;
        }
        public Map<String, Map<String, String>> getAddressMap() {
            return addressMap;
        }
        public Map<String, String> getKeystoreMap() {
            return keystoreMap;
        }
        public Map<String, String> getActiveMap() {
            return activeMap;
        }
        public Map<String, Map<String, String>> getMainMap() {
            return mainMap;
        }
        public Map<String, Map<String, String>> getTestMap() {
            return testMap;
        }
    }

    public static void saveData(){
        CustomData data = new CustomData(userAccounts,addressKeystores,activeAccounts,mainnetUserTokens,testnetUserTokens);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream("plugins/ZILCraft/zilcraft.data");
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream); 
            objectOutputStream.writeObject(data); 
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadData(){
        try {
            FileInputStream fileInputStream = new FileInputStream("plugins/ZILCraft/zilcraft.data");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            CustomData data = (CustomData) objectInputStream.readObject();
            userAccounts = data.getAddressMap();
            addressKeystores = data.getKeystoreMap();
            activeAccounts = data.getActiveMap();
            mainnetUserTokens = data.getMainMap();
            testnetUserTokens = data.getTestMap();
            fileInputStream.close();
            objectInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
