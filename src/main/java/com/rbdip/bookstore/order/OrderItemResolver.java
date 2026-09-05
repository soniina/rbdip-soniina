package com.rbdip.bookstore.order;

import com.rbdip.bookstore.product.Product;
import com.rbdip.bookstore.product.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderItemResolver {

    private static final int DEFAULT_QUANTITY = 1;

    private final ProductRepository productRepository;

    public OrderItemResolver(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    List<ResolvedOrderItem> resolve(List<CreateOrderRequest.Item> items) {
        List<ResolvedOrderItem> resolvedItems = new ArrayList<>();
        for (CreateOrderRequest.Item item : items) {
            Product product = findProduct(item.productId());
            int quantity = item.quantity() == null ? DEFAULT_QUANTITY : item.quantity();
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be positive");
            }
            resolvedItems.add(new ResolvedOrderItem(product, quantity));
        }
        return resolvedItems;
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("product " + productId + " not found"));
    }
}
