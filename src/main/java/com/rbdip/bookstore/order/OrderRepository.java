package com.rbdip.bookstore.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            SELECT o FROM Order o
            JOIN FETCH o.customer
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.product
            """)
    List<Order> findAllWithItems();

    @Query("""
            SELECT COUNT(i) > 0 FROM OrderItem i
            JOIN i.order.customer c
            WHERE i.product.id = :productId
              AND CASE WHEN c.lastName IS NULL THEN c.firstName
                       ELSE CONCAT(c.firstName, ' ', c.lastName) END = :authorName
            """)
    boolean hasPurchasedProduct(String authorName, Long productId);
}
