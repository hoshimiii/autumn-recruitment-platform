package dev.recruitops.infrastructure;

import dev.recruitops.application.CollectionException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AddressPolicy {
  private final boolean allowPrivateAddresses;

  public AddressPolicy(@Value("${recruitops.collector.allow-private-addresses:false}") boolean allowPrivateAddresses) {
    this.allowPrivateAddresses = allowPrivateAddresses;
  }

  public void requirePublic(URI uri) {
    try {
      for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
        if (!allowPrivateAddresses && blocked(address)) throw CollectionException.blocked("request_address_not_allowed");
      }
    } catch (UnknownHostException | SecurityException error) {
      throw CollectionException.blocked("request_address_unresolved");
    }
  }

  private static boolean blocked(InetAddress address) {
    byte[] bytes = address.getAddress();
    boolean uniqueLocalV6 = bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
    return address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
        || address.isSiteLocalAddress() || address.isMulticastAddress() || uniqueLocalV6;
  }
}
