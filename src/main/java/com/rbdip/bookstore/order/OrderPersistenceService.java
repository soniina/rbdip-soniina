package com.rbdip.bookstore.order;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderPersistenceService {

    private static final String NEW_ORDER_STATUS = "new";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderPersistenceService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    Order save(CreateOrderRequest request, List<ResolvedOrderItem> resolvedItems) {
        Order order = new Order(
                request.customerFullName(), request.customerAddress(), request.customerPhone(), NEW_ORDER_STATUS);
        Order savedOrder = orderRepository.save(order);

        for (ResolvedOrderItem item : resolvedItems) {
            orderItemRepository.save(new OrderItem(
                    savedOrder.getId(), item.product().getName(), item.product().getPrice(), item.quantity()));
        }
        return savedOrder;
    }
}
