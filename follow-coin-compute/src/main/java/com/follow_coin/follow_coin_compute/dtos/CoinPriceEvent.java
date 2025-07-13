package com.follow_coin.follow_coin_compute.dtos;

import com.follow_coin.follow_coin_compute.entities.CoinPrice;
import lombok.*;
import org.springframework.data.annotation.Id;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CoinPriceEvent extends EventWrapper {

    @Id
    private CoinPrice coinPrice;





}
