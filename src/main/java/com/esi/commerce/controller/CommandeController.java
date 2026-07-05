package com.esi.commerce.controller;

import com.esi.commerce.model.Commande;
import com.esi.commerce.service.CommandeService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/commandes")
public class CommandeController {

    private final CommandeService commandeService = new CommandeService();

    @PostMapping
    public Commande passerCommande(@RequestParam String clientId, @RequestBody Map<String, Integer> panier,
                                    @RequestParam(required = false) String codePromo) {
        return commandeService.passerCommande(clientId, panier, codePromo);
    }

    @PutMapping("/{id}/annuler")
    public boolean annuler(@PathVariable String id) {
        return commandeService.annulerCommande(id);
    }

    @GetMapping("/moyenne/{clientId}")
    public double moyenne(@PathVariable String clientId) {
        return commandeService.calculerMoyennePanier(clientId);
    }
}
