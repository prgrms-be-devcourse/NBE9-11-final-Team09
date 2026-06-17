package com.back.team9.moyeota.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Healthcontroller {

    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}