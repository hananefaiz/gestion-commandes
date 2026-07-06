package com.esi.commerce.service;

import com.esi.commerce.model.Client;
import com.esi.commerce.model.Commande;
import com.esi.commerce.model.Produit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommandeServiceTest {

    private CommandeService commandeService;

    @BeforeEach
    void setUp() {
        commandeService = new CommandeService();
        commandeService.ajouterProduit(new Produit("P1", "Clavier", 100.0, 10));
        commandeService.ajouterProduit(new Produit("P2", "Souris", 50.0, 10));
        commandeService.ajouterClient(new Client("C1", "Alice", "alice@mail.com", false));
        commandeService.ajouterClient(new Client("C2", "Bob", "bob@mail.com", true));
    }

    @Test
    void passerCommande_casNominal_calculeLeBonMontant() {
        Commande c = commandeService.passerCommande("C1", Map.of("P1", 1), null);
        assertNotNull(c);
        assertEquals(100.0, c.getMontantTotal(), 0.01);
    }

    @Test
    void passerCommande_vipBronze_appliqueRemiseDe10Pourcent() {
        Commande c = commandeService.passerCommande("C2", Map.of("P1", 1), null);
        assertEquals(90.0, c.getMontantTotal(), 0.01);
    }

    @Test
    void passerCommande_vipArgent_appliqueRemiseEtFidelite() {
        commandeService.getClients().get(1).setPointsFidelite(600);
        Commande c = commandeService.passerCommande("C2", Map.of("P1", 1), null);
        // 100 - 15% = 85, puis -15 fidelite = 70
        assertEquals(70.0, c.getMontantTotal(), 0.01);
    }

    @Test
    void passerCommande_vipOr_appliqueRemiseEtFidelite() {
        commandeService.getClients().get(1).setPointsFidelite(1500);
        Commande c = commandeService.passerCommande("C2", Map.of("P1", 1), null);
        // 100 - 20% = 80, puis -15 fidelite = 65
        assertEquals(65.0, c.getMontantTotal(), 0.01);
    }

    @Test
    void passerCommande_avecCodePromo_deduitLeMontantFixe() {
        Commande c = commandeService.passerCommande("C1", Map.of("P1", 1), "PROMO20");
        assertEquals(80.0, c.getMontantTotal(), 0.01);
    }

    @Test
    void passerCommande_promoNoel_appliqueLeBonPalier() {
        Commande c = commandeService.passerCommande("C1", Map.of("P2", 1), "PROMONOEL");
        assertEquals(35.0, c.getMontantTotal(), 0.01);
    }

    @Test
    void passerCommande_clientInexistant_retourneNull() {
        assertNull(commandeService.passerCommande("INCONNU", Map.of("P1", 1), null));
    }

    @Test
    void passerCommande_quantiteInvalide_ignoreLaLigne() {
        Commande c = commandeService.passerCommande("C1", Map.of("P1", -1), null);
        assertNotNull(c);
        assertEquals(0.0, c.getMontantTotal(), 0.01, "Une quantite invalide ne doit generer aucun montant");
    }

    @Test
    void passerCommande_fraisDeLivraison_ajoutesSelonLePoids() {
        Commande c = commandeService.passerCommande("C1", Map.of("P2", 6), null);
        assertEquals(310.0, c.getMontantTotal(), 0.01); // 300 (produits) + 10 (livraison, palier 6-10)
    }

    @Test
    void passerCommande_remisesCumulees_neDescendJamaisSousZero() {
        commandeService.getClients().get(1).setPointsFidelite(1500);
        commandeService.ajouterProduit(new Produit("P3", "Stylo", 5.0, 100));
        Commande c = commandeService.passerCommande("C2", Map.of("P3", 1), "PROMO50");
        assertTrue(c.getMontantTotal() >= 0, "Le montant total ne doit jamais etre negatif");
    }

    @Test
    void genererDevis_neModifiePasLeStock() {
        int stockAvant = commandeService.getProduits().get(0).getStock();
        double devis = commandeService.genererDevis("C2", Map.of("P1", 1), null);
        assertEquals(90.0, devis, 0.01);
        assertEquals(stockAvant, commandeService.getProduits().get(0).getStock());
    }

    @Test
    void genererDevis_clientInexistant_retourneZero() {
        assertEquals(0.0, commandeService.genererDevis("INCONNU", Map.of("P1", 1), null));
    }

    @Test
    void annulerCommande_restitueExactementLaQuantiteVendue() {
        Commande c = commandeService.passerCommande("C1", Map.of("P2", 3), null);
        assertEquals(7, commandeService.getProduits().get(1).getStock());

        commandeService.annulerCommande(c.getId());

        assertEquals(10, commandeService.getProduits().get(1).getStock(),
                "Le stock doit etre restitue avec la quantite exacte vendue");
    }

    @Test
    void annulerCommande_inexistante_retourneFalse() {
        assertFalse(commandeService.annulerCommande("INCONNUE"));
    }

    @Test
    void modifierCommande_recalculeLeMontantAvecLaMemeLogique() {
        Commande c = commandeService.passerCommande("C1", Map.of("P1", 1), null);
        boolean ok = commandeService.modifierCommande(c.getId(), Map.of("P2", 2));
        assertTrue(ok);
        assertEquals(100.0, c.getMontantTotal(), 0.01);
    }

    @Test
    void modifierCommande_inexistante_retourneFalse() {
        assertFalse(commandeService.modifierCommande("INCONNUE", Map.of("P1", 1)));
    }

    @Test
    void calculerMoyennePanier_sansCommande_retourneZeroSansException() {
        assertEquals(0.0, commandeService.calculerMoyennePanier("C1"), 0.01);
    }

    @Test
    void calculerMoyennePanier_avecPlusieursCommandes_calculeLaMoyenne() {
        commandeService.passerCommande("C1", Map.of("P1", 1), null);
        commandeService.passerCommande("C1", Map.of("P2", 1), null);
        assertEquals(75.0, commandeService.calculerMoyennePanier("C1"), 0.01);
    }
}
