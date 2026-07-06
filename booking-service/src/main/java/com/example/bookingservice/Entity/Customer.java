package com.example.bookingservice.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.persistence.autoconfigure.EntityScan;


/**
CREATE TABLE customer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL
);
*/

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "customer")

public class Customer {

    @Id
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String address;
    private String phoneNumber;



}
