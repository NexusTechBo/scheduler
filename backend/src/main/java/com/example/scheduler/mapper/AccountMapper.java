package com.example.scheduler.mapper;

import com.example.scheduler.dto.account.AccountResponse;
import com.example.scheduler.entity.Account;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    AccountResponse toResponse(Account account);
}
