package net.skycore.client.event;

public interface Subscription extends AutoCloseable {
   @Override
   public void close();
}
