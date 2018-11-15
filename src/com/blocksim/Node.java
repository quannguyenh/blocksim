package com.blocksim;

import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Node implements Runnable {
    private static final int CACHE_MAX_SIZE = 5000;
    private static final  int MINING_DIFFICULTY = 2;
    private static final  int TX_PER_BLOCK = 5;
    private static final  int BLOCK_CACHE_THRESH = 50;

    private final UUID id;
    // wallet can be public because the only entity that has a reference to "this"
    // is the "Seed" entity and it's part of managing the simulation, not being
    // a part of it, so it can have access to anything. Same goes for the rest of the
    // attributes.
    Wallet wallet;
    CopyOnWriteArrayList<INode> neighbors;

    private LinkedHashMap<Object, Object> cache;
    LinkedHashMap<String, Block> blockChain;
    volatile CopyOnWriteArrayList<Transaction> txPool;
    volatile CopyOnWriteArrayList<Block> blockBuffer;
    private int prevTxPoolSize, prevBlockBufferSize;
    private float sendSuccessProbability = 0.8f;
    private float createTxProbability = 0.5f;

    private Block curr = null;
    private String target = new String(new char[MINING_DIFFICULTY]).replace('\0', '0');
    String prevHash = null;
    private int index = 0; // Index of current existing last block in the block chain.

    private Random rndm = new Random();

    @SuppressWarnings({"serial", "unchecked"})
    public Node(UUID id) throws Exception {
        cache = new LinkedHashMap<>(CACHE_MAX_SIZE, 0.75F, true) {
            protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
                return size() > CACHE_MAX_SIZE;
            }
        };

        this.id = id;
        txPool = new CopyOnWriteArrayList<>();
        blockBuffer = new CopyOnWriteArrayList<>();

        wallet = new Wallet();

        prevTxPoolSize = 0;
        prevBlockBufferSize = 0;

        ArrayList<Object> res = Seed.addNeighbors(this);
        neighbors = (CopyOnWriteArrayList<INode>) res.get(0);
        blockChain = (LinkedHashMap<String, Block>) res.get(1);
    }

    // For the initial nodes.
    public Node(UUID id, boolean garbage) {
        cache = new LinkedHashMap<>(CACHE_MAX_SIZE, 0.75F, true) {
            protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
                return size() > CACHE_MAX_SIZE;
            }
        };

        this.id = id;
        txPool = new CopyOnWriteArrayList<>();
        blockBuffer = new CopyOnWriteArrayList<>();
        try {
            wallet = new Wallet();
        } catch (Exception e) {
            e.printStackTrace();
        }

        prevTxPoolSize = 0;
        prevBlockBufferSize = 0;
    }

    @Override
    public void run() {
        while (true) {
            try {
                processTxs();

                double num = rndm.nextDouble();
                if (num <= createTxProbability) {
                    createTx();
                }

                processBlocks();

                if (prevTxPoolSize >= TX_PER_BLOCK && curr == null) { // One block at a time
                    createBlock();
                }

                if (curr != null) {
                    mineBlock();
                }

                System.out.println(this.blockBuffer);
//				System.out.println(blockChain);
            } catch (Exception e) {
                System.out.println("ABORT");
                e.printStackTrace();
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void createBlock() {
        ArrayList<Transaction> txList = new ArrayList<>();
        for (int i = 0; i < 5; ++i) {
            txList.add(txPool.remove(0));
        }

        curr = new Block(txList, prevHash);
        prevTxPoolSize -= 5;
    }

    // Only the 'this' has reference to their wallet.
    // The rest of the nodes only have references to node interface.
    // Thus no nodes can know or use the private keys of other nodes.
    private void createTx() throws Exception {
        PublicKey recipient = Seed.getRandomAddress();
        Transaction tx = new Transaction(wallet.pb, recipient, wallet.pr);
//		System.out.println("Created a tx");
        txPool.add(tx);
//		cache.put(tx.txId, null);
        sendTx(tx);
    }

    private void sendTx(Transaction tx) {
        for (INode neighbor : neighbors) {
            double num = rndm.nextDouble();
            if (num <= sendSuccessProbability) {
                neighbor.addTx(tx);
            }
        }
    }

    private void sendBlock(Block b) {
        for (INode neighbor : neighbors) {
            double num = rndm.nextDouble();
            if (num <= sendSuccessProbability) {
                neighbor.addBlock(b);
            }
        }
    }

    private void processTxs() throws Exception {
        if (txPool.size() != prevTxPoolSize && txPool.size() != 0) { // Received new txs
            ArrayList<Integer> toBeRemoved = new ArrayList<>();
            for (int i = txPool.size() - 1; i >= prevTxPoolSize; --i) {
                if (cache.containsKey(txPool.get(i).txId)) { // Duplicate sending
//					System.out.println("Duplicate tx sent to node: " + this.id.toString());
//					txPool.remove(i);
                    toBeRemoved.add(i);
                } else {
                    if (!Transaction.verify(txPool.get(i))) {
                        txPool.remove(i);
                        toBeRemoved.add(i);
                        System.out.println("Removed invalid tx");
                    } else {
                        cache.put(txPool.get(i).txId, null); // Hashset didn't work for some reason. Don't judge.
                        sendTx(txPool.get(i));
                    }
                }
            }

            for (int i : toBeRemoved) {
                txPool.remove(i);
            }

            prevTxPoolSize = txPool.size();
        }

    }

    private void processBlocks() {
        boolean addedABlock = false;
        if (blockBuffer.size() != prevBlockBufferSize && blockBuffer.size() != 0) {
            ArrayList<Integer> toBeRemoved = new ArrayList<>();
            ArrayList<Block> newBlocks = new ArrayList<>();
            for (int i = blockBuffer.size() - 1; i >= prevBlockBufferSize; --i) {
                if (cache.containsKey(blockBuffer.get(i).hash)) { // Duplicate sending
//					System.out.println("Duplicate block sent to node: " + this.id.toString());
                    toBeRemoved.add(i);
                } else {
                    if ((!blockBuffer.get(i).hash.equals(blockBuffer.get(i).calculateHash()))
                            && (!blockBuffer.get(i).prevHash.equals(prevHash) || !(complementsBlockBuffer(blockBuffer.get(i))))
                            && (!curr.hash.substring(0, MINING_DIFFICULTY).equals(target))) {
                        toBeRemoved.add(i);
                        System.out.println("Removed invalid block");
                    } else {
                        if (blockBuffer.get(i).prevHash.equals(prevHash) && !addedABlock) {
                            blockChain.put(blockBuffer.get(i).hash, blockBuffer.get(i));
                            for (Transaction tx : blockBuffer.get(i).data) {
                                txPool.remove(tx);
                            }

                            if (curr != null) {
                                for (Transaction tx : curr.data) {
                                    if (!blockBuffer.get(i).data.contains(tx)) {
                                        txPool.add(tx);
                                    }
                                }
                            }

                            prevHash = blockBuffer.get(i).hash;
                            ++index;
                            curr = null;
                            addedABlock = true;
                        }
                        cache.put(blockBuffer.get(i).hash, null);
                        sendBlock(blockBuffer.get(i));
                        newBlocks.add(blockBuffer.get(i));
                    }
                }
            }

            for (int i : toBeRemoved) {
                blockBuffer.remove(i);
            }

            prevBlockBufferSize = blockBuffer.size();

            if (addedABlock) {
                updateBlockBuffer();
            } else if (newBlocks.size() > 0) {
                Block max = newBlocks.get(0); // max index could at most be index + 1
                for (Block b : newBlocks) {
                    if (b.index >= max.index) {
                        max = b;
                    }
                }
                if (max.index == index + 1) {
                    Block b1 = blockChain.remove(prevHash);
                    if (b1 != null) { // This happened one time and I think I have fixed the bug.
                        b1.ttl = 0;
                        blockBuffer.add(b1);
                        Block _b1 = null;

                        for (Block b : blockBuffer) {// Really should have made it a hashmap :(
                            if (b.hash.equals(max.prevHash)) {
                                _b1 = b;
                                break;
                            }
                        }

                        if (_b1 != null) { // If _b1 wasn't removed from the cache due to ttl limit exceeding
                            blockChain.put(_b1.hash, _b1);
                            blockChain.put(max.hash, max); // max is _b2 in the email.
                            prevHash = max.hash;
                            ++index;
                            addedABlock = true;
                            curr = null;
                            updateBlockBuffer();
                        }
                    }
                }
            }

        }
    }

    private boolean isChainValid() {
        String[] _blockChain = {};
        // This is sorted according to the order of insertion.
        _blockChain = blockChain.keySet().toArray(_blockChain);

        for (int curr = _blockChain.length - 1, prev = curr - 1; prev >= 0; --prev, --curr) {
            Block _curr = blockChain.get(_blockChain[curr]);

            if (!_blockChain[curr].equals(_curr.calculateHash())) {
                System.out.println("BLOCK HASHES NOT EQUAL!");
                return false;
            }

            if (!_curr.prevHash.equals(_blockChain[prev])) {
                System.out.println("PREVIOUS HASHES NOT EQUAL!");
                return false;
            }
        }

        return true;
    }

    private void mineBlock() {
        if (!curr.hash.substring(0, MINING_DIFFICULTY).equals(target)) {
            ++curr.nonce;
            curr.hash = curr.calculateHash();
            return;
        }

        prevHash = curr.hash;
        curr.index = ++index;
        blockChain.put(prevHash, curr);
        cache.put(curr.hash, curr);
        sendBlock(curr);
        updateBlockBuffer();
        curr = null;
//		System.out.println("Created a block");
    }

    // Called when a new block is added to the block chain
    private void updateBlockBuffer() {
        for (Block b : blockBuffer) {
            if (++b.ttl > BLOCK_CACHE_THRESH) {
                blockBuffer.remove(b);
            }
        }

        prevBlockBufferSize = blockBuffer.size();
    }

    private boolean complementsBlockBuffer(Block b) {
        // Should have used a concurrent hash map for the block buffer
        // but it's too late now ;_;
        for (Block i : blockBuffer) {
            if (b.prevHash.equals(i.hash)) {
                return true;
            }
        }
        return false;
    }

}
