package com.rbdip.bookstore.review;

public interface PurchaseHistory {

    boolean hasPurchasedProduct(String authorName, Long productId);
}
