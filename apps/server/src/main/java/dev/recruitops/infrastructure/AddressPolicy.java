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
  private final HostResolver resolver;

  @org.springframework.beans.factory.annotation.Autowired
  public AddressPolicy(@Value("${recruitops.collector.allow-private-addresses:false}") boolean allowPrivateAddresses) {
    this(allowPrivateAddresses, InetAddress::getAllByName);
  }

  AddressPolicy(boolean allowPrivateAddresses, HostResolver resolver) {
    this.allowPrivateAddresses = allowPrivateAddresses;
    this.resolver = resolver;
  }

  public Set<String> requirePublic(URI uri) {
    return addresses(resolvePublic(uri));
  }

  public InetAddress[] resolvePublic(URI uri) {
    try {
      InetAddress[] resolved = resolver.resolve(uri.getHost());
      if (resolved.length == 0) throw CollectionException.blocked("request_address_unresolved");
      for (InetAddress address : resolved) {
        if (!allowPrivateAddresses && blocked(address)) throw CollectionException.blocked("request_address_not_allowed");
      }
      return resolved.clone();
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
    if (bytes.length == 4) {
      if (prefix(bytes, 32, 192, 0, 0, 9) || prefix(bytes, 32, 192, 0, 0, 10)) return false;
      return prefix(bytes, 8, 0) || prefix(bytes, 8, 10) || prefix(bytes, 10, 100, 64)
          || prefix(bytes, 8, 127) || prefix(bytes, 16, 169, 254) || prefix(bytes, 12, 172, 16)
          || prefix(bytes, 24, 192, 0, 0) || prefix(bytes, 24, 192, 0, 2)
          || prefix(bytes, 24, 192, 88, 99) || prefix(bytes, 16, 192, 168)
          || prefix(bytes, 15, 198, 18) || prefix(bytes, 24, 198, 51, 100)
          || prefix(bytes, 24, 203, 0, 113) || prefix(bytes, 3, 224);
    }
    // Limit collection to native global unicast, excluding protocol, tunnel and documentation blocks.
    return !prefix(bytes, 3, 0x20) || prefix(bytes, 23, 0x20, 0x01, 0)
        || prefix(bytes, 32, 0x20, 0x01, 0x0d, 0xb8) || prefix(bytes, 16, 0x20, 0x02)
        || prefix(bytes, 20, 0x3f, 0xff, 0);
  }

  private static boolean prefix(byte[] address, int bits, int... network) {
    for (int i = 0; bits > 0; i++, bits -= 8) {
      int mask = 0xff << Math.max(0, 8 - bits);
      if (((address[i] & 0xff) & mask) != (network[i] & mask)) return false;
    }
    return true;
  }

  @FunctionalInterface interface HostResolver {
    InetAddress[] resolve(String host) throws UnknownHostException;
  }
}
