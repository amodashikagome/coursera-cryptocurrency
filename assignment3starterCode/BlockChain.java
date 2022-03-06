// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    private int maxHeight;
    // preserver insertion order to support picking the oldest block 
    private HashMap<byte[], Integer> blockHeight = new LinkedHashMap<byte[], Integer>();
    private HashMap<byte[], Block> blockMap = new HashMap<byte[], Block>();
    private HashMap<byte[], UTXOPool> utxoPoolMap = new HashMap<byte[], UTXOPool>();
    private TransactionPool txPool = new TransactionPool();

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        blockMap.put(genesisBlock.getHash(), genesisBlock);
        blockHeight.put(genesisBlock.getHash(), 1);
        maxHeight = 1;
        UTXOPool utxoPool = new UTXOPool();
        addCoinBase(genesisBlock, utxoPool);
        utxoPoolMap.put(genesisBlock.getHash(), utxoPool);
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        for (Map.Entry<byte[], Integer> entry : blockHeight.entrySet()) {
            if (entry.getValue() == maxHeight) {
                return blockMap.get(entry.getKey());
            }

        }
        return null;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        return utxoPoolMap.get(getMaxHeightBlock().getHash());
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return txPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        if (block.getPrevBlockHash() == null || !blockMap.containsKey(block.getPrevBlockHash())) {
            return false;
        }
        if (blockHeight.get(block.getPrevBlockHash()) + 1 <= maxHeight - CUT_OFF_AGE) {
            return false;
        }

        // coin spend txs
        TxHandler txHandler = new TxHandler(utxoPoolMap.get(block.getPrevBlockHash()));
        Transaction[] outputs = txHandler.handleTxs(block.getTransactions().toArray(new Transaction[0]));
        // not all transactions are valid
        if (outputs.length != block.getTransactions().size()) {
            return false;
        }
        UTXOPool utxoPool = txHandler.getUTXOPool();

        // coin base tx
        addCoinBase(block, utxoPool);

        // remove txs
        for (Transaction tx : block.getTransactions()) {
             txPool.removeTransaction(tx.getHash());
        }
        // add block
        blockMap.put(block.getHash(), block);
        blockHeight.put(block.getHash(), blockHeight.get(block.getPrevBlockHash()) + 1);
        maxHeight = Math.max(maxHeight, blockHeight.get(block.getPrevBlockHash()) + 1);
        utxoPoolMap.put(block.getHash(), utxoPool);

        return true;
    }

    private void addCoinBase(Block block, UTXOPool utxoPool) {
        Transaction coinbse = block.getCoinbase();
        for (int i = 0; i < coinbse.getOutputs().size(); i++) {
            Transaction.Output op = coinbse.getOutputs().get(i);
            UTXO newUtxo = new UTXO(coinbse.getHash(), i);
            utxoPool.addUTXO(newUtxo, op);
        }
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        txPool.addTransaction(tx);
    }
}