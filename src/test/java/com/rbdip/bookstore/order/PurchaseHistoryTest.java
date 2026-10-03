package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbdip.bookstore.reference.AbstractIntegrationTest;
import com.rbdip.bookstore.review.PurchaseHistory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PurchaseHistoryTest extends AbstractIntegrationTest {

    @Autowired
    private PurchaseHistory purchaseHistory;

    @ParameterizedTest
    @CsvSource({"Ivan Petrov", "Madonna", "'Anna  Maria Smith'", "'Trailing '"})
    void findsPurchaseOnlyForMatchingAuthorAndProduct(String authorName) {
        Long purchasedProductId = createProduct("Clean Code");
        Long otherProductId = createProduct("Refactoring");
        CreateOrderRequest request = new CreateOrderRequest(
                authorName, "Test Address", null, "regular", null,
                List.of(new CreateOrderRequest.Item(purchasedProductId, 1)));
        ResponseEntity<Map> response = restTemplate.postForEntity("/orders", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(purchaseHistory.hasPurchasedProduct(authorName, purchasedProductId)).isTrue();
        assertThat(purchaseHistory.hasPurchasedProduct(authorName, otherProductId)).isFalse();
        assertThat(purchaseHistory.hasPurchasedProduct("Another Customer", purchasedProductId)).isFalse();
        assertThat(purchaseHistory.hasPurchasedProduct(null, purchasedProductId)).isFalse();
    }

    @Test
    void returnsFalseWhenThereAreNoOrders() {
        Long productId = createProduct("Clean Code");

        assertThat(purchaseHistory.hasPurchasedProduct("Ivan Petrov", productId)).isFalse();
    }

    private Long createProduct(String name) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/products", Map.of("name", name, "price", new BigDecimal("35.00")), Map.class);
        return Long.valueOf(response.getBody().get("id").toString());
    }
}
