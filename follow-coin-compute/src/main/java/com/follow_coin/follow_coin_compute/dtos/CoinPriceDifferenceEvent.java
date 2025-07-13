package com.follow_coin.follow_coin_compute.dtos;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CoinPriceDifferenceEvent extends EventWrapper {

        @Id
        private CoinPriceDifferenceEventKey _id;
        private double differenceAbsolute;
        private String symbol = "unk";
        private boolean interpolated = false;


}
