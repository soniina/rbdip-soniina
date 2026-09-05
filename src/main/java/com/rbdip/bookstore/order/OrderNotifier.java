package com.rbdip.bookstore.order;

import java.math.BigDecimal;

public interface OrderNotifier {

    void sendConfirmation(String customerName, Long orderId, BigDecimal total);
}
