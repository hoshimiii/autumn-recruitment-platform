package dev.recruitops.infrastructure;

import dev.recruitops.application.CollectionException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AddressPolicy {
  private final boolean allowPrivateAddresses;

  public AddressPolicy(@Value("${recruitops.collector.allow-private-addresses:false}") boolean allowPrivateAddresses) {
    this.allowPrivateAddresses = allowPrivateAddresses;
  }

  public Set<String> requirePublic(URI uri) {
    try {
      InetAddress[] resolved = InetAddress.getAllByName(uri.getHost());
      for (InetAddress address : resolved) {
        if (!allowPrivateAddresses && blocked(address)) throw CollectionException.blocked("request_address_not_allowed");
      }
      return addresses(resolved);
    } catch (UnknownHostException | SecurityException error) {
      throw CollectionException.blocked("request_address_unresolved");
    }
  }

  public void requireStable(URI uri, Set<String> approved) {
    Set<String> current = requirePublic(uri);
    requireSame(approved, current);
  }

  static void requireSame(Set<String> approved, Set<String> current) {
    if (!approved.equals(current)) {
      throw CollectionException.blocked("request_address_changed");
    }
  }

  private static Set<String> addresses(InetAddress[] resolved) {
    return Arrays.stream(resolved).map(InetAddress::getHostAddress)
        .collect(Collectors.toUnmodifiableSet());
  }

  private static boolean blocked(InetAddress address) {
    byte[] bytes = address.getAddress();
    boolean uniqueLocalV6 = bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
    return address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
        || address.isSiteLocalAddress() || address.isMulticastAddress() || uniqueLocalV6;
  }
}
