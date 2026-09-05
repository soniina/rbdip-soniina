package com.rbdip.bookstore.order;

import com.rbdip.bookstore.product.Product;

record ResolvedOrderItem(Product product, int quantity) {
}
