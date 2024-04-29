package com.ds.coursework2;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import com.ds.coursework2.grpc.services.AddItemRequest;
import com.ds.coursework2.grpc.services.AddItemResponse;
import com.ds.coursework2.grpc.services.customer.CheckItemAvailabilityRequest;
import com.ds.coursework2.grpc.services.customer.CheckItemAvailabilityResponse;
import com.ds.coursework2.grpc.services.DeleteItemRequest;
import com.ds.coursework2.grpc.services.DeleteItemResponse;
import com.ds.coursework2.grpc.services.GetItemRequest;
import com.ds.coursework2.grpc.services.GetItemResponse;
import com.ds.coursework2.grpc.services.ItemObject;
import com.ds.coursework2.grpc.services.ItemServiceGrpc;
import com.ds.coursework2.grpc.services.UpdateItemRequest;
import com.ds.coursework2.grpc.services.UpdateItemResponse;
import com.ds.coursework2.grpc.services.ViewItemCatalogueResponse;
import com.ds.coursework2.grpc.services.customer.CustomerServiceGrpc;
import com.ds.coursework2.grpc.services.customer.ReserveItemsRequest;
import com.ds.coursework2.grpc.services.customer.ReserveItemsResponse;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ClientMain {
    private static final String CUSTOMER = "customer";
    private static final String SELLER = "seller";
    private ManagedChannel channel = null;
    private ItemServiceGrpc.ItemServiceBlockingStub clientStub;
    private CustomerServiceGrpc.CustomerServiceBlockingStub customerStub;
    private String host = null;
    private Scanner scanner = new Scanner(System.in);
    int port = -1;
    String clientType;

    public static void main(String[] args) {
        String host = null;
        int port = -1;
        String clientType;
        if (args.length != 3) {
            System.out.println("Usage Client Main <host> <port>");
            System.exit(1);
        }
        host = args[0];
        port = Integer.parseInt(args[1].trim());
        clientType = args[2].trim();

        ClientMain client = new ClientMain(host, port, clientType);
        client.initializeConnection();
        client.displayMenu(clientType);
        client.closeConnection();
    }

    public ClientMain(String host, int port, String mode) {
        this.host = host;
        this.port = port;
        this.clientType = mode;
    }

    private void initializeConnection() {
        System.out.println("Initializing Connecting to server at " + host + ":" + port);
        channel = ManagedChannelBuilder.forAddress("localhost", port)
                .usePlaintext()
                .build();
        clientStub = ItemServiceGrpc.newBlockingStub(channel);
        customerStub = CustomerServiceGrpc.newBlockingStub(channel);
    }

    private void closeConnection() {
        channel.shutdown();
    }

    private void displayMenu(String clientType) {
        switch (clientType) {
            case SELLER:
                displaySellerMenu();
                break;
            case CUSTOMER:
                displayCustomerMenu();
                break;
            default:
                System.out.println("Invalid client type.");
                break;
        }
    }

    private void displaySellerMenu() {
        int option = 0;
        do {
            System.out.println("Please select one of the following");
            System.out.println("1 - Add Item");
            System.out.println("2 - Get Item details");
            System.out.println("3 - Update an existing Item");
            System.out.println("4 - Delete Item");
            System.out.println("-1 - Exit");

            System.out.println("Enter your option: ");
            option = scanner.nextInt();

            if (option > 0) {
                handleSellerOptions(option);
            }
        } while (option != -1);
    }

    private void displayCustomerMenu() {
        int option = 0;
        do {
            System.out.println("Please select one of the following");
            System.out.println("1 - View Item Catalogue");
            System.out.println("2 - Purchase Item");
            System.out.println("-1 - Exit");

            System.out.println("Enter your option: ");
            option = scanner.nextInt();

            if (option > 0) {
                handleCustomerOption(option);
            }
        } while (option != -1);
    }

    private void handleSellerOptions(int sellerOption) {
        switch (sellerOption) {
            case 1:
                addItem();
                break;
            case 2:
                getItem();
                break;
            case 3:
                updateItem();
                break;
            case 4:
                deleteItem();
                break;
            default:
                break;
        }
    }

    // #region Seller operations
    private void addItem() {
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

        System.out.printf("The Item with ID {0} has been added to the inventory.\n", itemId);
    }

    private void getItem() {
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

    private void updateItem() {
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

    private void deleteItem() {
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

    // #endregion

    // customer region
    private void handleCustomerOption(int option) {
        switch (option) {
            case 1:
                viewItemCatalogue();
                break;
            case 2:
                purchaseItem();
            default:
                break;
        }
    }
    // end of customer region

    private void purchaseItem() {
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
            ;
        }
    }

    private void viewItemCatalogue() {
        System.out.println("Requesting Item cataologue from the server.");
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