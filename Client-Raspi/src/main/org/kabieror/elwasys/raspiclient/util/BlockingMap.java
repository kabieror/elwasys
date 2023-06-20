package org.kabieror.elwasys.raspiclient.util;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class BlockingMap<K, V> {
    private Map<K, ArrayBlockingQueue<V>> map = new ConcurrentHashMap<>();

    public void put(K key, V value) {
        getQueue(key).offer(value);
    }

    public void clear(K key) {
        getQueue(key).clear();
    }

    public V get(K key) throws InterruptedException {
        return getQueue(key).take();
    }

    public V get(K key, long timeout, TimeUnit unit) throws InterruptedException {
        return getQueue(key).poll(timeout, unit);
    }

    private BlockingQueue<V> getQueue(K key) {
        return map.computeIfAbsent(key, k -> new ArrayBlockingQueue<>(1));
    }

}
