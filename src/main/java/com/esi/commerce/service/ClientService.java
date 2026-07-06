package com.esi.commerce.service;

import com.esi.commerce.exception.ClientInvalideException;
import com.esi.commerce.model.Client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClientService {

    private final List<Client> clients = new ArrayList<>();

    public void ajouter(Client client) { clients.add(client); }

    public Optional<Client> trouverParId(String clientId) {
        return clients.stream().filter(c -> c.getId().equals(clientId)).findFirst();
    }

    public Client trouverOuLever(String clientId) {
        return trouverParId(clientId).orElseThrow(() -> new ClientInvalideException(clientId));
    }

    public List<Client> getClients() { return List.copyOf(clients); }
}
