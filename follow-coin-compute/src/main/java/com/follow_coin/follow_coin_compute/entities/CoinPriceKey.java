package com.follow_coin.follow_coin_compute.entities;


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
public class CoinPriceKey {

    private String symbol;
    private String datetime;

}
