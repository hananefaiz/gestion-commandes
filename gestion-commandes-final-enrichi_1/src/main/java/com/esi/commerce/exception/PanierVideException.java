package com.esi.commerce.exception;

public class PanierVideException extends CommerceException {
    public PanierVideException() {
        super("Le panier ne peut pas etre vide");
    }
}
