package com.ds.coursework2;

import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import com.ds.coursework2.grpc.services.ItemObject;
import com.ds.coursework2.grpc.services.ItemServiceGrpc;
import com.ds.coursework2.grpc.services.ViewItemCatalogueResponse;
import com.ds.coursework2.grpc.services.customer.CheckItemAvailabilityRequest;
import com.ds.coursework2.grpc.services.customer.CheckItemAvailabilityResponse;
import com.ds.coursework2.grpc.services.customer.CustomerServiceGrpc;
import com.ds.coursework2.grpc.services.customer.ReserveItemsRequest;
import com.ds.coursework2.grpc.services.customer.ReserveItemsResponse;

public class CustomerOptions {
    private static Scanner scanner;
    private static CustomerServiceGrpc.CustomerServiceBlockingStub customerStub;
    private static ItemServiceGrpc.ItemServiceBlockingStub clientStub;

    public static void initializeAttributes(Scanner sc, CustomerServiceGrpc.CustomerServiceBlockingStub stub,
            ItemServiceGrpc.ItemServiceBlockingStub cstub) {
        scanner = sc;
        customerStub = stub;
        clientStub = cstub;
    }

    public static void purchaseItem() {
        System.out.println("Please enter details of the item to be purchased");
        System.out.println("Item Name: ");
        String itemName = scanner.next().trim();
        System.out.println("Quantity: ");
        int quantity = scanner.nextInt();

        CheckItemAvailabilityRequest request = CheckItemAvailabilityRequest.newBuilder()
                .setItemName(itemName)
                .setQuantity(quantity)
                .build();
        System.out.println("The server is checking the availability.");
        CheckItemAvailabilityResponse response = customerStub.checkItemAvailability(request);
        if (response != null && response.getItemExists()) {
            // ask the user for payment details and date.
            System.out.println("You have to pay Rs. " + response.getOrderPrice() + ".......");
            System.out.println("Enter your reservation date (dd-MM-YYYY): ");
            String reserveDate = scanner.next().trim();
            System.out.println("Enter card number: ");
            String cardNumber = scanner.next().trim();

            // make the reservation
            ReserveItemsRequest paymentRequest = ReserveItemsRequest.newBuilder()
                    .setItemName(itemName)
                    .setQuantity(quantity)
                    // .setOrderPrice(response.getOrderPrice())
                    // .setReserveDate(reserveDate)
                    // .setCardNumber(cardNumber)
                    .build();

            System.out.println("The server is processing your request....");
            ReserveItemsResponse paymentResponse = customerStub.reserveItem(paymentRequest);
            System.out.println(paymentResponse.getSuccessMessage());
        } else {
            System.out.println("This item does not exist.");
        }
    }

    public static void viewItemCatalogue() {
        System.out.println("Requesting Item catalogue from the server.");
        ViewItemCatalogueResponse response = clientStub.getItemCatalogue(null);
        List<ItemObject> items = response.getItemCatalogueList();
        String itemNameHeader = "Item Name";
        String itemPriceHeader = "Unit Price (LKR)";
        System.out.println(itemNameHeader + String.join(" ", Collections.nCopies((30 - itemPriceHeader.length()), " "))
                + itemPriceHeader);
        // display items
        for (ItemObject item : items) {
            List<String> namePriceSpacing = Collections.nCopies((30 - itemPriceHeader.length()), " ");
            System.out.println(item.getItemName() + String.join(" ", namePriceSpacing) + item.getPrice());
        }
    }
}
