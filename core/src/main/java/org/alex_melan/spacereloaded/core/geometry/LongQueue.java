package org.alex_melan.spacereloaded.core.geometry;

/**
 * Примитивная FIFO-очередь long'ов на кольцевом буфере — без боксинга,
 * чтобы фронтир flood fill не создавал мусора (ядро без внешних зависимостей,
 * поэтому fastutil не используется).
 */
public final class LongQueue {
    private long[] buffer;
    private int head;
    private int size;

    public LongQueue(int initialCapacity) {
        int cap = Integer.highestOneBit(Math.max(16, initialCapacity - 1)) << 1;
        this.buffer = new long[cap];
    }

    public void enqueue(long value) {
        if (size == buffer.length) {
            grow();
        }
        buffer[(head + size) & (buffer.length - 1)] = value;
        size++;
    }

    public long dequeue() {
        if (size == 0) {
            throw new IllegalStateException("queue is empty");
        }
        long value = buffer[head];
        head = (head + 1) & (buffer.length - 1);
        size--;
        return value;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    private void grow() {
        long[] next = new long[buffer.length << 1];
        for (int i = 0; i < size; i++) {
            next[i] = buffer[(head + i) & (buffer.length - 1)];
        }
        buffer = next;
        head = 0;
    }
}
