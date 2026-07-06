package com.esi.commerce.exception;

public class CommandeDejaAnnuleeException extends CommerceException {
    public CommandeDejaAnnuleeException(String commandeId) {
        super("La commande " + commandeId + " est deja annulee");
    }
}
