package com.blocksim;

import com.blocksim.utils.Utils;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.atomic.AtomicLong;

public class Transaction {
    private static AtomicLong sequence = new AtomicLong(0);
    String txId;
    private PublicKey sender;
    private PublicKey recipient;
    //	double value;
    private byte[] signature;
    private String data;
    // TODO: inputs, outputs, value.

    public Transaction(PublicKey sender, PublicKey reciepient, PrivateKey pr) throws Exception {
        this.sender = sender;
        this.recipient = reciepient;
        txId = Utils.hash(Utils.getStringFromKey(sender) + Utils.getStringFromKey(reciepient)
                + sequence.getAndIncrement());
        data = Utils.getStringFromKey(sender) + Utils.getStringFromKey(reciepient);
        genSignature(pr);
    }

    private static boolean verifySignature(Transaction tx) throws Exception {
        return Utils.verifyECDSASig(tx.sender, tx.data, tx.signature);
    }

    public static boolean verify(Transaction tx) throws Exception {
        // TODO: verify sender's available funds.
        return verifySignature(tx);
    }

    /**
     * Private key is not saved in the transaction.
     * The transaction creator passes it to calculate their signature and then sends it
     * to the network, thus no one knows their private key.
     * @param pr
     * @throws Exception
     */
    private void genSignature(PrivateKey pr) throws Exception {
        signature = Utils.applyECDSASig(pr, data);
    }
}
