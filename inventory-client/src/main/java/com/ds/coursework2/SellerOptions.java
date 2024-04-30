package com.ds.coursework2;

import java.util.Scanner;

import com.ds.coursework2.grpc.services.AddItemRequest;
import com.ds.coursework2.grpc.services.AddItemResponse;
import com.ds.coursework2.grpc.services.DeleteItemRequest;
import com.ds.coursework2.grpc.services.DeleteItemResponse;
import com.ds.coursework2.grpc.services.GetItemRequest;
import com.ds.coursework2.grpc.services.GetItemResponse;
import com.ds.coursework2.grpc.services.ItemServiceGrpc;
import com.ds.coursework2.grpc.services.UpdateItemRequest;
import com.ds.coursework2.grpc.services.UpdateItemResponse;

public class SellerOptions {
    private static Scanner scanner;
    private static ItemServiceGrpc.ItemServiceBlockingStub clientStub;

    public static void initializeAttributes(Scanner sc, ItemServiceGrpc.ItemServiceBlockingStub stub) {
        scanner = sc;
        clientStub = stub;
    }

    public static void addItem() {
        System.out.println("==== Please enter Item details. ====");
        System.out.println("Item Name: ");
        String itemName = scanner.next();
        System.out.println("price (in LKR): ");
        double price = scanner.nextDouble();
        System.out.println("quantity: ");
        int quantity = scanner.nextInt();

        // create the AddItemRequest
        AddItemRequest addItemRequest = AddItemRequest
                .newBuilder()
                .setItemName(itemName)
                .setPrice(price)
                .setQuantity(quantity)
                .build();

        System.out.println("Item details sending to server....");
        AddItemResponse itemResponse = clientStub.addItem(addItemRequest);
        int itemId = itemResponse.getItemId();

        System.out.printf("The Item with ID {} has been added to the inventory.\n", itemId);
    }

    public static void getItem() {
        System.out.println("==== Get Item  details. ====");
        System.out.println("Item ID: ");
        int itemId = scanner.nextInt();

        // create the GetItemRequest
        GetItemRequest request = GetItemRequest.newBuilder()
                .setItemId(itemId)
                .build();

        GetItemResponse response = clientStub.getItem(request);
        System.out.println("==== Item details ====");
        System.out.println("Item ID: " + response.getItemId());
        System.out.println("Item name: " + response.getItemName());
        System.out.println("Price: " + response.getPrice());
        System.out.println("Remaining Quantity: " + response.getQuantity());
    }

    public static void updateItem() {
        System.out.println("==== Update Item  details. ====");
        System.out.println("Item ID: ");
        int itemId = scanner.nextInt();
        System.out.println("New price (in LKR): ");
        double newPrice = scanner.nextDouble();

        // create the UpdateItemRequest
        UpdateItemRequest request = UpdateItemRequest.newBuilder()
                .setItemId(itemId)
                .setNewPrice(newPrice)
                .build();

        System.out.println("Update item request sending to the server.");
        UpdateItemResponse response = clientStub.updateItem(request);
        System.out.println("Item ID " + itemId + " successfully updated in the Inventory.");
    }

    public static void deleteItem() {
        System.out.println("==== Delete Item  details. ====");
        System.out.println("Item ID: ");
        int itemId = scanner.nextInt();

        // create the UpdateItemRequest
        DeleteItemRequest request = DeleteItemRequest.newBuilder()
                .setItemId(itemId)
                .build();

        System.out.println("Delete item request sending to the server.");
        DeleteItemResponse response = clientStub.deleteItem(request);
        System.out.println("Item ID " + itemId + " successfully deleted from the Inventory.");
    }

}
