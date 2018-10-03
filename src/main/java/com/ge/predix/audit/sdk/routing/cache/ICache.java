package com.ge.predix.audit.sdk.routing.cache;

import java.util.Map;
import java.util.Optional;

public interface ICache<K, V> {
    Optional<V> get(K key);
    V put(K key, V value);
    void delete(K key);
    Map<K, V> getAll();
    void evict();
    void shutdown();
    void gracefulShutdown() throws Exception;
}
