package dodecane2242.zilcraft;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import com.firestack.laksaj.account.Account;
import com.firestack.laksaj.blockchain.Contract.State;
import com.firestack.laksaj.contract.CallParams;
import com.firestack.laksaj.contract.ContractFactory;
import com.firestack.laksaj.contract.Value;
import com.firestack.laksaj.crypto.Schnorr;
import com.firestack.laksaj.crypto.Signature;
import com.firestack.laksaj.jsonrpc.HttpProvider;
import com.firestack.laksaj.transaction.Transaction;
import com.firestack.laksaj.transaction.TxStatus;
import com.google.gson.Gson;

public class CustomContract extends com.firestack.laksaj.contract.Contract {
    private Account account;

    public CustomContract(ContractFactory factory, String code, String abi, String address, Value[] init, List<State> state) throws Exception {
        super(factory, code, abi, address, init, state);
    }
    
    @Override
    public HttpProvider.CreateTxResult prepareTx(Transaction tx) throws Exception {
        tx = sign(tx);
        HttpProvider.CreateTxResult createTxResult = getProvider().createTransaction(tx.toTransactionPayload()).getResult();
        return createTxResult;
    }

    @Override
    public Transaction prepareTx(Transaction tx, int attempts, int interval) throws Exception {
        tx = sign(tx);
        try {
            HttpProvider.CreateTxResult createTxResult = getProvider().createTransaction(tx.toTransactionPayload()).getResult();
            tx.confirm(createTxResult.getTranID(), attempts, interval);
        } catch (IOException e) {
            e.printStackTrace();
            tx.setStatus(TxStatus.Rejected);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return tx;
    }

    public void setAccount (Account account){
        this.account=account;
    }

    private static class data {
        private String _tag;
        private Value[] params;
        data(String transition, Value[] args){
            _tag = transition;
            params = args;
        }
    }

    public HttpProvider.CreateTxResult callWithoutConfirm(String transition, Value[] args, CallParams params) throws Exception {
        if (null == this.getAddress() || this.getAddress().isEmpty()) {
            throw new IllegalArgumentException("Contract has not been deployed!");
        }
        data data = new data(transition,args);
        Gson gson = new Gson();
        Transaction transaction = Transaction.builder()
                .ID(params.getID())
                .version(params.getVersion())
                .nonce(params.getNonce())
                .amount(params.getAmount())
                .gasPrice(params.getGasPrice())
                .gasLimit(params.getGasLimit())
                .senderPubKey(params.getSenderPubKey())
                .data(gson.toJson(data))
                .provider(this.getProvider())
                .toAddr(this.getAddress())
                .code(this.getCode().replace("/\\", ""))
                .build();
        return this.prepareTx(transaction);
    }
    
    private Transaction sign(Transaction tx) throws IOException {
        Account signer = account;
        HttpProvider provider = getProvider();
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
}
