package com.ds.coursework2;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.zookeeper.KeeperException;

import com.ds.coursework2.grpc.services.customer.CheckItemAvailabilityRequest;
import com.ds.coursework2.grpc.services.customer.CheckItemAvailabilityResponse;
import com.ds.coursework2.grpc.services.customer.CustomerServiceGrpc;
import com.ds.coursework2.grpc.services.customer.CustomerServiceGrpc.CustomerServiceBlockingStub;
import com.ds.coursework2.grpc.services.customer.CustomerServiceGrpc.CustomerServiceImplBase;
import com.ds.coursework2.grpc.services.customer.ReserveItemsRequest;
import com.ds.coursework2.grpc.services.customer.ReserveItemsResponse;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class CustomerServiceImpl extends CustomerServiceImplBase implements DistributedTxListener {
    private Inventory inventory;
    private ManagedChannel channel;
    private CustomerServiceBlockingStub clientStub;
    private String transactionItemName;
    private int transactionQuantity;
    private DistributedTx transaction;

    public CustomerServiceImpl(Inventory inventory) {
        this.inventory = inventory;
        this.transaction = inventory.getTransaction();
    }

    @Override
    public void checkItemAvailability(CheckItemAvailabilityRequest request,
            StreamObserver<CheckItemAvailabilityResponse> responseObserver) {
        String itemName = request.getItemName();
        int quantity = request.getQuantity();
        Item item = inventory.getItem(itemName);
        CheckItemAvailabilityResponse response = null;
        if (item != null) {
            double finalPrice = item.getPrice() * quantity;
            response = CheckItemAvailabilityResponse.newBuilder()
                    .setItemName(itemName)
                    .setQuantity(quantity)
                    .setItemExists(true)
                    .setOrderPrice(finalPrice)
                    .build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void reserveItem(ReserveItemsRequest request, StreamObserver<ReserveItemsResponse> responseObserver) {
        String itemName = request.getItemName();
        int quantity = request.getQuantity();
        ReserveItemsResponse response = null;
        String message = "Your transaction is successful.";

        try {
            if (inventory.isLeader()) {
                // Thread.sleep(2000);
                System.out.println("Updating item catalogue as primary");
                beginTransaction(itemName, quantity);
                updateSecondaryServers(itemName, quantity);
                ((DistributedTxCoordinator) inventory.getTransaction()).perform();
            } else {
                // Act As Secondary
                if (request.getSentByPrimary()) {
                    System.out.println("Updating item catalogue on secondary, on Primary's command");
                    beginTransaction(itemName, quantity);
                    if (quantity > 0) {
                        ((DistributedTxParticipant) inventory.getTransaction()).voteCommit();
                    } else {
                        ((DistributedTxParticipant) inventory.getTransaction()).voteAbort();
                    }
                } else {
                    response = callPrimary(itemName, quantity);
                    if (response.getSuccessMessage() != null) {
                        System.out.println("all good");
                    }
                }
            }
        } catch (KeeperException kx) {
            message = "This transaction cannot be processed at the moment.";
        } catch (IOException ex1) {
            message = "This transaction cannot be processed at the moment.";
        } catch (Exception ex) {
            message = "This transaction cannot be processed at the moment since one is already being processed.";
        }

        response = ReserveItemsResponse.newBuilder()
                .setSuccessMessage(message).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private synchronized void modifyItemData() {
        inventory.decrementItemCount(transactionItemName, transactionQuantity);
        transactionItemName = null;
        transactionQuantity = -1;
    }

    private void beginTransaction(String itemName, int quantity) throws Exception {
        String uuId = String.valueOf(UUID.randomUUID());
        System.out.println("UUID for the transaction: " + uuId);
        transactionItemName = itemName;
        transactionQuantity = quantity;
        System.out.println(
                "transaction name: " + transactionItemName + ", transaction quantity: " + transactionQuantity);
        inventory.getTransaction().start(itemName, uuId);
    }

    @Override
    public void onGlobalCommit() {
        DistributedLock itemLock;
        try {
            itemLock = new DistributedLock(transactionItemName);
            itemLock.acquireLock();
            Thread.sleep(5000);
            modifyItemData();
            itemLock.releaseLock();
        } catch (IOException | KeeperException | InterruptedException e) {
            System.out.println("something bad happened on global commit.");
        }
    }

    @Override
    public void onGlobalAbort() {
        System.out.println("global abort to be implemented.");
    }

    private void updateSecondaryServers(String accountId, int quantity) throws KeeperException, InterruptedException {
        System.out.println("Updating secondary servers");
        List<String[]> othersData = inventory.getOthersData();
        for (String[] data : othersData) {
            String IPAddress = data[0];
            int port = Integer.parseInt(data[1]);
            callServer(accountId, quantity, true, IPAddress, port);
        }
    }

    private ReserveItemsResponse callServer(String itemName, int quantity, boolean isSentByPrimary, String IPAddress,
            int port) {
        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();
        clientStub = CustomerServiceGrpc.newBlockingStub(channel);

        ReserveItemsRequest request = ReserveItemsRequest
                .newBuilder()
                .setItemName(itemName)
                .setQuantity(quantity)
                // .setOrderPrice(response.getOrderPrice())
                // .setReserveDate(reserveDate)
                // .setCardNumber(cardNumber)
                .build();
        ReserveItemsResponse response = clientStub.reserveItem(request);
        return response;
    }

    private ReserveItemsResponse callPrimary(String itemName, int quantity) {
        System.out.println("Calling Primary server");
        String[] currentLeaderData = inventory.getCurrentLeaderData();
        String IPAddress = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);
        return callServer(itemName, quantity, false, IPAddress, port);
    }

}
