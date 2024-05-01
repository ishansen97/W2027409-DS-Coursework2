package com.ds.coursework2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ds.coursework2.grpc.services.order.CheckOrderAvailabilityRequest;
import com.ds.coursework2.grpc.services.order.CheckOrderAvailabilityResponse;
import com.ds.coursework2.grpc.services.order.ItemOrderRequest;
import com.ds.coursework2.grpc.services.order.OrderItemsRequest;
import com.ds.coursework2.grpc.services.order.OrderItemsResponse;
import com.ds.coursework2.grpc.services.order.OrderServiceGrpc.OrderServiceImplBase;

import io.grpc.stub.StreamObserver;

public class OrderServiceImpl extends OrderServiceImplBase {
    private Inventory inventory;
    private Map<String, Integer> orderRequests;

    public OrderServiceImpl(Inventory inventory) {
        this.inventory = inventory;
        this.orderRequests = new HashMap<>();
    }

    @Override
    public void checkOrderAvailability(CheckOrderAvailabilityRequest request,
            StreamObserver<CheckOrderAvailabilityResponse> responseObserver) {

        // only the server with leader lock accepts factory order requests.
        CheckOrderAvailabilityResponse response = null;
        if (inventory.isLeader()) {
            List<ItemOrderRequest> requestedItems = request.getAvailabilityRequestList();
            boolean itemsExist = true;
            double totalPrice = 0.0f;
            for (ItemOrderRequest orderRequest : requestedItems) {
                String itemName = orderRequest.getItemName();
                Item item = inventory.getItem(itemName);
                if (item == null) {
                    itemsExist = false;
                    orderRequests = new HashMap<>();
                    break;
                }
                int quantity = orderRequest.getQuantity();
                if ((item.getQuantity() - quantity) < 0) {
                    itemsExist = false;
                    orderRequests = new HashMap<>();
                    break;
                }
                double itemAmount = item.getPrice() * quantity;
                totalPrice += itemAmount;
                // add to the Hashmap.
                orderRequests.put(itemName, quantity);
            }

            // create the response.
            response = CheckOrderAvailabilityResponse.newBuilder()
                    .setItemsExist(itemsExist)
                    .setOrderPrice(totalPrice)
                    .build();
        } else {
            System.out.println("Secondary servers will not accept factory orders.");
            response = CheckOrderAvailabilityResponse.newBuilder()
                    .setItemsExist(false)
                    .build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void orderItems(OrderItemsRequest request, StreamObserver<OrderItemsResponse> responseObserver) {
        OrderItemsResponse response = null;
        if (inventory.isLeader()) {
            if (request != null) {
                for (Entry<String, Integer> elem : orderRequests.entrySet()) {
                    inventory.decrementItemCount(elem.getKey(), elem.getValue());
                }
                orderRequests = null;
                response = OrderItemsResponse.newBuilder()
                        .setOrderSuccess(true)
                        .build();
            }
        } else {
            System.out.println("Secondary servers will not accept factory orders.");
            response = OrderItemsResponse.newBuilder()
                    .setOrderSuccess(false)
                    .build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
