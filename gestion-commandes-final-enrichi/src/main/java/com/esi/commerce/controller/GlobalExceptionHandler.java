package com.esi.commerce.controller;

import com.esi.commerce.exception.CommerceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CommerceException.class)
    public ResponseEntity<Map<String, String>> gererExceptionMetier(CommerceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erreur", ex.getMessage()));
    }
}
