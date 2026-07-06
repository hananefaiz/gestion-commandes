package com.esi.commerce.service;

import com.esi.commerce.model.Client;
import com.esi.commerce.model.Commande;
import com.esi.commerce.model.LigneCommande;
import com.esi.commerce.model.Produit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ÉTAPE INTERMÉDIAIRE (2/3) du refactoring :
 * - Bugs corrigés (restitution de stock exacte, division par zéro, montant négatif)
 * - Constantes magiques extraites
 * - Duplication éliminée : genererDevis(), passerCommande() et modifierCommande()
 *   partagent maintenant les mêmes méthodes privées de calcul
 * PAS ENCORE fait (voir version finale) : séparation en plusieurs services (SRP),
 * exceptions métier dédiées, tests unitaires.
 */
public class CommandeService {

    private static final double REMISE_VIP_BRONZE = 0.10;
    private static final double REMISE_VIP_ARGENT = 0.15;
    private static final double REMISE_VIP_OR = 0.20;
    private static final int SEUIL_POINTS_ARGENT = 500;
    private static final int SEUIL_POINTS_OR = 1000;
    private static final double REMISE_GROS_MONTANT = 0.05;
    private static final double SEUIL_GROS_MONTANT = 500;
    private static final int SEUIL_POINTS_FIDELITE = 100;
    private static final double REMISE_FIDELITE = 15;
    private static final int DIVISEUR_POINTS = 10;

    private List<Produit> produits = new ArrayList<>();
    private List<Client> clients = new ArrayList<>();
    private List<Commande> commandes = new ArrayList<>();

    public void ajouterProduit(Produit p) { produits.add(p); }
    public void ajouterClient(Client c) { clients.add(c); }

    public Commande passerCommande(String clientId, Map<String, Integer> panier, String codePromo) {
        Client client = trouverClient(clientId);
        if (client == null) {
            System.out.println("Client introuvable");
            return null;
        }

        Commande commande = new Commande("C" + (commandes.size() + 1), client);
        double[] sousTotalEtPoids = remplirLignes(panier, commande);
        double sousTotal = sousTotalEtPoids[0];
        int poidsTotal = (int) sousTotalEtPoids[1];

        double total = calculerMontantFinal(client, sousTotal, codePromo);
        total += calculerFraisLivraison(poidsTotal, client);
        total = Math.max(0, total);

        client.setPointsFidelite(client.getPointsFidelite() + (int) (sousTotal / DIVISEUR_POINTS));
        commande.setMontantTotal(total);
        commande.setCodePromo(codePromo);
        commande.setStatut("VALIDEE");
        commandes.add(commande);
        return commande;
    }

    // Ne duplique plus le calcul : réutilise calculerMontantFinal() comme passerCommande()
    public double genererDevis(String clientId, Map<String, Integer> panier, String codePromo) {
        Client client = trouverClient(clientId);
        if (client == null) {
            System.out.println("Client introuvable");
            return 0;
        }
        double sousTotal = 0;
        for (Map.Entry<String, Integer> entree : panier.entrySet()) {
            Produit produit = trouverProduit(entree.getKey());
            int quantite = entree.getValue();
            if (produit != null && produit.getStock() >= quantite) {
                sousTotal += produit.getPrix() * quantite;
            }
        }
        return Math.max(0, calculerMontantFinal(client, sousTotal, codePromo));
    }

    public boolean modifierCommande(String commandeId, Map<String, Integer> nouveauPanier) {
        Commande commande = trouverCommande(commandeId);
        if (commande == null) {
            return false;
        }
        double[] sousTotalEtPoids = remplirLignes(nouveauPanier, commande);
        double total = calculerMontantFinal(commande.getClient(), sousTotalEtPoids[0], commande.getCodePromo());
        commande.setMontantTotal(Math.max(0, total));
        return true;
    }

