package com.rbdip.bookstore.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {

    private static final int ADDRESS_MAX_LENGTH = 500;
    private static final int PHONE_MAX_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(length = ADDRESS_MAX_LENGTH)
    private String address;

    @Column(length = PHONE_MAX_LENGTH)
    private String phone;

    protected Customer() {
        // for JPA
    }

    public Customer(String fullName, String address, String phone) {
        this.fullName = fullName;
        int separator = fullName.indexOf(' ');
        this.firstName = separator < 0 ? fullName : fullName.substring(0, separator);
        if (separator >= 0) {
            this.lastName = fullName.substring(separator + 1);
        }
        this.address = address;
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        if (firstName == null) {
            return fullName;
        }
        return lastName == null ? firstName : firstName + " " + lastName;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }
}
