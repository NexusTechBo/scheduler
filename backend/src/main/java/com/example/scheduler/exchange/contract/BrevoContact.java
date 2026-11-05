package com.example.scheduler.exchange.contract;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BrevoContact(String email, String name) {}
