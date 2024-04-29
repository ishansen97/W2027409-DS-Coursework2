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
import com.ds.coursework2.grpc.services.UpdateItemRequest;
import com.ds.coursework2.grpc.services.UpdateItemResponse;
import com.ds.coursework2.grpc.services.ViewItemCatalogueRequest;
import com.ds.coursework2.grpc.services.ViewItemCatalogueResponse;
import com.ds.coursework2.grpc.services.ItemServiceGrpc.ItemServiceImplBase;

import io.grpc.stub.StreamObserver;

public class ItemServiceImpl extends ItemServiceImplBase {
    private Inventory inventory;

    public ItemServiceImpl(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public void addItem(AddItemRequest request, StreamObserver<AddItemResponse> responseObserver) {
        System.out.println("Received items: ");
        String itemName = request.getItemName();
        System.out.println("Name: " + request.getItemName());
        System.out.println("Price: " + request.getPrice());
        System.out.println("Quantity: " + request.getQuantity());

        int newId = inventory.getAddedItemId() + 1;
        Item item = new Item(newId, itemName, request.getPrice(), request.getQuantity());
        try {
            inventory.addItem(itemName, item);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }

        AddItemResponse response = AddItemResponse
                .newBuilder()
                .setItemId(newId)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getItem(GetItemRequest request, StreamObserver<GetItemResponse> responseObserver) {
        int itemId = request.getItemId();
        Item item = inventory.getItem(itemId);
        GetItemResponse response = null;
        if (item != null) {
            response = GetItemResponse.newBuilder()
                    .setItemId(itemId)
                    .setItemName(item.getItemName())
                    .setPrice(item.getPrice())
                    .setQuantity(item.getQuantity())
                    .build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void updateItem(UpdateItemRequest request, StreamObserver<UpdateItemResponse> responseObserver) {
        int itemId = request.getItemId();
        double newPrice = request.getNewPrice();
        inventory.updateItem(itemId, newPrice);

        UpdateItemResponse response = UpdateItemResponse.newBuilder()
                .setItemId(itemId)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deleteItem(DeleteItemRequest request, StreamObserver<DeleteItemResponse> responseObserver) {
        int itemId = request.getItemId();
        inventory.deleteItem(itemId);
        DeleteItemResponse response = DeleteItemResponse.newBuilder()
                .setItemId(itemId)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getItemCatalogue(ViewItemCatalogueRequest request,
            StreamObserver<ViewItemCatalogueResponse> responseObserver) {
        List<Item> items = inventory.getAllItems();
        Iterable<ItemObject> itemResponse = items.stream().map((item) -> populateItemObject(item))
                .collect(Collectors.toList());
        ViewItemCatalogueResponse response = ViewItemCatalogueResponse.newBuilder()
                .addAllItemCatalogue(itemResponse)
                .build();

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
}
