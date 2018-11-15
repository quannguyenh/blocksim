package com.blocksim;

import java.util.concurrent.CopyOnWriteArrayList;

public class INode {
    private volatile CopyOnWriteArrayList<Transaction> txPool;
    private volatile CopyOnWriteArrayList<Block> blockBuffer;

    public INode(CopyOnWriteArrayList<Transaction> txPool, CopyOnWriteArrayList<Block> blockBuffer) {
        this.txPool = txPool;
        this.blockBuffer = blockBuffer;
    }

    public void addTx(Transaction tx) {
        txPool.add(tx);
    }

    public void addBlock(Block b) {
        blockBuffer.add(b);
    }
}