    public boolean annulerCommande(String commandeId) {
        Commande commande = trouverCommande(commandeId);
        if (commande == null) {
            return false;
        }
        if (commande.getStatut().equals("VALIDEE")) {
            for (LigneCommande ligne : commande.getLignes()) {
                // Bug corrigé : on restitue la quantité exacte vendue.
                ligne.getProduit().setStock(ligne.getProduit().getStock() + ligne.getQuantite());
            }
        }
        commande.setStatut("ANNULEE");
        return true;
    }

    public double calculerMoyennePanier(String clientId) {
        double total = 0;
        int count = 0;
        for (Commande c : commandes) {
            if (c.getClient().getId().equals(clientId)) {
                total += c.getMontantTotal();
                count++;
            }
        }
        // Bug corrigé : ne divise plus par zéro si le client n'a aucune commande.
        return count == 0 ? 0.0 : total / count;
    }

    public List<Produit> getProduits() { return produits; }
    public List<Client> getClients() { return clients; }
    public List<Commande> getCommandes() { return commandes; }

    // ---- Méthodes privées factorisées : plus aucune duplication entre
    //      passerCommande(), genererDevis() et modifierCommande() ----

    private double[] remplirLignes(Map<String, Integer> panier, Commande commande) {
        double sousTotal = 0;
        int poidsTotal = 0;
        for (Map.Entry<String, Integer> entree : panier.entrySet()) {
            Produit produit = trouverProduit(entree.getKey());
            int quantite = entree.getValue();
            if (produit != null && quantite > 0 && produit.getStock() >= quantite) {
                produit.setStock(produit.getStock() - quantite);
                sousTotal += produit.getPrix() * quantite;
                poidsTotal += quantite;
                commande.ajouterLigne(new LigneCommande(produit, quantite));
            }
        }
        return new double[] { sousTotal, poidsTotal };
    }

    private double calculerMontantFinal(Client client, double sousTotal, String codePromo) {
        double total = appliquerRemiseVipEtVolume(client, sousTotal);
        total = appliquerCodePromo(total, codePromo);
        total = appliquerRemiseFidelite(client, total);
        return total;
    }

    private double appliquerRemiseVipEtVolume(Client client, double sousTotal) {
        double remise = 0;
        if (client.isEstVIP()) {
            if (client.getPointsFidelite() > SEUIL_POINTS_OR) {
                remise = REMISE_VIP_OR;
            } else if (client.getPointsFidelite() > SEUIL_POINTS_ARGENT) {
                remise = REMISE_VIP_ARGENT;
            } else {
                remise = REMISE_VIP_BRONZE;
            }
        }
        if (sousTotal > SEUIL_GROS_MONTANT) {
            remise += REMISE_GROS_MONTANT;
        }
        return sousTotal - (sousTotal * remise);
    }

    private double appliquerCodePromo(double total, String codePromo) {
        if (codePromo == null) {
            return total;
        }
        return switch (codePromo) {
            case "PROMO10" -> total - 10;
            case "PROMO20" -> total - 20;
            case "PROMO50" -> total - 50;
            case "PROMONOEL" -> total - (total > 200 ? 30 : 15);
            default -> total;
        };
    }

    private double appliquerRemiseFidelite(Client client, double total) {
        if (client.getPointsFidelite() >= SEUIL_POINTS_FIDELITE) {
            client.setPointsFidelite(client.getPointsFidelite() - SEUIL_POINTS_FIDELITE);
            return total - REMISE_FIDELITE;
        }
        return total;
    }

    private double calculerFraisLivraison(int poidsTotal, Client client) {
        if (poidsTotal <= 1) return 0;
        if (poidsTotal <= 5) return client.isEstVIP() ? 2.5 : 5;
        if (poidsTotal <= 10) return client.isEstVIP() ? 5 : 10;
        return client.isEstVIP() ? 8 : 15;
    }

    private Client trouverClient(String clientId) {
        for (Client c : clients) if (c.getId().equals(clientId)) return c;
        return null;
    }

    private Produit trouverProduit(String produitId) {
        for (Produit p : produits) if (p.getId().equals(produitId)) return p;
        return null;
    }

    private Commande trouverCommande(String commandeId) {
        for (Commande c : commandes) if (c.getId().equals(commandeId)) return c;
        return null;
    }
}
