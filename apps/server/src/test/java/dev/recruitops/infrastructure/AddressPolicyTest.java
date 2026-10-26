package dev.recruitops.infrastructure;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.recruitops.application.CollectionException;
import java.net.URI;
import java.net.InetAddress;
import java.util.Set;
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

  @Test void mixedPublicAndPrivateDnsAnswerFailsBeforeTransport() throws Exception {
    var policy = new AddressPolicy(false, host -> new InetAddress[]{
        InetAddress.getByName("93.184.216.34"), InetAddress.getByName("127.0.0.1")});
    assertThrows(CollectionException.class, () -> policy.resolvePublic(URI.create("http://fixture.invalid/jobs")));
  }

  @Test void alternateNumericAndIpv6PrivateFormsAreBlocked() {
    var policy = new AddressPolicy(false);
    for (String host : new String[]{"127.1", "2130706433", "[::1]", "[::ffff:127.0.0.1]", "[fc00::1]", "[fe80::1]"}) {
      assertThrows(CollectionException.class, () -> policy.resolvePublic(URI.create("http://" + host + "/jobs")), host);
    }
  }

  @Test void nonPublicAndTransitionAddressesAreBlockedInNumericAndDnsForms() throws Exception {
    for (String value : new String[]{"100.64.0.1", "100.127.255.254", "0.1.2.3", "192.0.0.8",
        "192.0.2.1", "192.88.99.2", "198.18.0.1", "198.51.100.1", "203.0.113.1", "240.0.0.1",
        "::ffff:100.64.0.1", "::100.64.0.1", "64:ff9b::a00:1", "2001::1", "2001:db8::1",
        "2002:7f00:1::1", "3fff::1"}) {
      String host = value.contains(":") ? "[" + value + "]" : value;
      assertThrows(CollectionException.class,
          () -> new AddressPolicy(false).resolvePublic(URI.create("http://" + host + "/jobs")), value);
      var answer = InetAddress.getByName(value);
      var policy = new AddressPolicy(false, name -> new InetAddress[]{answer});
      assertThrows(CollectionException.class,
          () -> policy.resolvePublic(URI.create("http://fixture.invalid/jobs")), "DNS " + value);
      assertDoesNotThrow(() -> new AddressPolicy(true, name -> new InetAddress[]{answer})
          .resolvePublic(URI.create("http://fixture.invalid/jobs")), "explicit override " + value);
    }
  }

  @Test void ordinaryGlobalAddressesAndIpv4ProtocolExceptionsRemainAllowed() throws Exception {
    for (String value : new String[]{"93.184.216.34", "8.8.8.8", "100.63.255.255", "100.128.0.1",
        "192.0.0.9", "192.0.0.10", "2606:4700:4700::1111", "2001:4860:4860::8888", "::ffff:8.8.8.8"}) {
      var answer = InetAddress.getByName(value);
      var policy = new AddressPolicy(false, name -> new InetAddress[]{answer});
      assertDoesNotThrow(() -> policy.resolvePublic(URI.create("https://fixture.invalid/jobs")), value);
    }
  }

  @Test
  void addressChangeFailsClosed() {
    assertThrows(CollectionException.class,
        () -> AddressPolicy.requireSame(Set.of("203.0.113.1"), Set.of("203.0.113.2")));
    assertDoesNotThrow(
        () -> AddressPolicy.requireSame(Set.of("203.0.113.1"), Set.of("203.0.113.1")));
  }
}
