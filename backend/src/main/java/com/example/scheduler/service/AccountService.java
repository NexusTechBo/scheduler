package com.example.scheduler.service;

import com.example.scheduler.dto.account.AccountResponse;

public interface AccountService {
    AccountResponse findAccountByCi(String ci);
}
