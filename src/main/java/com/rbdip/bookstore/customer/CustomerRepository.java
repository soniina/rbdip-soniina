package com.rbdip.bookstore.customer;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("""
            SELECT c FROM Customer c
            WHERE CASE WHEN c.firstName IS NULL THEN c.fullName
                       WHEN c.lastName IS NULL THEN c.firstName
                       ELSE CONCAT(c.firstName, ' ', c.lastName) END = :fullName
              AND (c.address = :address OR (c.address IS NULL AND :address IS NULL))
              AND (c.phone = :phone OR (c.phone IS NULL AND :phone IS NULL))
            """)
    Optional<Customer> findByFullNameAndAddressAndPhone(String fullName, String address, String phone);
}
