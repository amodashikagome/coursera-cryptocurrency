import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        Set<UTXO> claimedUTXO = new HashSet<UTXO>();
        double totalInputValue = 0;
        double totalOutputValue = 0;
        for (int i = 0; i < tx.getInputs().size(); i++) {
            Transaction.Input in = tx.getInputs().get(i);
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
            // (1)
            if (!utxoPool.contains(utxo)) {
                return false;
            }
            // (2)
            Transaction.Output previousOp = utxoPool.getTxOutput(utxo);
            if (!Crypto.verifySignature(previousOp.address, tx.getRawDataToSign(i), in.signature)) {
                return false;
            };
            // (3)
            if (claimedUTXO.contains(utxo)) {
                return false;
            }
            claimedUTXO.add(utxo);
            totalInputValue += previousOp.value;
        }
        for (int i = 0; i < tx.getOutputs().size(); i++) {
            Transaction.Output op = tx.getOutputs().get(i);
            // (4)
            if (op.value < 0) {
                return false;
            }
            totalOutputValue += op.value;
        }
        // (5)
        if (totalInputValue < totalOutputValue) {
            return false;
        }
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> validTxs = new ArrayList<Transaction>();
        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                validTxs.add(tx);
                for (int i = 0; i < tx.getInputs().size(); i++) {
                    Transaction.Input in = tx.getInputs().get(i);
                    UTXO usedUtxo = new UTXO(in.prevTxHash, in.outputIndex);
                    utxoPool.removeUTXO(usedUtxo);
                }
                for (int i = 0; i < tx.getOutputs().size(); i++) {
                    Transaction.Output op = tx.getOutputs().get(i);
                    UTXO newUtxo = new UTXO(tx.getHash(), i);
                    utxoPool.addUTXO(newUtxo, op);
                }
            }
        }
        return validTxs.toArray(new Transaction[validTxs.size()]);
    }

    public UTXOPool getUTXOPool() {
        return utxoPool;
    }

}
