package com.redhat.coderland.eventstore;

import com.redhat.coderland.bridge.AmqpConfiguration;
import com.redhat.coderland.bridge.AmqpToEventBus;
import com.redhat.coderland.bridge.AmqpVerticle;
import com.redhat.coderland.reactica.model.Event;
import com.redhat.coderland.reactica.model.UserInLineEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.reactivex.Completable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class UserSimulatorTest {

    private static final LinkedBlockingDeque<Event> USER_EVENTS = new LinkedBlockingDeque<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSimulatorTest.class);

    @Inject
    Vertx vertx;


    private static Future<Void> bridgeStarted;

    @Test
    public void testUserSimulation() throws Exception {

        LOGGER.info("TEST > Starting the UserSimulation Test");
        bridgeStarted = Future.future();
        deployAMQPVerticle().subscribe(CompletableHelper.toObserver(bridgeStarted));
        LOGGER.info("TEST > Deploying AMQPVerticle");

        assertFalse(bridgeStarted.failed());
        LOGGER.info("TEST > AMQPVerticle successfully deploy");

        //Clear the messages to make sure that we do not have any lingering messages.
        USER_EVENTS.clear();

        LOGGER.info("TEST > Registering the EventBus consumer");
        vertx.eventBus().<JsonObject>consumer("from-user-in-line",message -> {
            JsonObject userEventJson = message.body();
            assertNotNull(userEventJson);

            assertEquals(userEventJson.getString("eventType"),Event.EventType.USER_IN_LINE.toString());
            userEventJson.remove("eventType");
            userEventJson.remove("queueName");

            Event userEvent = userEventJson.mapTo(UserInLineEvent.class);
            assertNotNull(userEvent);
            USER_EVENTS.push(userEvent);
        });



        Event event = USER_EVENTS.poll(3, TimeUnit.SECONDS);
        assertNotNull(event);

    }


    private Completable deployAMQPVerticle() {
        AmqpToEventBus user_queue = new AmqpToEventBus();
        user_queue.setAddress("from-user-in-line");
        user_queue.setQueue(Event.USER_IN_LINE);

        AmqpConfiguration configuration = new AmqpConfiguration()
                .setContainer("amqp-examples")
                .setHost("localhost")
                .setPort(5672)
                .setUser("user")
                .setPassword("user123")
                .addAmqpToEventBus(user_queue);

        return vertx.rxDeployVerticle(AmqpVerticle.class.getName(), new DeploymentOptions().setConfig(JsonObject.mapFrom(configuration))).ignoreElement();
    }
}
