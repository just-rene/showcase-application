package com.follow_coin.follow_coin_compute.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


@Getter
@Setter
abstract class EventWrapper {
        private UUID uuid = null; //for message transmission via sqs
}
