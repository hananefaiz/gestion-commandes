package com.esi.commerce.service;

import com.esi.commerce.exception.ProduitIndisponibleException;
import com.esi.commerce.model.Produit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProduitService {

    private final List<Produit> produits = new ArrayList<>();

    public void ajouter(Produit produit) { produits.add(produit); }

    public Optional<Produit> trouverParId(String produitId) {
        return produits.stream().filter(p -> p.getId().equals(produitId)).findFirst();
    }

    public Produit trouverDisponibleOuLever(String produitId, int quantite) {
        return trouverParId(produitId)
                .filter(p -> p.getStock() >= quantite)
                .orElseThrow(() -> new ProduitIndisponibleException(produitId));
    }

    public List<Produit> getProduits() { return List.copyOf(produits); }
}
