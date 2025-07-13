package com.follow_coin.follow_coin_compute.dtos;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CoinPriceDifferenceEventKey {
    private String startDateTime;
    private String endDateTime;
}
