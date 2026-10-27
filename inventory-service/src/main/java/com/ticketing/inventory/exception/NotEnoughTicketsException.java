package com.ticketing.inventory.exception;

public class NotEnoughTicketsException extends RuntimeException {

    public NotEnoughTicketsException(String message) {
        super(message);
    }
}