import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {

    private final double p_graph;
    private final double p_malicious;
    private final double p_txDistribution;
    private final int numRounds;
    private int currentRound = 0;
    private Set<Transaction> sentTxs = new HashSet<>();
    private Set<Transaction> receivedTxs =  new HashSet<>();
    private HashMap<Transaction, Integer> receivedTxsCount = new HashMap<>();

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        this.p_graph = p_graph;
        this.p_malicious = p_malicious;
        this.p_txDistribution = p_txDistribution;
        this.numRounds = numRounds;
    }

    public void setFollowees(boolean[] followees) {
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        for (Transaction tx : pendingTransactions) {
            receivedTxsCount.put(tx, receivedTxsCount.getOrDefault(tx, 0) + 1);
        }
    }

    public Set<Transaction> sendToFollowers() {
        if (currentRound < numRounds) {
            Set<Transaction> toSend = new HashSet<>();
            // for (Transaction tx : receivedTxs) {
            for (Transaction tx : receivedTxsCount.keySet()) {
                if (!sentTxs.contains(tx)) {
                    toSend.add(tx);
                    sentTxs.add(tx);
                }
            }
            currentRound++;
            return toSend;
        } else {
            Set<Transaction> txs = new HashSet<>();
            for (Transaction tx : receivedTxsCount.keySet()) {
                if (receivedTxsCount.get(tx) > 1) {
                    txs.add(tx);
                }
            }
            return txs;
        }
        
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        for (Candidate candidate : candidates) {
            Transaction tx = candidate.tx;
            receivedTxsCount.put(tx, receivedTxsCount.getOrDefault(tx, 0) + 1);
        }
    }
}
