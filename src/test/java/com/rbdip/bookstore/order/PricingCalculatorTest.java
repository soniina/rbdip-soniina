package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PricingCalculatorTest {

    private final PricingCalculator calculator = new PricingCalculator();

    @Test
    void doesNotApplyBulkDiscountAtQuantityThreshold() {
        BigDecimal total = calculate("10.00", 10, "regular", null);

        assertThat(total).isEqualByComparingTo("100.00");
    }

    @Test
    void appliesBulkDiscountAboveQuantityThreshold() {
        BigDecimal total = calculate("10.00", 11, "regular", null);

        assertThat(total).isEqualByComparingTo("104.50");
    }

    @ParameterizedTest
    @CsvSource({"vip, 90.00", "wholesale, 85.00", "regular, 100.00"})
    void appliesDiscountForCustomerType(String customerType, String expected) {
        BigDecimal total = calculate("100.00", 1, customerType, null);

        assertThat(total).isEqualByComparingTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"SAVE10, 90.00", "SAVE20PERCENT, 80.00", "UNKNOWN, 100.00"})
    void appliesCouponDiscount(String couponCode, String expected) {
        BigDecimal total = calculate("100.00", 1, "regular", couponCode);

        assertThat(total).isEqualByComparingTo(expected);
    }

    @Test
    void clampsTotalToZeroWhenFixedDiscountExceedsSubtotal() {
        BigDecimal total = calculate("5.00", 1, "regular", "SAVE10");

        assertThat(total).isEqualByComparingTo("0.00");
    }

    @Test
    void doesNotApplyLargeOrderDiscountAtThreshold() {
        BigDecimal total = calculate("1000.00", 1, "regular", null);

        assertThat(total).isEqualByComparingTo("1000.00");
    }

    @Test
    void appliesLargeOrderDiscountAboveThreshold() {
        BigDecimal total = calculate("1000.01", 1, "regular", null);

        assertThat(total).isEqualByComparingTo("980.01");
    }

    @Test
    void appliesDiscountsInExistingOrder() {
        BigDecimal total = calculate("100.00", 11, "vip", "SAVE10");

        assertThat(total).isEqualByComparingTo("930.50");
    }

    @ParameterizedTest
    @CsvSource({"10.004, 10.00", "10.005, 10.01", "10.006, 10.01"})
    void roundsTotalToTwoDecimalPlacesUsingHalfUp(String price, String expected) {
        BigDecimal total = calculate(price, 1, "regular", null);

        assertThat(total).isEqualByComparingTo(expected);
    }

    @Test
    void returnsZeroForEmptyOrderWithFixedDiscount() {
        BigDecimal total = calculator.calculateOrderTotal(List.of(), "regular", "SAVE10");

        assertThat(total).isEqualByComparingTo("0.00");
    }

    @ParameterizedTest
    @CsvSource({"-10.00, 1", "10.00, -1"})
    void returnsZeroForNegativeLineTotal(String price, int quantity) {
        BigDecimal total = calculate(price, quantity, "regular", null);

        assertThat(total).isEqualByComparingTo("0.00");
    }

    private BigDecimal calculate(String price, int quantity, String customerType, String couponCode) {
        return calculator.calculateOrderTotal(
                List.of(new PricingCalculator.LineItem(new BigDecimal(price), quantity)), customerType, couponCode);
    }
}
