package com.cs4092.dddproject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductService productService; // For interacting with products
    private final CustomerService customerService; // For accessing customer data

    // Dependency injection
    @Autowired
    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                        ProductService productService, CustomerService customerService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productService = productService;
        this.customerService = customerService;
    }

    // Place a new order (assuming validation and processing happen within)
    public Order placeOrder(Customer customer, CreditCard creditCard, Order order) {
        order.setCustomer(customer); // Set the customer for the order
        order.setCreditCard(creditCard); // Set the credit card for the order

        // (Validation and Processing logic - refer to previous discussions)
        // - Validate order items and product quantities
        // - Process payment
        // TODO: add support for processOrder() and Stock on Product

        // Save the order
        order = orderRepository.save(order);

        // Manually add the order to the customer's list (assuming modifiable list)
        List<Order> customerOrders = customer.getOrders();
        customerOrders.add(order);
        customer.setOrders(customerOrders); // Update the customer object with modified list

        // Manually add the order to the credit card's list (assuming modifiable list)
        List<Order> creditCardOrders = creditCard.getOrders();
        creditCardOrders.add(order);
        creditCard.setOrders(creditCardOrders); // Update the credit card object with modified list

        return order;
    }

    public void processOrder(Order order, Long targetWarehouseId) {
        // 1. Validate order status (optional for this example)
        if (!order.getOrderStatus().equals("placed")) {
            throw new IllegalStateException("Order cannot be processed in current status: " + order.getOrderStatus());
        }

        // 2. Check product availability and quantity (interaction with ProductService might be needed)
        List<OrderItem> orderItems = order.getOrderItems();
        for (OrderItem orderItem : orderItems) {
            Product product = orderItem.getProduct();
            int quantity = orderItem.getQuantity();
            if (!productService.isAvailable(product.getProductId(), quantity)) {
                throw new RuntimeException("Insufficient quantity available for product: " + product.getProductName());
            }
        }

        // 3. Update inventory (interaction with ProductService might be needed)
        for (OrderItem orderItem : orderItems) {
            Product product = orderItem.getProduct();
            int quantity = orderItem.getQuantity();
            productService.updateInventory(product.getProductId(), -quantity, targetWarehouseId);
        }

        // 4. Update order status
        order.setOrderStatus("processing"); // Replace with appropriate status

        // 5. (Optional) Trigger fulfillment actions (e.g., sending notifications, generating labels)

        // 6. Save the updated order
        orderRepository.save(order);
    }

    // Get all orders for a customer
    public List<Order> getOrdersByCustomer(Customer customer) {
        return orderRepository.findAll().stream()
                .filter(order -> order.getCustomer().equals(customer))
                .collect(Collectors.toList());
    }

    // Get an order by its ID
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }
}
