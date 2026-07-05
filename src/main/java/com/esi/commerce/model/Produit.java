package com.esi.commerce.model;

public class Produit {
    private String id;
    private String nom;
    private double prix;
    private int stock;

    public Produit(String id, String nom, double prix, int stock) {
        this.id = id;
        this.nom = nom;
        this.prix = prix;
        this.stock = stock;
    }

    public String getId() { return id; }
    public String getNom() { return nom; }
    public double getPrix() { return prix; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
}
