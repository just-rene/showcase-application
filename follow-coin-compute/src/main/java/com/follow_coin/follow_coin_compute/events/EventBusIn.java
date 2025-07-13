package com.follow_coin.follow_coin_compute.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.follow_coin.follow_coin_compute.dtos.CoinPriceDifferenceEvent;
import com.follow_coin.follow_coin_compute.dtos.CoinPriceDifferenceEventKey;
import com.follow_coin.follow_coin_compute.dtos.CoinPriceEvent;
import com.follow_coin.follow_coin_compute.entities.CoinPrice;
import com.follow_coin.follow_coin_compute.entities.CoinPriceKey;
import com.follow_coin.follow_coin_compute.repos.CoinPriceEventRepo;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Function;

import static reactor.core.publisher.Mono.defer;

@Component
public class EventBusIn {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private CoinPriceEventRepo coinPriceEventRepo;

    @Autowired
    private SqsTemplate sqsTemplate;

    private static final Logger logger = LoggerFactory.getLogger(EventBusIn.class);

    @SqsListener(value = "testq")
    public void listen(String message) throws JsonProcessingException {

        CoinPriceEvent currentCoinPriceEvent = mapper.readValue(message, CoinPriceEvent.class);

        this.computeCoinPriceDifference(currentCoinPriceEvent).doOnSuccess( data -> {

            //send to filter
            //sqsTemplate.sendAsync("filter");
        }).subscribe();
    }

    public Mono<CoinPriceDifferenceEvent> computeCoinPriceDifference(CoinPriceEvent currentCoinPriceEvent){

        //collects 2 CoinPriceEvents and computes price difference
        return coinPriceEventRepo.save(currentCoinPriceEvent).map(ccpe -> {

            //extract and get event one minute before
            LocalDateTime localDateTimeCurrentEvent = extractDateTime(ccpe);

            LocalDateTime localDateTimeLastEvent = localDateTimeCurrentEvent.minusMinutes(1);
            String localDateTimeLastEventString = localDateTimeLastEvent.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
            String symbol = ccpe.getCoinPrice().getCoinPriceKey().getSymbol();

            Mono<CoinPriceEvent> lastCoinPriceEvent = coinPriceEventRepo
                    .getCoinPriceEventByDateTimeAndSymbol(localDateTimeLastEventString, symbol)
                    .switchIfEmpty(defer( () -> interpolateWithLatestValue(localDateTimeCurrentEvent, ccpe))); //if empty -> interpolate  with older value (if it exists)

            return Mono.zip(lastCoinPriceEvent, Mono.just(ccpe));

            //creates the CoinPriceDifferenceEvent
        }).map(coinPriceEvents -> {
            return coinPriceEvents.map(x -> {
                double priceDifference = x.getT1().getCoinPrice().getPrice() - x.getT2().getCoinPrice().getPrice();

                String symbol = x.getT1().getCoinPrice().getCoinPriceKey().getSymbol();

                String startDate = x.getT1().getCoinPrice().getCoinPriceKey().getDatetime();
                String endDate = x.getT2().getCoinPrice().getCoinPriceKey().getDatetime();

                CoinPriceDifferenceEventKey coinPriceDifferenceEventKey = new CoinPriceDifferenceEventKey(startDate, endDate);

                return new CoinPriceDifferenceEvent(coinPriceDifferenceEventKey, priceDifference, symbol, true);
            });
        }).flatMap(Function.identity());
    }


    /**
     * very basic interpolation... expecting x_0 = 0, x_1 = 1,  x = 0.5
     * doesn't account for actual time delta between the data points -> because points shouldn't be very far apart
     * <a href="https://de.wikipedia.org/wiki/Interpolation_(Mathematik)#Lineare_Interpolation">linear interpolation</a>
     *
     * @return
     */
    private Mono<CoinPriceEvent> interpolateWithLatestValue(LocalDateTime localDateTimeCurrentEvent, CoinPriceEvent cpep) {

        Mono<CoinPriceEvent> latestCoinPriceEventBefore = coinPriceEventRepo
                .getCoinPriceEventBefore(localDateTimeCurrentEvent.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")), cpep.getCoinPrice().getCoinPriceKey().getSymbol());


        return latestCoinPriceEventBefore.map(lcpe -> {
            double f0 = lcpe.getCoinPrice().getPrice();
            double f1 = cpep.getCoinPrice().getPrice();

            double x_0 = 0;
            double x_1 = 1;
            double x = 0.5;

            double interpolatedCoinPrice = f0 + ((f1 - f0) / (x_1 - x_0)) * (x - x_0);


            String symbol = lcpe.getCoinPrice().getCoinPriceKey().getSymbol();
            String dateTime = localDateTimeCurrentEvent.minusMinutes(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

            CoinPriceKey coinPriceKey = new CoinPriceKey(symbol,dateTime );
            CoinPrice coinPrice = new CoinPrice(coinPriceKey, interpolatedCoinPrice, UUID.randomUUID());

            return new CoinPriceEvent(coinPrice);

        });
    }

    private LocalDateTime extractDateTime(CoinPriceEvent c) {
        String datetimeString = c.getCoinPrice().getCoinPriceKey().getDatetime();
        return LocalDateTime.parse(datetimeString, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    }

}

