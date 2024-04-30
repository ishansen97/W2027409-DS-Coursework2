package com.ds.coursework2;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.ds.coursework2.grpc.services.order.CheckOrderAvailabilityRequest;
import com.ds.coursework2.grpc.services.order.CheckOrderAvailabilityResponse;
import com.ds.coursework2.grpc.services.order.ItemOrderRequest;
import com.ds.coursework2.grpc.services.order.OrderItemsRequest;
import com.ds.coursework2.grpc.services.order.OrderItemsResponse;
import com.ds.coursework2.grpc.services.order.OrderServiceGrpc;

public class FactoryOptions {
    private static Scanner scanner;
    private static OrderServiceGrpc.OrderServiceBlockingStub orderStub;

    public static void initializeAttributes(Scanner sc, OrderServiceGrpc.OrderServiceBlockingStub stub) {
        scanner = sc;
        orderStub = stub;
    }

    public static void orderItems() {
        System.out.println("Please enter the company name.");
        String companyName = scanner.next();
        String askForItems = "y";
        List<ItemOrderRequest> itemRequests = new ArrayList<ItemOrderRequest>();

        do {
            System.out.println("===== Please enter Item details to be purchased. =====");
            System.out.println("Item Name: ");
            String itemName = scanner.next();
            System.out.println("Quantity: ");
            int quantity = scanner.nextInt();

            ItemOrderRequest itemRequest = ItemOrderRequest.newBuilder()
                    .setItemName(itemName)
                    .setQuantity(quantity)
                    .build();
            itemRequests.add(itemRequest);
            System.out.println("Do you wish to continue (Y/N)?");
            askForItems = scanner.next();
        } while (!askForItems.equalsIgnoreCase("n"));

        CheckOrderAvailabilityRequest request = CheckOrderAvailabilityRequest.newBuilder()
                .addAllAvailabilityRequest(itemRequests)
                .build();

        CheckOrderAvailabilityResponse response = orderStub.checkOrderAvailability(request);
        if (!response.getItemsExist()) {
            System.out.println("You cannot purchase these items as they are currently unavailable.");
        } else {
            // create the order
            OrderItemsRequest orderRequest = OrderItemsRequest.newBuilder()
                    .setCompanyName(companyName)
                    .build();

            OrderItemsResponse orderResponse = orderStub.orderItems(orderRequest);
            if (orderResponse.getOrderSuccess()) {
                System.out.println("The order requsted by " + companyName + " is successfully completed.");
            }
        }
    }
}
