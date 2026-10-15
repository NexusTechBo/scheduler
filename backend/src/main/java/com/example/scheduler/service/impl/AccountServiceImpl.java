package com.example.scheduler.service.impl;

import com.example.scheduler.dto.account.AccountResponse;
import com.example.scheduler.mapper.AccountMapper;
import com.example.scheduler.repository.AccountRepository;
import com.example.scheduler.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Override
    public AccountResponse findAccountByCi(String ci) {
        var  account = accountRepository.findByCi(ci).orElse(null);
        return accountMapper.toResponse(account);
    }
}
