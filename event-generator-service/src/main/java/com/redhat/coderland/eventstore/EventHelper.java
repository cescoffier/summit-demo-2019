package com.redhat.coderland.eventstore;

import com.redhat.coderland.reactica.model.*;
import io.vertx.core.json.JsonObject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.UUID;

@ApplicationScoped
public class EventHelper {

  @Inject
  CuteNameService cuteNameService;

  @Inject
  RandomRideService randomRideService;

  public static JsonObject create(String event, User user, Ride ride) {
    JsonObject json = new JsonObject()
      .put("event", event);
    if (user != null) {
      json.put("user", JsonObject.mapFrom(user));
    }
    if (ride != null) {
      json.put("ride", JsonObject.mapFrom(ride));
    }
    return json;
  }

  public Event createRandomUserInLineEvent() {
    String userName = cuteNameService.generate();
    String id = UUID.randomUUID().toString();
    User user = new User(id,userName);
    Ride ride = randomRideService.getRandomRide();
    return new UserInLineEvent(user,ride);
  }

  public Event createUserOnRollerCoasterEvent(User user) {
    return new UserOnRideEvent(user);
  }

  public static JsonObject createRideEvent(String event, User user) {
    return create(event, user, null);
  }

}
