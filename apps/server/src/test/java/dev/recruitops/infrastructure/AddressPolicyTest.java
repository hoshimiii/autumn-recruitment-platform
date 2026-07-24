package dev.recruitops.infrastructure;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.recruitops.application.CollectionException;
import java.net.URI;
import org.junit.jupiter.api.Test;

class AddressPolicyTest {
  @Test
  void productionBlocksLoopbackAndPrivateAddresses() {
    AddressPolicy policy = new AddressPolicy(false);

    assertThrows(CollectionException.class, () -> policy.requirePublic(URI.create("http://127.0.0.1/jobs")));
    assertThrows(CollectionException.class, () -> policy.requirePublic(URI.create("http://192.168.1.10/jobs")));
  }

  @Test
  void explicitTestOverrideAllowsPrivateAddresses() {
    assertDoesNotThrow(() -> new AddressPolicy(true).requirePublic(URI.create("http://127.0.0.1/jobs")));
  }
}
