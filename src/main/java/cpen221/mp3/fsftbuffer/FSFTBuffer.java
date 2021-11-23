package cpen221.mp3.fsftbuffer;
import java.lang.System;
import java.util.*;

public class FSFTBuffer<T extends Bufferable> {

    /* the default buffer size is 32 objects */
    public static final int DSIZE = 32;

    /* the default timeout value is 3600s */
    public static final int DTIMEOUT = 3600;

    private final int capacity;
    private final int timeout;
    private int currentCapacity;

    private final TreeMap<Long, ArrayList<T>> buffer = new TreeMap<>();

    /* TODO: Implement this datatype */

    /**
     * Create a buffer with a fixed capacity and a timeout value.
     * Objects in the buffer that have not been refreshed within the
     * timeout period are removed from the cache.
     *
     * @param capacity the number of objects the buffer can hold
     * @param timeout  the duration, in seconds, an object should
     *                 be in the buffer before it times out
     */
    public FSFTBuffer(int capacity, int timeout) {
        this.capacity = capacity;
        this.timeout = timeout;
        this.currentCapacity = 0;
    }

    /**
     * Create a buffer with default capacity and timeout values.
     */
    public FSFTBuffer() {
        this(DSIZE, DTIMEOUT);
    }

    /**
     * Add a value to the buffer.
     * If the buffer is full then remove the least recently accessed
     * object to make room for the new object.
     */
    public boolean put(T t) {
        long currentTime = System.currentTimeMillis() / 1000;

        Set<Long> times = new HashSet<>(buffer.keySet());

        for (long time : times) {
            if (time >= time + timeout) {
                buffer.remove(time);
                currentCapacity--;
            } else {
                break;
            }
        }

        // remove least recently accessed
        if (currentCapacity >= capacity) {
            buffer.get(buffer.firstKey()).remove(0);
        }

        // if another object is added in the same second
        for (long time : buffer.keySet()) {
            if (time == currentTime) {
                buffer.get(time).add(t);
                currentCapacity++;
                return true;
            }
        }

        newTime(currentTime, t);
        return true;
    }

    private void newTime(long currentTime, T t) {
        ArrayList<T> l = new ArrayList<>();
        l.add(t);
        buffer.put(currentTime, l);
        currentCapacity++;
    }

    /**
     * @param id the identifier of the object to be retrieved
     * @return the object that matches the identifier from the
     * buffer
     */
    public T get(String id) throws NoSuchElementException {
        /* Do not return null. Throw a suitable checked exception when an object
            is not in the cache. You can add the checked exception to the method
            signature. */

        for (long time : buffer.keySet()) {
            for (T t : buffer.get(time)) {
                if (Objects.equals(t.id(), id)) {
                    return t;
                }
            }
        }

        throw new NoSuchElementException();
    }

    /**
     * Update the last refresh time for the object with the provided id.
     * This method is used to mark an object as "not stale" so that its
     * timeout is delayed.
     *
     * @param id the identifier of the object to "touch"
     * @return true if successful and false otherwise
     */
    public boolean touch(String id) {
        Set<Long> times = new HashSet<>(buffer.keySet());

        for (long time :times) {
            for (T t : buffer.get(time)) {
                if (Objects.equals(t.id(), id)) {
                    buffer.get(time).remove(t);
                    newTime(System.currentTimeMillis() / 1000, t);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Update an object in the buffer.
     * This method updates an object and acts like a "touch" to
     * renew the object in the cache.
     *
     * @param t the object to update
     * @return true if successful and false otherwise
     */
    public boolean update(T t) {
        Set<Long> times = new HashSet<>(buffer.keySet());

        for (long time :times) {
            for (T t1 : buffer.get(time)) {
                if (Objects.equals(t.id(), t1.id())) {
                    buffer.get(time).remove(t);
                    newTime(System.currentTimeMillis() / 1000, t);
                    return true;
                }
            }
        }
        return false;
    }
}
