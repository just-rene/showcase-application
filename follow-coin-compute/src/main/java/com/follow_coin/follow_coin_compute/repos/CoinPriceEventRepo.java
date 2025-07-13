package com.follow_coin.follow_coin_compute.repos;


import com.follow_coin.follow_coin_compute.dtos.CoinPriceEvent;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CoinPriceEventRepo extends ReactiveMongoRepository<CoinPriceEvent, String> {

    @Aggregation(pipeline = {
            "{ '$match': { $and: [ { '_id.coinPriceKey.datetime' : { $lt : ?0 } } ,  { '_id.coinPriceKey.symbol': {$eq: ?1} }   ] } }",
            "{ '$sort' : { '_id.coinPriceKey.datetime' : -1 } }",
            "{ '$limit' : 1 }"
    })
    Mono<CoinPriceEvent> getCoinPriceEventBefore(String localDateTimeString, String symbol);

    @Aggregation(pipeline = {
            "{ '$match': { $and: [ { '_id.coinPriceKey.datetime' : { $eq : ?0 }  } ,  { '_id.coinPriceKey.symbol': {$eq: ?1}}   ] } }",
            "{ '$limit' : 1 }"
    })
    Mono<CoinPriceEvent> getCoinPriceEventByDateTimeAndSymbol(String localDateTimeString, String symbol);

}
