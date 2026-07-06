package com.esi.commerce.exception;

public class ClientInvalideException extends CommerceException {
    public ClientInvalideException(String clientId) {
        super("Le client " + clientId + " est introuvable");
    }
}
