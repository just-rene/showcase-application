package com.follow_coin.follow_coin_compute.events.integragtions_tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.follow_coin.follow_coin_compute.dtos.CoinPriceDifferenceEvent;
import com.follow_coin.follow_coin_compute.dtos.CoinPriceEvent;
import com.follow_coin.follow_coin_compute.entities.CoinPrice;
import com.follow_coin.follow_coin_compute.entities.CoinPriceKey;
import com.follow_coin.follow_coin_compute.events.EventBusIn;
import com.follow_coin.follow_coin_compute.repos.CoinPriceEventRepo;
import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.apache.commons.lang3.function.Suppliers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Example;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.UUID;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * Integration Testing against implementation with repositories
 */
@ActiveProfiles("integration")
@SpringBootTest
@TestPropertySource(locations = "classpath:application-integration.properties")
@EnableAutoConfiguration(exclude = {
        AwsAutoConfiguration.class,
        SqsAutoConfiguration.class
})
class EventBusInIntegrationsTest {

    @MockitoBean
    private SqsTemplate sqsTemplate;

    @Autowired
    public EventBusIn eventBusIn;

    @Autowired
    private CoinPriceEventRepo coinPriceEventRepo;

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0").withExposedPorts(27017);

    @DynamicPropertySource
    static void containersProperties(DynamicPropertyRegistry registry) {
        mongoDBContainer.start();
        registry.add("spring.data.mongodb.host", mongoDBContainer::getHost);
        registry.add("spring.data.mongodb.port", mongoDBContainer::getFirstMappedPort);
    }

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
    void integration_does_save_work() throws JsonProcessingException {

        //set up
        CoinPriceKey coinPriceKey = new CoinPriceKey("COIN", "2025-07-11T11:11:00.000Z");
        CoinPrice coinPrice = new CoinPrice(coinPriceKey, 1001.0, UUID.randomUUID());
        CoinPriceEvent coinPriceEvent = new CoinPriceEvent(coinPrice);

        //when
        eventBusIn.computeCoinPriceDifference(coinPriceEvent).block();

        //then
        var result = coinPriceEventRepo.findOne(Example.of(coinPriceEvent)).block();

        assert result != null;
        assertEquals(result.getCoinPrice().getPrice(), coinPriceEvent.getCoinPrice().getPrice(), "CoinPriceEvents should be equal!");
        assertEquals(result.getCoinPrice().getCoinPriceKey().getDatetime(), coinPriceEvent.getCoinPrice().getCoinPriceKey().getDatetime(), "CoinPriceEvents should be equal!");
        assertEquals(result.getCoinPrice().getCoinPriceKey().getSymbol(), coinPriceEvent.getCoinPrice().getCoinPriceKey().getSymbol(), "CoinPriceEvents should be equal!");
    }

    @Test
    void integration_does_difference_event_get_computed_correctly() throws JsonProcessingException {

        //set up

        //first event
        CoinPriceKey coinPriceKey = new CoinPriceKey("COIN", "2025-07-11T12:00:00.000Z");
        CoinPrice coinPrice = new CoinPrice(coinPriceKey, 10.0, UUID.randomUUID());
        CoinPriceEvent coinPriceEvent = new CoinPriceEvent(coinPrice);

        //second event
        CoinPriceKey coinPriceKey2 = new CoinPriceKey("COIN", "2025-07-11T12:01:00.000Z");
        CoinPrice coinPrice2 = new CoinPrice(coinPriceKey2, 15.0, UUID.randomUUID());
        CoinPriceEvent coinPriceEvent2 = new CoinPriceEvent(coinPrice2);

        //when
        eventBusIn.computeCoinPriceDifference(coinPriceEvent).block();
        CoinPriceDifferenceEvent coinPriceDifferenceEvent = eventBusIn.computeCoinPriceDifference(coinPriceEvent2).block();

        //then
        assert coinPriceDifferenceEvent != null;
        assertEquals(10.0 - 15.0, coinPriceDifferenceEvent.getDifferenceAbsolute(), "");
    }

    @Test
    void integration_does_not_compute_difference_event_between_different_coins() throws JsonProcessingException {

        //set up

        //first event
        CoinPriceKey coinPriceKey = new CoinPriceKey("COIN_A", "2025-07-11T12:00:00.000Z");
        CoinPrice coinPrice = new CoinPrice(coinPriceKey, 10.0, UUID.randomUUID());
        CoinPriceEvent coinPriceEvent = new CoinPriceEvent(coinPrice);

        //second event
        CoinPriceKey coinPriceKey2 = new CoinPriceKey("COIN_B", "2025-07-11T12:01:00.000Z");
        CoinPrice coinPrice2 = new CoinPrice(coinPriceKey2, 15.0, UUID.randomUUID());
        CoinPriceEvent coinPriceEvent2 = new CoinPriceEvent(coinPrice2);

        //when
        eventBusIn.computeCoinPriceDifference(coinPriceEvent).block();
        CoinPriceDifferenceEvent coinPriceDifferenceEvent = eventBusIn.computeCoinPriceDifference(coinPriceEvent2).block();

        //then
        assertNull(coinPriceDifferenceEvent, "should be null, it might be that the system " +
                "is computing CoinPriceDifferenceEvent between different coins, which is wrong!");
    }


    @Disabled
    @Test
    void test_rest_with_rest_assured() {

        //example for testing a REST_API against a test system
        //the system must run on a test server
        given()
                .when()
                .get("http://localhost:8081/test")
                .then()
                .body(contains(true));
    }

}