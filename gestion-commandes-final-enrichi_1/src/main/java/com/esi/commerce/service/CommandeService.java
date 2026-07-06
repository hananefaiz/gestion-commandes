package com.esi.commerce.service;

import com.esi.commerce.exception.CommandeDejaAnnuleeException;
import com.esi.commerce.exception.CommandeIntrouvableException;
import com.esi.commerce.exception.PanierVideException;
import com.esi.commerce.exception.QuantiteInvalideException;
import com.esi.commerce.model.Client;
import com.esi.commerce.model.Commande;
import com.esi.commerce.model.LigneCommande;
import com.esi.commerce.model.Produit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service métier final : produits/clients séparés (SRP), exceptions dédiées,
 * calcul de prix entièrement factorisé (0 duplication), toutes les règles
 * métier enrichies conservées (paliers VIP, frais de livraison, devis).
 */
public class CommandeService {

    public static final double REMISE_VIP_BRONZE = 0.10;
    public static final double REMISE_VIP_ARGENT = 0.15;
    public static final double REMISE_VIP_OR = 0.20;
    public static final int SEUIL_POINTS_ARGENT = 500;
    public static final int SEUIL_POINTS_OR = 1000;
    public static final double REMISE_GROS_MONTANT = 0.05;
    public static final double SEUIL_GROS_MONTANT = 500;
    public static final int SEUIL_POINTS_FIDELITE = 100;
    public static final double REMISE_FIDELITE = 15;
    public static final int DIVISEUR_POINTS = 10;

    private final ProduitService produitService;
    private final ClientService clientService;
    private final List<Commande> commandes = new ArrayList<>();

    public CommandeService(ProduitService produitService, ClientService clientService) {
        this.produitService = produitService;
        this.clientService = clientService;
    }

    public Commande passerCommande(String clientId, Map<String, Integer> panier, String codePromo) {
        if (panier == null || panier.isEmpty()) {
            throw new PanierVideException();
        }
        Client client = clientService.trouverOuLever(clientId);
        Commande commande = new Commande("C" + (commandes.size() + 1), client);

        LignesResultat resultat = remplirLignes(panier, commande);
        double total = calculerMontantFinal(client, resultat.sousTotal, codePromo);
        total += calculerFraisLivraison(resultat.poidsTotal, client);
        total = Math.max(0, total);

        client.ajouterPoints((int) (resultat.sousTotal / DIVISEUR_POINTS));
        commande.setMontantTotal(total);
        commande.setCodePromo(codePromo);
        commande.setStatut("VALIDEE");
        commandes.add(commande);
        return commande;
    }

    public double genererDevis(String clientId, Map<String, Integer> panier, String codePromo) {
        Client client = clientService.trouverOuLever(clientId);
        double sousTotal = 0;
        if (panier != null) {
            for (Map.Entry<String, Integer> entree : panier.entrySet()) {
                var produit = produitService.trouverParId(entree.getKey());
                if (produit.isPresent() && produit.get().getStock() >= entree.getValue()) {
                    sousTotal += produit.get().getPrix() * entree.getValue();
                }
            }
        }
        return Math.max(0, calculerMontantFinal(client, sousTotal, codePromo));
    }

    public Commande modifierCommande(String commandeId, Map<String, Integer> nouveauPanier) {
        Commande commande = trouverCommandeOuLever(commandeId);
        LignesResultat resultat = remplirLignes(nouveauPanier, commande);
        double total = calculerMontantFinal(commande.getClient(), resultat.sousTotal, commande.getCodePromo());
        commande.setMontantTotal(Math.max(0, total));
        return commande;
    }

    public Commande annulerCommande(String commandeId) {
        Commande commande = trouverCommandeOuLever(commandeId);
        if ("ANNULEE".equals(commande.getStatut())) {
            throw new CommandeDejaAnnuleeException(commandeId);
        }
        for (LigneCommande ligne : commande.getLignes()) {
            Produit produit = ligne.getProduit();
            produit.setStock(produit.getStock() + ligne.getQuantite());
        }
        commande.setStatut("ANNULEE");
        return commande;
    }

    public double calculerMoyennePanier(String clientId) {
        List<Commande> commandesDuClient = commandes.stream()
                .filter(c -> c.getClient().getId().equals(clientId))
                .toList();
        if (commandesDuClient.isEmpty()) {
            return 0.0;
        }
        double total = commandesDuClient.stream().mapToDouble(Commande::getMontantTotal).sum();
        return total / commandesDuClient.size();
    }

    public List<Commande> getCommandes() { return List.copyOf(commandes); }

    // ---- Logique factorisée, partagée par passerCommande(), genererDevis() et modifierCommande() ----

    private LignesResultat remplirLignes(Map<String, Integer> panier, Commande commande) {
        double sousTotal = 0;
        int poidsTotal = 0;
        for (Map.Entry<String, Integer> entree : panier.entrySet()) {
            String produitId = entree.getKey();
            int quantite = entree.getValue();
            if (quantite <= 0) {
                throw new QuantiteInvalideException(produitId, quantite);
            }
            Produit produit = produitService.trouverDisponibleOuLever(produitId, quantite);
            produit.setStock(produit.getStock() - quantite);
            sousTotal += produit.getPrix() * quantite;
            poidsTotal += quantite;
            commande.ajouterLigne(new LigneCommande(produit, quantite));
        }
        return new LignesResultat(sousTotal, poidsTotal);
    }

    private double calculerMontantFinal(Client client, double sousTotal, String codePromo) {
        double total = appliquerRemiseVipEtVolume(client, sousTotal);
        total = appliquerCodePromo(total, codePromo);
        return appliquerRemiseFidelite(client, total);
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
            client.consommerPoints(SEUIL_POINTS_FIDELITE);
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

    private Commande trouverCommandeOuLever(String commandeId) {
        return commandes.stream()
                .filter(c -> c.getId().equals(commandeId))
                .findFirst()
                .orElseThrow(() -> new CommandeIntrouvableException(commandeId));
    }

    private record LignesResultat(double sousTotal, int poidsTotal) {}
}
