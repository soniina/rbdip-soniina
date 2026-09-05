package com.rbdip.bookstore.order;

import com.rbdip.bookstore.customer.Customer;
import com.rbdip.bookstore.customer.CustomerRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderPersistenceService {

    private static final String NEW_ORDER_STATUS = "new";

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public OrderPersistenceService(CustomerRepository customerRepository, OrderRepository orderRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    Order save(CreateOrderRequest request, List<ResolvedOrderItem> resolvedItems) {
        Customer customer = findOrCreateCustomer(request);
        Order order = new Order(customer, NEW_ORDER_STATUS);
        for (ResolvedOrderItem item : resolvedItems) {
            order.addItem(item.product(), item.quantity());
        }
        return orderRepository.save(order);
    }

    private Customer findOrCreateCustomer(CreateOrderRequest request) {
        return customerRepository
                .findByFullNameAndAddressAndPhone(
                        request.customerFullName(), request.customerAddress(), request.customerPhone())
                .orElseGet(() -> customerRepository.save(new Customer(
                        request.customerFullName(), request.customerAddress(), request.customerPhone())));
    }
}
