package com.example.scheduler.controller;

import com.example.scheduler.dto.account.AccountResponse;
import com.example.scheduler.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account Controller")
public class AccountController {
    private final AccountService accountService;

    @GetMapping("/{ci}")
    @Operation(summary = "GET /api/account/{ci} — get an account by Id")
    public ResponseEntity<AccountResponse> findAccountByCi(@PathVariable String ci) {
        return ResponseEntity.ok(accountService.findAccountByCi(ci));
    }
}
