package com.ds.coursework2;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.zookeeper.KeeperException;

import com.ds.coursework2.grpc.services.AddItemRequest;
import com.ds.coursework2.grpc.services.AddItemResponse;
import com.ds.coursework2.grpc.services.DeleteItemRequest;
import com.ds.coursework2.grpc.services.DeleteItemResponse;
import com.ds.coursework2.grpc.services.GetItemRequest;
import com.ds.coursework2.grpc.services.GetItemResponse;
import com.ds.coursework2.grpc.services.ItemObject;
import com.ds.coursework2.grpc.services.ItemServiceGrpc;
import com.ds.coursework2.grpc.services.UpdateItemRequest;
import com.ds.coursework2.grpc.services.UpdateItemResponse;
import com.ds.coursework2.grpc.services.ViewItemCatalogueRequest;
import com.ds.coursework2.grpc.services.ViewItemCatalogueResponse;
import com.ds.coursework2.grpc.services.ItemServiceGrpc.ItemServiceImplBase;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class ItemServiceImpl extends ItemServiceImplBase {
    private Inventory inventory;
    private ManagedChannel channel;
    private ItemServiceGrpc.ItemServiceBlockingStub clientStub;

    public ItemServiceImpl(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public void addItem(AddItemRequest request, StreamObserver<AddItemResponse> responseObserver) {
        System.out.println("Received items: ");
        String itemName = request.getItemName();
        double price = request.getPrice();
        int quantity = request.getQuantity();

        AddItemResponse response = null;
        try {
            if (inventory.isLeader()) {
                System.out.println("Name: " + itemName);
                System.out.println("Price: " + price);
                System.out.println("Quantity: " + quantity);
                int newId = inventory.getAddedItemId() + 1;
                Item item = new Item(newId, itemName, request.getPrice(), request.getQuantity());
                try {
                    inventory.addItem(itemName, item);
                } catch (KeeperException | InterruptedException e) {
                    e.printStackTrace();
                }

                response = AddItemResponse
                        .newBuilder()
                        .setItemId(newId)
                        .build();

                onItemAddition(itemName, price, quantity);
            } else {
                // Act as secondary
                if (request.getSentByPrimary()) {
                    System.out.println("Printing the message sent by the primary server.");
                    System.out.println("The new item \'" + request.getItemName()
                            + "\' has been added to the system by the primary server.");
                } else {
                    response = callPrimaryOnAddition(itemName, price, quantity);
                }
            }
        } catch (Exception ex) {
            System.out.println("Something went wrong when adding new Item.");
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getItem(GetItemRequest request, StreamObserver<GetItemResponse> responseObserver) {
        int itemId = request.getItemId();
        GetItemResponse response = null;

        try {
            if (inventory.isLeader()) {
                Item item = inventory.getItem(itemId);
                if (item != null) {
                    response = GetItemResponse.newBuilder()
                            .setItemId(itemId)
                            .setItemName(item.getItemName())
                            .setPrice(item.getPrice())
                            .setQuantity(item.getQuantity())
                            .build();
                }
            } else {
                // Act as secondary
                System.out.println("Redirecting the item retrieval request to primary.");
                response = callPrimaryForRetrieval(itemId);
            }
        } catch (Exception ex) {
            System.out.println("something went wrong when retrieving the Item.");
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void updateItem(UpdateItemRequest request, StreamObserver<UpdateItemResponse> responseObserver) {
        int itemId = request.getItemId();
        double newPrice = request.getNewPrice();
        UpdateItemResponse response = null;

        try {
            if (inventory.isLeader()) {
                inventory.updateItem(itemId, newPrice);
                response = UpdateItemResponse.newBuilder()
                        .setItemId(itemId)
                        .build();
                OnItemUpdation(itemId, newPrice);
            } else {
                // Act as secondary
                if (request.getSentByPrimary()) {
                    System.out.println("Printing message sent by primary server.");
                    System.out.println("The item ID " + itemId + " has been updated.");
                } else {
                    response = callPrimaryOnUpdation(itemId, newPrice);
                }
            }
        } catch (Exception ex) {
            System.err.println("Something went wrong in updating the item.");
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deleteItem(DeleteItemRequest request, StreamObserver<DeleteItemResponse> responseObserver) {
        int itemId = request.getItemId();
        DeleteItemResponse response = null;

        try {
            if (inventory.isLeader()) {
                inventory.deleteItem(itemId);
                response = DeleteItemResponse.newBuilder()
                        .setItemId(itemId)
                        .build();

                onItemDeletion(itemId);
            } else {
                // Act as secondary.
                if (request.getSentByPrimary()) {
                    System.out.println("Printing this message on primary server command.");
                    System.out.println("The Item ID " + itemId + " has been deleted from the system.");
                } else {
                    response = callPrimaryOnDeletion(itemId);
                }
            }
        } catch (Exception ex) {
            System.err.println("Something went wrong in deleting the item.");
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getItemCatalogue(ViewItemCatalogueRequest request,
            StreamObserver<ViewItemCatalogueResponse> responseObserver) {

        ViewItemCatalogueResponse response = null;
        try {
            if (inventory.isLeader()) {
                List<Item> items = inventory.getAllItems();
                Iterable<ItemObject> itemResponse = items.stream().map((item) -> populateItemObject(item))
                        .collect(Collectors.toList());
                response = ViewItemCatalogueResponse.newBuilder()
                        .addAllItemCatalogue(itemResponse)
                        .build();
            } else {
                // Act as secondary
                response = callPrimaryForItemCatalogue();
            }
        } catch (Exception ex) {
            System.err.println("Something went wrong in displaying item catalogue.");
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // util methods
    private ItemObject populateItemObject(Item item) {
        ItemObject obj = ItemObject.newBuilder()
                .setItemId(item.getItemId())
                .setItemName(item.getItemName())
                .setPrice(item.getPrice())
                .setQuantity(item.getQuantity())
                .build();

        return obj;
    }

    // addition
    private void onItemAddition(String itemName, double price, int quantity)
            throws KeeperException, InterruptedException {
        System.out.println("Informing secondary servers on Adding new Item");
        List<String[]> othersData = inventory.getOthersData();
        for (String[] data : othersData) {
            String IPAddress = data[0];
            int port = Integer.parseInt(data[1]);
            callServerOnAddition(itemName, price, quantity, true, IPAddress, port);
        }
    }

    private AddItemResponse callPrimaryOnAddition(String itemName, double price, int quantity) {
        String[] currentLeaderData = inventory.getCurrentLeaderData();
        String IPAddress = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);
        return callServerOnAddition(itemName, price, quantity, false, IPAddress, port);
    }

    private AddItemResponse callServerOnAddition(String itemName, double price, int quantity, boolean isSentByPrimary,
            String IPAddress,
            int port) {
        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();
        clientStub = ItemServiceGrpc.newBlockingStub(channel);

        AddItemRequest request = AddItemRequest
                .newBuilder()
                .setItemName(itemName)
                .setPrice(price)
                .setQuantity(quantity)
                .setSentByPrimary(isSentByPrimary)
                .build();
        AddItemResponse response = clientStub.addItem(request);
        return response;
    }

    private GetItemResponse callPrimaryForRetrieval(int itemId) {
        String[] currentLeaderData = inventory.getCurrentLeaderData();
        String IPAddress = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);

        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();
        clientStub = ItemServiceGrpc.newBlockingStub(channel);

        GetItemRequest request = GetItemRequest
                .newBuilder()
                .setItemId(itemId)
                .setSentByPrimary(false)
                .build();
        GetItemResponse response = clientStub.getItem(request);
        return response;
    }

    // updation
    private void OnItemUpdation(int itemId, double newPrice) throws KeeperException, InterruptedException {
        System.out.println("Informing secondary servers on Updating an Item");
        List<String[]> othersData = inventory.getOthersData();
        for (String[] data : othersData) {
            String IPAddress = data[0];
            int port = Integer.parseInt(data[1]);
            callServerOnUpdation(itemId, newPrice, true, IPAddress, port);
        }
    }

    private UpdateItemResponse callPrimaryOnUpdation(int itemId, double newPrice) {
        String[] currentLeaderData = inventory.getCurrentLeaderData();
        String IPAddress = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);
        return callServerOnUpdation(itemId, newPrice, false, IPAddress, port);
    }

    private UpdateItemResponse callServerOnUpdation(int itemId, double newPrice, boolean isSentByPrimary,
            String IPAddress, int port) {
        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();
        clientStub = ItemServiceGrpc.newBlockingStub(channel);

        UpdateItemRequest request = UpdateItemRequest
                .newBuilder()
                .setItemId(itemId)
                .setNewPrice(newPrice)
                .setSentByPrimary(isSentByPrimary)
                .build();
        UpdateItemResponse response = clientStub.updateItem(request);
        return response;
    }

    // deletion
    private void onItemDeletion(int itemId) throws KeeperException, InterruptedException {
        System.out.println("Informing secondary servers on deleting an Item");
        List<String[]> othersData = inventory.getOthersData();
        for (String[] data : othersData) {
            String IPAddress = data[0];
            int port = Integer.parseInt(data[1]);
            callServerOnDeletion(itemId, true, IPAddress, port);
        }
    }

    private DeleteItemResponse callPrimaryOnDeletion(int itemId) {
        String[] currentLeaderData = inventory.getCurrentLeaderData();
        String IPAddress = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);
        return callServerOnDeletion(itemId, false, IPAddress, port);
    }

    private DeleteItemResponse callServerOnDeletion(int itemId, boolean isSentByPrimary, String IPAddress, int port) {
        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();
        clientStub = ItemServiceGrpc.newBlockingStub(channel);

        DeleteItemRequest request = DeleteItemRequest
                .newBuilder()
                .setItemId(itemId)
                .setSentByPrimary(isSentByPrimary)
                .build();
        DeleteItemResponse response = clientStub.deleteItem(request);
        return response;
    }

    // view catalogue
    private ViewItemCatalogueResponse callPrimaryForItemCatalogue() {
        String[] currentLeaderData = inventory.getCurrentLeaderData();
        String IPAddress = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);

        System.out.println("Call Server " + IPAddress + ":" + port);
        channel = ManagedChannelBuilder.forAddress(IPAddress, port)
                .usePlaintext()
                .build();
        clientStub = ItemServiceGrpc.newBlockingStub(channel);

        ViewItemCatalogueRequest request = ViewItemCatalogueRequest
                .newBuilder()
                .setSentByPrimary(false)
                .build();
        ViewItemCatalogueResponse response = clientStub.getItemCatalogue(request);
        return response;
    }

}
