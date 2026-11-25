package com.ticketing.inventory.exception;

public class CapacityConflictException extends RuntimeException {

    public CapacityConflictException(String message) {
        super(message);
    }
}