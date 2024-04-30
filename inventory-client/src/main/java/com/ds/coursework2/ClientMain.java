package com.ds.coursework2;

import java.util.ArrayList;
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
import com.ds.coursework2.grpc.services.order.CheckOrderAvailabilityRequest;
import com.ds.coursework2.grpc.services.order.CheckOrderAvailabilityResponse;
import com.ds.coursework2.grpc.services.order.ItemOrderRequest;
import com.ds.coursework2.grpc.services.order.OrderItemsRequest;
import com.ds.coursework2.grpc.services.order.OrderItemsResponse;
import com.ds.coursework2.grpc.services.order.OrderServiceGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ClientMain {
    private static final String CUSTOMER = "customer";
    private static final String SELLER = "seller";
    private static final String FACTORY = "factory";
    private ManagedChannel channel = null;
    private ItemServiceGrpc.ItemServiceBlockingStub clientStub;
    private CustomerServiceGrpc.CustomerServiceBlockingStub customerStub;
    private OrderServiceGrpc.OrderServiceBlockingStub orderStub;
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
        orderStub = OrderServiceGrpc.newBlockingStub(channel);
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
            case FACTORY:
                displayFactoryMenu();
                break;
            default:
                System.out.println("Invalid client type.");
                break;
        }
    }

    private void displaySellerMenu() {
        int option = 0;
        SellerOptions.initializeAttributes(scanner, clientStub);
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
        CustomerOptions.initializeAttributes(scanner, customerStub, clientStub);
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

    private void displayFactoryMenu() {
        int option = 0;
        CustomerOptions.initializeAttributes(scanner, customerStub, clientStub);
        FactoryOptions.initializeAttributes(scanner, orderStub);
        do {
            System.out.println("Please select one of the following");
            System.out.println("1 - View Item Catalogue");
            System.out.println("2 - Perform bulk Item order");
            System.out.println("-1 - Exit");

            System.out.println("Enter your option: ");
            option = scanner.nextInt();

            if (option > 0) {
                handleFactoryOption(option);
            }
        } while (option != -1);
    }

    private void handleSellerOptions(int sellerOption) {
        switch (sellerOption) {
            case 1:
                SellerOptions.addItem();
                break;
            case 2:
                SellerOptions.getItem();
                break;
            case 3:
                SellerOptions.updateItem();
                break;
            case 4:
                SellerOptions.deleteItem();
                break;
            default:
                System.out.println("Invalid customer option.");
                break;
        }
    }

    // customer region
    private void handleCustomerOption(int option) {
        switch (option) {
            case 1:
                CustomerOptions.viewItemCatalogue();
                break;
            case 2:
                CustomerOptions.purchaseItem();
            default:
                break;
        }
    }
    // end of customer region

    private void handleFactoryOption(int option) {
        switch (option) {
            case 1:
                CustomerOptions.viewItemCatalogue();
                break;
            case 2:
                FactoryOptions.orderItems();
            default:
                break;
        }
    }
}