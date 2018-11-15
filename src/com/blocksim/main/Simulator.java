package com.blocksim.main;

import com.blocksim.Node;
import com.blocksim.Seed;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Simulator {
    public static final int nodeNum = 10;

    public static void main(String[] args) throws Exception {
        ExecutorService es = Executors.newFixedThreadPool(nodeNum);

        ArrayList<Node> initNodes = Seed.init(); // initially has 10 nodes
        initNodes.stream().parallel().forEach(
            n -> es.execute(n)
        );

        for (int i = 0; i < nodeNum - 10; ++i) {
            es.execute(new Node(UUID.randomUUID()));
        }
    }
}
