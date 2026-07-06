package com.esi.commerce.exception;

public abstract class CommerceException extends RuntimeException {
    protected CommerceException(String message) { super(message); }
}
