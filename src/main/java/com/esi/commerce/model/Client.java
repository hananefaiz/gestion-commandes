package com.esi.commerce.model;

public class Client {
    private String id;
    private String nom;
    private String email;
    private boolean estVIP;
    private int pointsFidelite;

    public Client(String id, String nom, String email, boolean estVIP) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.estVIP = estVIP;
        this.pointsFidelite = 0;
    }

    public String getId() { return id; }
    public String getNom() { return nom; }
    public String getEmail() { return email; }
    public boolean isEstVIP() { return estVIP; }
    public int getPointsFidelite() { return pointsFidelite; }
    public void setPointsFidelite(int p) { this.pointsFidelite = p; }
    public void ajouterPoints(int p) { this.pointsFidelite += p; }
    public void consommerPoints(int p) { this.pointsFidelite = Math.max(0, this.pointsFidelite - p); }
}
