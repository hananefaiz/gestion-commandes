package com.esi.commerce.exception;

public class ProduitIndisponibleException extends CommerceException {
    public ProduitIndisponibleException(String produitId) {
        super("Le produit " + produitId + " est introuvable ou en stock insuffisant");
    }
}
