package dev.recruitops.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class RequestPacerTest {
  @Test
  void productionFloorCannotBeConfiguredBelowFiveSeconds() {
    RequestPacer pacer = new RequestPacer(0, new MockEnvironment());

    assertEquals(5_000, pacer.floorMillis());
  }

  @Test
  void testProfileMayDisableTheFloor() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("test");

    RequestPacer pacer = new RequestPacer(0, environment);

    assertEquals(0, pacer.floorMillis());
  }
}
