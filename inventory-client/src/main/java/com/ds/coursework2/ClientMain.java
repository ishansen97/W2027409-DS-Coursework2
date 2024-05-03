package com.ds.coursework2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import com.ds.coursework2.grpc.services.ItemServiceGrpc;
import com.ds.coursework2.grpc.services.customer.CustomerServiceGrpc;
import com.ds.coursework2.grpc.services.order.OrderServiceGrpc;
import com.ds.coursework2.naming.NameServerClient;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ClientMain {
    public static final String NAME_SERVICE_ADDRESS = "http://localhost:2379";
    public static final String ITEM_SERVICE = "ItemService";
    public static final String CUSTOMER_SERVICE = "CustomerService";
    public static final String ORDER_SERVICE = "OrderService";
    private static final String CUSTOMER = "customer";
    private static final String SELLER = "seller";
    private static final String FACTORY = "factory";

    private static final String PRIMARY = "primary";
    private static final String SECONDARY = "secondary";
    private static final String TERTIARY = "tertiary";

    private ManagedChannel channel = null;
    private ItemServiceGrpc.ItemServiceBlockingStub clientStub;
    private CustomerServiceGrpc.CustomerServiceBlockingStub customerStub;
    private OrderServiceGrpc.OrderServiceBlockingStub orderStub;
    private String host = null;
    private Scanner scanner = new Scanner(System.in);
    int port = -1;
    String clientType;
    String serverType;

    public static void main(String[] args) throws InterruptedException, IOException {
        String host = null;
        int port = -1;
        String clientType, serverType;
        if (args.length != 2) {
            System.out.println("Usage Client Main <serverType> <clientType>");
            System.exit(1);
        }
        // host = args[0];
        // port = Integer.parseInt(args[1].trim());
        serverType = args[0].trim();
        clientType = args[1].trim();

        // ClientMain client = new ClientMain(host, port, clientType);
        ClientMain client = new ClientMain(clientType, serverType);
        client.initializeConnection();
        client.displayMenu(clientType);
        client.closeConnection();
    }

    public ClientMain(String host, int port, String mode) {
        this.host = host;
        this.port = port;
        this.clientType = mode;
    }

    public ClientMain(String clientType, String serverType) throws InterruptedException, IOException {
        this.clientType = clientType;
        this.serverType = serverType;
        fetchConnectionDetails();
    }

    private void fetchConnectionDetails() throws InterruptedException, IOException {
        NameServerClient client = new NameServerClient(NAME_SERVICE_ADDRESS);
        NameServerClient.ServiceDetails serviceDetails = client.findService(ITEM_SERVICE);
        host = serviceDetails.getIPAddress();
        port = serviceDetails.getPort();
        System.out.println("Host: " + host + ", port: " + port);
    }

    private String getServiceName() {
        switch (this.clientType) {
            case SELLER:
                return ITEM_SERVICE;
            case CUSTOMER:
                return CUSTOMER_SERVICE;
            case FACTORY:
                return ORDER_SERVICE;
            default:
                return null;
        }
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