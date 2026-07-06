package com.esi.commerce.exception;

public class CommandeIntrouvableException extends CommerceException {
    public CommandeIntrouvableException(String commandeId) {
        super("La commande " + commandeId + " est introuvable");
    }
}
