package com.esi.commerce.service;

import com.esi.commerce.model.Client;
import com.esi.commerce.model.Commande;
import com.esi.commerce.model.LigneCommande;
import com.esi.commerce.model.Produit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service unique gérant produits, clients et commandes.
 * NB : classe volontairement non refactorée (baseline du projet e-commerce).
 */
public class CommandeService {

    private List<Produit> produits = new ArrayList<>();
    private List<Client> clients = new ArrayList<>();
    private List<Commande> commandes = new ArrayList<>();

    public void ajouterProduit(Produit p) { produits.add(p); }
    public void ajouterClient(Client c) { clients.add(c); }

    // Méthode 1 : passage de commande - complexité très élevée, plusieurs bugs
    public Commande passerCommande(String clientId, Map<String, Integer> panier, String codePromo) {
        Client client = null;
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).getId().equals(clientId)) {
                client = clients.get(i);
            }
        }
        if (client == null) {
            System.out.println("Client introuvable");
            return null;
        }

        Commande commande = new Commande("C" + (commandes.size() + 1), client);
        double sousTotal = 0;

        for (Map.Entry<String, Integer> entree : panier.entrySet()) {
            Produit produit = null;
            for (int i = 0; i < produits.size(); i++) {
                if (produits.get(i).getId().equals(entree.getKey())) {
                    produit = produits.get(i);
                }
            }
            if (produit != null) {
                int quantite = entree.getValue();
                // BUG : aucune vérification que quantite > 0, une quantité négative
                // augmente le stock au lieu de le diminuer
                if (produit.getStock() >= quantite) {
                    produit.setStock(produit.getStock() - quantite);
                    sousTotal = sousTotal + produit.getPrix() * quantite;
                    commande.ajouterLigne(new LigneCommande(produit, quantite));
                } else {
                    System.out.println("Stock insuffisant pour " + produit.getNom());
                }
            }
        }

        double total = sousTotal;
        if (client.isEstVIP()) {
            if (sousTotal > 500) {
                total = total - (total * 0.10) - (total * 0.05);
            } else {
                total = total - (total * 0.10);
            }
        } else {
            if (sousTotal > 500) {
                total = total - (total * 0.05);
            }
        }

        if (codePromo != null) {
            if (codePromo.equals("PROMO10")) {
                total = total - 10;
            } else if (codePromo.equals("PROMO20")) {
                total = total - 20;
            } else if (codePromo.equals("PROMO50")) {
                total = total - 50;
            }
        }

        if (client.getPointsFidelite() >= 100) {
            total = total - 15;
            client.setPointsFidelite(client.getPointsFidelite() - 100);
        }
        // BUG : le total peut devenir négatif si plusieurs réductions se cumulent
        // (aucun plancher à 0 n'est appliqué)

        int nouveauxPoints = (int) (sousTotal / 10);
        client.setPointsFidelite(client.getPointsFidelite() + nouveauxPoints);

        commande.setMontantTotal(total);
        commande.setCodePromo(codePromo);
        commande.setStatut("VALIDEE");
        commandes.add(commande);
        return commande;
    }

    // Méthode 2 : duplique presque intégralement le calcul de prix de passerCommande
    public boolean modifierCommande(String commandeId, Map<String, Integer> nouveauPanier) {
        Commande commande = null;
        for (int i = 0; i < commandes.size(); i++) {
            if (commandes.get(i).getId().equals(commandeId)) {
                commande = commandes.get(i);
            }
        }
        if (commande == null) {
            return false;
        }

        double sousTotal = 0;
        for (Map.Entry<String, Integer> entree : nouveauPanier.entrySet()) {
            Produit produit = null;
            for (int i = 0; i < produits.size(); i++) {
                if (produits.get(i).getId().equals(entree.getKey())) {
                    produit = produits.get(i);
                }
            }
            if (produit != null) {
                int quantite = entree.getValue();
                if (produit.getStock() >= quantite) {
                    sousTotal = sousTotal + produit.getPrix() * quantite;
                }
            }
        }

        double total = sousTotal;
        Client client = commande.getClient();
        if (client.isEstVIP()) {
            if (sousTotal > 500) {
                total = total - (total * 0.10) - (total * 0.05);
            } else {
                total = total - (total * 0.10);
            }
        } else {
            if (sousTotal > 500) {
                total = total - (total * 0.05);
            }
        }

        commande.setMontantTotal(total);
        return true;
    }

    // Méthode 3 : annulation - BUG de restitution du stock (incrémente de 1 au lieu de la quantité)
    public boolean annulerCommande(String commandeId) {
        Commande commande = null;
        for (int i = 0; i < commandes.size(); i++) {
            if (commandes.get(i).getId().equals(commandeId)) {
                commande = commandes.get(i);
            }
        }
        if (commande == null) {
            return false;
        }
        if (commande.getStatut().equals("VALIDEE")) {
            for (LigneCommande ligne : commande.getLignes()) {
                // BUG : devrait restituer ligne.getQuantite(), pas 1 unité fixe
                ligne.getProduit().setStock(ligne.getProduit().getStock() + 1);
            }
        }
        commande.setStatut("ANNULEE");
        return true;
    }

    // Méthode 4 : BUG de division par zéro si le client n'a aucune commande
    public double calculerMoyennePanier(String clientId) {
        double total = 0;
        int count = 0;
        for (Commande c : commandes) {
            if (c.getClient().getId().equals(clientId)) {
                total = total + c.getMontantTotal();
                count = count + 1;
            }
        }
        return total / count; // division par zéro possible si count == 0
    }

    public List<Produit> getProduits() { return produits; }
    public List<Client> getClients() { return clients; }
    public List<Commande> getCommandes() { return commandes; }
}
