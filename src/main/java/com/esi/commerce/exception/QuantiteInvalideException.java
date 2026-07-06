package com.esi.commerce.exception;

public class QuantiteInvalideException extends CommerceException {
    public QuantiteInvalideException(String produitId, int quantite) {
        super("Quantite invalide (" + quantite + ") pour le produit " + produitId);
    }
}
