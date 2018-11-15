package com.blocksim;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.blocksim.utils.Constants.GENESIS;
import static com.blocksim.utils.Constants.NUM_NODES;

public class Seed {

    private volatile static ConcurrentHashMap<UUID, Node> allNodes;
    private static Random ran;

    static {
        allNodes = new ConcurrentHashMap<>();
        ran = new Random();
    }

    private Seed() {
    }

    public static PublicKey getRandomAddress() {
        ArrayList<UUID> keysAsArray = new ArrayList<>(allNodes.keySet());
        UUID id = keysAsArray.get(ran.nextInt(allNodes.size()));
        Node newNode = allNodes.get(id);
        return newNode.wallet.pb;
    }

    public static ArrayList<Node> init() {
        ArrayList<Node> nodes = new ArrayList<>();
        for (int i = 0; i < NUM_NODES; i++) {
            UUID x = UUID.randomUUID();
            Node s = new Node(x, false);
            s.blockChain = new LinkedHashMap<>();
            s.blockChain.put(GENESIS, new Block());
            s.neighbors = new CopyOnWriteArrayList<>();
            s.prevHash = GENESIS;
            nodes.add((s));
            allNodes.put(x, s);
        }

        for (Node n : nodes) {
            INode myStuff = new INode(n.txPool, n.blockBuffer);
            for (Node m : nodes) {
                INode hisStuff = new INode(m.txPool, m.blockBuffer);
                if (n != m) {
                    n.neighbors.add(hisStuff);
                    m.neighbors.add(myStuff);
                }
            }
        }
        return nodes;
    }

    public static ArrayList<Object> addNeighbors(Node n) {

        CopyOnWriteArrayList<INode> returnedNeighbors = new CopyOnWriteArrayList<>();
        ArrayList<UUID> keysAsArray = new ArrayList<>(allNodes.keySet());
        INode myStuff = new INode(n.txPool, n.blockBuffer);
        LinkedHashMap<String, Block> _blockChain = null;
        int maxLength = -1;

        int neighbors = ran.nextInt(6) + 5;
        for (int i = 0; i < neighbors; i++) {
            UUID id = keysAsArray.get(ran.nextInt(allNodes.size()));
            Node newNode = allNodes.get(id);
            returnedNeighbors.add(new INode(newNode.txPool, newNode.blockBuffer));
            newNode.neighbors.add(myStuff); //rustom 7asses en fy 7aga ghalat xD
            if (newNode.blockChain.size() > maxLength) {
                _blockChain = newNode.blockChain;
                maxLength = newNode.blockChain.size();
            }
        }

        ArrayList<Object> res = new ArrayList<>();
        res.add(returnedNeighbors);
        res.add(_blockChain);
        return res;
    }

}
