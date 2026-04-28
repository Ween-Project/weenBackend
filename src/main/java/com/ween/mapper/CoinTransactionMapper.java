package com.ween.mapper;

import com.ween.dto.response.CoinTransactionResponse;
import com.ween.entity.CoinTransaction;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CoinTransactionMapper {
    CoinTransactionResponse toCoinTransactionResponse(CoinTransaction transaction);}