package com.blocksim;

import com.blocksim.utils.Utils;

import java.util.ArrayList;
import java.util.Date;

import static com.blocksim.utils.Constants.GENESIS;

public class Block {
    String hash;
    String prevHash;
    ArrayList<Transaction> data;
    int nonce = 0;
    int ttl = 0; // Incremented every time a block is added to the block chain
    int index;
    private long timestamp;

    public Block(ArrayList<Transaction> data, String prevHash) {
        this.data = data;
        this.prevHash = prevHash;
        this.timestamp = new Date().getTime();
        this.hash = calculateHash();
    }

    public Block() {
        this.hash = GENESIS;
        index = 0;
    }

    public String calculateHash() {
        return Utils.hash(prevHash + Long.toString(timestamp) + Integer.toString(nonce) + data);
    }
}
