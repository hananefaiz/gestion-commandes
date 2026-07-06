package com.esi.commerce.service;

import com.esi.commerce.exception.*;
import com.esi.commerce.model.Client;
import com.esi.commerce.model.Commande;
import com.esi.commerce.model.Produit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommandeServiceTest {

    private ProduitService produitService;
    private ClientService clientService;
    private CommandeService commandeService;

    @BeforeEach
    void setUp() {
        produitService = new ProduitService();
        clientService = new ClientService();
        commandeService = new CommandeService(produitService, clientService);

        produitService.ajouter(new Produit("P1", "Clavier", 100.0, 10));
        produitService.ajouter(new Produit("P2", "Souris", 50.0, 10));
        clientService.ajouter(new Client("C1", "Alice", "alice@mail.com", false));
        clientService.ajouter(new Client("C2", "Bob", "bob@mail.com", true));
    }

    @Test
    void passerCommande_casNominal_calculeLeBonMontant() {
        Commande c = commandeService.passerCommande("C1", Map.of("P1", 1), null);
        assertEquals(100.0, c.getMontantTotal(), 0.01);
        assertEquals(9, produitService.trouverParId("P1").get().getStock());
    }

    @Test
    void passerCommande_vipBronze_appliqueRemiseDe10Pourcent() {
        Commande c = commandeService.passerCommande("C2", Map.of("P1", 1), null);
        assertEquals(90.0, c.getMontantTotal(), 0.01);
    }

    @Test
    void passerCommande_vipArgent_appliqueRemiseDe15Pourcent() {
        clientService.trouverParId("C2").get().setPointsFidelite(600);
        Commande c = commandeService.passerCommande("C2", Map.of("P1", 1), null);
        assertEquals(85.0, c.getMontantTotal(), 0.01);
    }

    @Test
    void passerCommande_vipOr_appliqueRemiseDe20Pourcent() {
        clientService.trouverParId("C2").get().setPointsFidelite(1500);
        Commande c = commandeService.passerCommande("C2", Map.of("P1", 1), null);
        assertEquals(80.0, c.getMontantTotal(), 0.01);
    }

    @Test
    void passerCommande_grosMontantVIP_cumuleLesDeuxRemises() {
        produitService.ajouter(new Produit("P3", "Ecran", 600.0, 5));
        Commande c = commandeService.passerCommande("C2", Map.of("P3", 1), null);
        assertEquals(510.0, c.getMontantTotal(), 0.01);
    }

    @Test
    void passerCommande_avecCodePromo_deduitLeMontantFixe() {
        Commande c = commandeService.passerCommande("C1", Map.of("P1", 1), "PROMO20");
        assertEquals(80.0, c.getMontantTotal(), 0.01);
    }

    @Test
    void passerCommande_promoNoelPetitMontant_deduit15() {
        Commande c = commandeService.passerCommande("C1", Map.of("P2", 1), "PROMONOEL");
        assertEquals(35.0, c.getMontantTotal(), 0.01);
    }

    @Test
    void passerCommande_promoNoelGrandMontant_deduit30() {
        produitService.ajouter(new Produit("P4", "Sac", 300.0, 5));
        Commande c = commandeService.passerCommande("C1", Map.of("P4", 1), "PROMONOEL");
        assertEquals(270.0, c.getMontantTotal(), 0.01);
    }

    @Test
    void passerCommande_panierVide_devraitLeverUneException() {
        assertThrows(PanierVideException.class,
                () -> commandeService.passerCommande("C1", Map.of(), null));
    }

    @Test
    void passerCommande_quantiteNegative_devraitLeverUneException() {
        assertThrows(QuantiteInvalideException.class,
                () -> commandeService.passerCommande("C1", Map.of("P1", -1), null));
    }

    @Test
    void passerCommande_stockInsuffisant_devraitLeverUneException() {
        assertThrows(ProduitIndisponibleException.class,
                () -> commandeService.passerCommande("C1", Map.of("P1", 999), null));
    }

    @Test
    void passerCommande_clientInexistant_devraitLeverUneException() {
        assertThrows(ClientInvalideException.class,
                () -> commandeService.passerCommande("INCONNU", Map.of("P1", 1), null));
    }

    @Test
    void passerCommande_remisesCumulees_neDescendJamaisSousZero() {
        Client vip = clientService.trouverParId("C2").get();
        vip.setPointsFidelite(1500);
        produitService.ajouter(new Produit("P5", "Stylo", 5.0, 100));
        Commande c = commandeService.passerCommande("C2", Map.of("P5", 1), "PROMO50");
        assertTrue(c.getMontantTotal() >= 0, "Le montant total ne doit jamais être négatif");
    }

    @Test
    void passerCommande_pointsFideliteSuffisants_appliqueRemiseEtReinitialisePoints() {
        Client vip = clientService.trouverParId("C2").get();
        vip.setPointsFidelite(150);
        produitService.ajouter(new Produit("P6", "Casque", 200.0, 10));
        Commande c = commandeService.passerCommande("C2", Map.of("P6", 1), null);
        // 200 - 10% (bronze) = 180, puis -15 fidelite = 165
        assertEquals(165.0, c.getMontantTotal(), 0.01);
        assertEquals(50, vip.getPointsFidelite());
    }

    @Test
    void passerCommande_fraisDeLivraison_ajoutesSelonLePoids() {
        produitService.ajouter(new Produit("P7", "Livre", 10.0, 20));
        // 6 unités => tranche "poids > 5 et <= 10" => 10 de frais pour un non-VIP
        Commande c = commandeService.passerCommande("C1", Map.of("P7", 6), null);
        assertEquals(70.0, c.getMontantTotal(), 0.01); // 60 (produits) + 10 (livraison)
    }

    @Test
    void genererDevis_neModifiePasLeStockEtReutiliseLeMemeCalcul() {
        int stockAvant = produitService.trouverParId("P1").get().getStock();
        double devis = commandeService.genererDevis("C2", Map.of("P1", 1), null);
        assertEquals(90.0, devis, 0.01);
        assertEquals(stockAvant, produitService.trouverParId("P1").get().getStock(),
                "Un devis ne doit pas modifier le stock reel");
    }

    @Test
    void annulerCommande_restitueExactementLaQuantiteVendue() {
        Commande c = commandeService.passerCommande("C1", Map.of("P2", 3), null);
        assertEquals(7, produitService.trouverParId("P2").get().getStock());

        commandeService.annulerCommande(c.getId());

        assertEquals(10, produitService.trouverParId("P2").get().getStock(),
                "Le stock doit etre restitue avec la quantite exacte vendue");
    }

    @Test
    void annulerCommande_dejaAnnulee_devraitLeverUneException() {
        Commande c = commandeService.passerCommande("C1", Map.of("P1", 1), null);
        commandeService.annulerCommande(c.getId());
        assertThrows(CommandeDejaAnnuleeException.class,
                () -> commandeService.annulerCommande(c.getId()));
    }

    @Test
    void annulerCommande_inexistante_devraitLeverUneException() {
        assertThrows(CommandeIntrouvableException.class,
                () -> commandeService.annulerCommande("INCONNUE"));
    }

    @Test
    void modifierCommande_recalculeLeMontantAvecLaMemeLogique() {
        Commande c = commandeService.passerCommande("C1", Map.of("P1", 1), null);
        Commande modifiee = commandeService.modifierCommande(c.getId(), Map.of("P2", 2));
        assertEquals(100.0, modifiee.getMontantTotal(), 0.01);
    }

    @Test
    void calculerMoyennePanier_sansCommande_retourneZeroSansLeverException() {
        assertEquals(0.0, commandeService.calculerMoyennePanier("C1"), 0.01);
    }

    @Test
    void calculerMoyennePanier_avecPlusieursCommandes_calculeLaMoyenne() {
        commandeService.passerCommande("C1", Map.of("P1", 1), null);
        commandeService.passerCommande("C1", Map.of("P2", 1), null);
        assertEquals(75.0, commandeService.calculerMoyennePanier("C1"), 0.01);
    }
}
