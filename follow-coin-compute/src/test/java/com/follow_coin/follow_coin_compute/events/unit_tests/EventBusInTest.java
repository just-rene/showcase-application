package com.follow_coin.follow_coin_compute.events.unit_tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.follow_coin.follow_coin_compute.dtos.CoinPriceEvent;
import com.follow_coin.follow_coin_compute.entities.CoinPrice;
import com.follow_coin.follow_coin_compute.entities.CoinPriceKey;
import com.follow_coin.follow_coin_compute.events.EventBusIn;
import com.follow_coin.follow_coin_compute.repos.CoinPriceEventRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

//Unit Tests
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class EventBusInTest {

    @Mock
    public CoinPriceEventRepo coinPriceEventRepo;

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    public EventBusIn eventBusIn;
    
    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {

    }

    @Test
    void test_repo() {
    }

    @Test
    void test_testcontainers_are_available_and_save_correctly() {
    }

    @Test
    void test_well_formed_message() {

        //test if standard behavior works

        //set up
        CoinPriceKey coinPriceKey = new CoinPriceKey("BTC", "2025-07-10T14:30:00.000Z");
        CoinPrice coinPrice = new CoinPrice(coinPriceKey, 100.0, UUID.randomUUID());
        CoinPriceEvent coinPriceEvent = new CoinPriceEvent(coinPrice);

        //Mockito.when(mapper.readValue(wellFormedMessage, CoinPriceEvent.class)).thenReturn(coinPriceEvent);
        Mockito.when(coinPriceEventRepo.save(coinPriceEvent)).thenReturn(Mono.just(coinPriceEvent));

        //generate an event 1 minute before
        String datetimeString = coinPriceEvent.getCoinPrice().getCoinPriceKey().getDatetime();
        LocalDateTime localDateTimeCurrentEvent = LocalDateTime.parse(datetimeString, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        LocalDateTime localDateTimeLastEvent = localDateTimeCurrentEvent.minusMinutes(1);

        CoinPriceKey coinPriceKey2 = new CoinPriceKey("BTC", "2025-07-10T14:29:00.000Z");
        CoinPrice coinPrice2 = new CoinPrice(coinPriceKey2, 102.0, UUID.randomUUID());
        CoinPriceEvent coinPriceEvent2 = new CoinPriceEvent(coinPrice2);

        Mockito.when(coinPriceEventRepo.getCoinPriceEventByDateTimeAndSymbol(localDateTimeLastEvent.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")), "BTC"))
                .thenReturn(Mono.just(coinPriceEvent2));

        //when
        var res = eventBusIn.computeCoinPriceDifference(coinPriceEvent).block();

        assertEquals(2, res.getDifferenceAbsolute(), "difference should be 2");
        assertEquals("BTC", res.getSymbol(), "wrong symbol, should be BTC");

    }

    @Test
    void test_null_message() {

        Mockito.when(coinPriceEventRepo.save(null)).thenReturn(Mono.empty());

        //when
        var res = eventBusIn.computeCoinPriceDifference(null).block();

        //then
        //should just be ignored, no exception should be thrown
        assertNull(res, "should just be ignored, no exception should be thrown");

    }

    @Test
    void test_first_event_arrives() {

        //test what happens when the first event arrives

        //set up
        CoinPriceKey coinPriceKey = new CoinPriceKey("BTC", "2025-07-10T14:30:00.000Z");
        CoinPrice coinPrice = new CoinPrice(coinPriceKey, 100.0, UUID.randomUUID());
        CoinPriceEvent coinPriceEvent = new CoinPriceEvent(coinPrice);


        Mockito.when(coinPriceEventRepo.save(coinPriceEvent)).thenReturn(Mono.just(coinPriceEvent));

        //generate an event 1 minute before
        String datetimeString = coinPriceEvent.getCoinPrice().getCoinPriceKey().getDatetime();
        LocalDateTime localDateTimeCurrentEvent = LocalDateTime.parse(datetimeString, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        LocalDateTime localDateTimeLastEvent = localDateTimeCurrentEvent.minusMinutes(1);


        Mockito.when(coinPriceEventRepo.getCoinPriceEventByDateTimeAndSymbol(localDateTimeLastEvent.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")), "BTC"))
                .thenReturn(Mono.empty());

        Mockito.when(coinPriceEventRepo.getCoinPriceEventBefore(localDateTimeCurrentEvent.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")), "BTC"))
                .thenReturn(Mono.empty());

        //when
        var res = eventBusIn.computeCoinPriceDifference(coinPriceEvent).block();

        //then
        //if only one entry exists it should just be ignored, no exceptions should be thrown
        assertNull(res, "if only one entry exists it should just be ignored, no exceptions should be thrown");

    }
}