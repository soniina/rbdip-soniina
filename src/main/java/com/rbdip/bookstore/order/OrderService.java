package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final String DEFAULT_CUSTOMER_TYPE = "regular";

    private final OrderValidator orderValidator;
    private final OrderItemResolver orderItemResolver;
    private final PricingCalculator pricingCalculator;
    private final OrderPersistenceService orderPersistenceService;
    private final OrderNotifier orderNotifier;

    public OrderService(
            OrderValidator orderValidator,
            OrderItemResolver orderItemResolver,
            PricingCalculator pricingCalculator,
            OrderPersistenceService orderPersistenceService,
            OrderNotifier orderNotifier) {
        this.orderValidator = orderValidator;
        this.orderItemResolver = orderItemResolver;
        this.pricingCalculator = pricingCalculator;
        this.orderPersistenceService = orderPersistenceService;
        this.orderNotifier = orderNotifier;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        orderValidator.validate(request);
        List<ResolvedOrderItem> resolvedItems = orderItemResolver.resolve(request.items());
        List<PricingCalculator.LineItem> lineItems = resolvedItems.stream()
                .map(item -> new PricingCalculator.LineItem(item.product().getPrice(), item.quantity()))
                .toList();

        BigDecimal total = pricingCalculator.calculateOrderTotal(
                lineItems, customerType(request), request.couponCode());
        Order order = orderPersistenceService.save(request, resolvedItems);
        orderNotifier.sendConfirmation(request.customerFullName(), order.getId(), total);

        return order;
    }

    private String customerType(CreateOrderRequest request) {
        return request.customerType() == null ? DEFAULT_CUSTOMER_TYPE : request.customerType();
    }
}
