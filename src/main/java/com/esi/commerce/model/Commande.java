package com.esi.commerce.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Commande {
    private String id;
    private Client client;
    private List<LigneCommande> lignes = new ArrayList<>();
    private String statut; // "EN_COURS", "VALIDEE", "ANNULEE"
    private LocalDate dateCommande;
    private double montantTotal;
    private String codePromo;

    public Commande(String id, Client client) {
        this.id = id;
        this.client = client;
        this.statut = "EN_COURS";
        this.dateCommande = LocalDate.now();
    }

    public String getId() { return id; }
    public Client getClient() { return client; }
    public List<LigneCommande> getLignes() { return lignes; }
    public void ajouterLigne(LigneCommande l) { lignes.add(l); }
    public String getStatut() { return statut; }
    public void setStatut(String s) { this.statut = s; }
    public LocalDate getDateCommande() { return dateCommande; }
    public double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(double m) { this.montantTotal = m; }
    public String getCodePromo() { return codePromo; }
    public void setCodePromo(String c) { this.codePromo = c; }
}
