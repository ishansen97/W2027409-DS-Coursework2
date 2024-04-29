package com.ds.coursework2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.zookeeper.KeeperException;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class Inventory {
    private DistributedLock leaderLock;
    private String host;
    private int serverPort;
    private AtomicBoolean isLeader = new AtomicBoolean(false);
    private byte[] leaderData;
    private ItemServiceImpl itemService;
    private CustomerServiceImpl customerService;
    private DistributedTx transaction;

    private Map<String, Item> itemStore;

    public static void main(String[] args) throws Exception {
        DistributedLock.setZooKeeperURL("localhost:2181");
        DistributedTx.setZooKeeperURL("localhost:2181");
        if (args.length != 1) {
            System.out.println("Usage InventoryServer <port>");
            System.exit(1);
        }
        int serverPort = Integer.parseInt(args[0].trim());
        Inventory inventory = new Inventory("localhost", serverPort);
        inventory.startServer();

    }

    public Inventory(String host, int port) throws IOException, InterruptedException, KeeperException {
        this.host = host;
        this.serverPort = port;
        this.itemService = new ItemServiceImpl(this);
        this.customerService = new CustomerServiceImpl(this);
        this.itemStore = new HashMap<String, Item>();
        leaderLock = new DistributedLock("InventoryServerCluster", buildServerData(host, port));
        transaction = new DistributedTxParticipant(customerService);
    }

    public void startServer() throws IOException, InterruptedException, KeeperException {
        Server server = ServerBuilder
                .forPort(serverPort)
                .addService(itemService)
                .addService(customerService)
                .build();
        server.start();
        System.out.println("InventoryServer Started and ready to accept requests on port " + serverPort);

        tryToBeLeader();
        server.awaitTermination();
    }

    public static String buildServerData(String IP, int port) {
        StringBuilder builder = new StringBuilder();
        builder.append(IP).append(":").append(port);
        return builder.toString();
    }

    private void tryToBeLeader() throws KeeperException, InterruptedException {
        Thread leaderCampaignThread = new Thread(new LeaderCampaignThread());
        leaderCampaignThread.start();
    }

    public synchronized void addItem(String itemName, Item item) throws KeeperException, InterruptedException {
        itemStore.put(itemName, item);
    }

    public int getAddedItemId() {
        return itemStore.size();
    }

    public Item getItem(String itemName) {
        return itemStore.get(itemName);
    }

    public Item getItem(int itemId) {
        for (Entry<String, Item> elem : itemStore.entrySet()) {
            Item elemItem = elem.getValue();
            if (elemItem.getItemId() == itemId) {
                return elemItem;
            }
        }
        return null;
    }

    public List<Item> getAllItems() {
        return itemStore.values().stream().collect(Collectors.toList());
    }

    public Iterable<Item> getItems() {
        return itemStore.values();
    }

    public synchronized void decrementItemCount(String itemName, int quantity) {
        Item item = itemStore.get(itemName);
        item.setQuantity(item.getQuantity() - quantity);
        itemStore.put(itemName, item);
        System.out.println("Current item count: " + item.getQuantity());
    }

    public synchronized void updateItem(int itemId, double newPrice) {
        for (Entry<String, Item> elem : itemStore.entrySet()) {
            Item elemItem = elem.getValue();
            if (elemItem.getItemId() == itemId) {
                elemItem.setPrice(newPrice);
                break;
            }
        }

        System.out.println("Price for Item ID " + itemId + " is updated.");
    }

    public synchronized void deleteItem(int itemId) {
        String itemName = "";
        for (Entry<String, Item> elem : itemStore.entrySet()) {
            Item elemItem = elem.getValue();
            if (elemItem.getItemId() == itemId) {
                itemName = elem.getKey();
                break;
            }
        }

        // remove the item
        if (itemStore.remove(itemName) != null) {
            System.out.println("Item ID " + itemId + " is removed.");
        }
    }

    public synchronized String[] getCurrentLeaderData() {
        return new String(leaderData).split(":");
    }

    // leader campaign class
    class LeaderCampaignThread implements Runnable {
        private byte[] currentLeaderData = null;

        @Override
        public void run() {
            System.out.println("Starting the leader Campaign");

            try {
                boolean leader = leaderLock.tryAcquireLock();

                while (!leader) {
                    byte[] leaderData = leaderLock.getLockHolderData();
                    if (currentLeaderData != leaderData) {
                        currentLeaderData = leaderData;
                        setCurrentLeaderData(currentLeaderData);
                    }
                    Thread.sleep(5000);
                    leader = leaderLock.tryAcquireLock();
                }
                // System.out.println("I got the leader lock. Now acting as primary");
                // isLeader.set(true);
                currentLeaderData = null;
                beTheLeader();
            } catch (Exception e) {
            }
        }
    }

    private void beTheLeader() {
        System.out.println("I got the leader lock. Now acting as primary");
        isLeader.set(true);
        transaction = new DistributedTxCoordinator(customerService);
    }

    private synchronized void setCurrentLeaderData(byte[] leaderData) {
        this.leaderData = leaderData;
    }

    public DistributedTx getTransaction() {
        return transaction;
    }

    public List<String[]> getOthersData() throws KeeperException, InterruptedException {
        List<String[]> result = new ArrayList<>();
        List<byte[]> othersData = leaderLock.getOthersData();

        for (byte[] data : othersData) {
            String[] dataStrings = new String(data).split(":");
            result.add(dataStrings);
        }
        return result;
    }

    public boolean isLeader() {
        return isLeader.get();
    }
}