package com.ticketing.booking.exception;

public class NotEnoughTicketsException extends RuntimeException {

    public NotEnoughTicketsException(String message) {
        super(message);
    }
}