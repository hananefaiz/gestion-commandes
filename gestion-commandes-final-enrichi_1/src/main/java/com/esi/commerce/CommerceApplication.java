package com.esi.commerce;

import com.esi.commerce.service.ClientService;
import com.esi.commerce.service.ProduitService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CommerceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommerceApplication.class, args);
    }

    @Bean
    public ProduitService produitService() { return new ProduitService(); }

    @Bean
    public ClientService clientService() { return new ClientService(); }
}
