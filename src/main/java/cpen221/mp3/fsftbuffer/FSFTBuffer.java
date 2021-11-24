package cpen221.mp3.fsftbuffer;
import java.lang.System;
import java.util.*;
import java.util.stream.Collectors;

public class FSFTBuffer<T extends Bufferable> {

    /* the default buffer size is 32 objects */
    public static final int DSIZE = 32;

    /* the default timeout value is 3600s */
    public static final int DTIMEOUT = 3600;

    private final int capacity;
    private final int timeout;
    private int currentCapacity;

    private final TreeMap<Long, ArrayList<T>> buffer = new TreeMap<>();
    private final ArrayList<String> bufferIds = new ArrayList<>();

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
    public synchronized boolean put(T t) {
        // signal to get() that t exists and get() should wait until it's been added
        bufferIds.add(t.id());

        long currentTime = System.currentTimeMillis() / 1000;

        Set<Long> times = new HashSet<>(buffer.keySet());

        for (long time : times) {
            if (time >= time + timeout) {
                buffer.remove(time);
                bufferIds.removeAll(buffer.get(time).stream().map(Bufferable::id).collect(Collectors.toList()));
                currentCapacity -= buffer.get(time).size();
            } else {
                break;
            }
        }

        // remove least recently accessed
        if (currentCapacity >= capacity) {
            bufferIds.remove(buffer.get(buffer.firstKey()).get(0).id());
            buffer.get(buffer.firstKey()).remove(0);
            currentCapacity--;
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
    public synchronized T get(String id) throws NoSuchElementException {
        /* Do not return null. Throw a suitable checked exception when an object
            is not in the cache. You can add the checked exception to the method
            signature. */

        // if object is in the cache, wait until it's been properly added
        if (bufferIds.contains(id)) {
            while (true) {
                for (long time : buffer.keySet()) {
                    for (T t : buffer.get(time)) {
                        if (Objects.equals(t.id(), id)) {
                            update(t);
                            return t;
                        }
                    }
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
    public synchronized boolean touch(String id) {
        Set<Long> times = new HashSet<>(buffer.keySet());

        for (long time :times) {
            for (T t : buffer.get(time)) {
                if (Objects.equals(t.id(), id)) {
                    buffer.get(time).remove(t);
                    currentCapacity--;
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
    public synchronized boolean update(T t) {
        Set<Long> times = new HashSet<>(buffer.keySet());

        for (long time :times) {
            for (T t1 : buffer.get(time)) {
                if (Objects.equals(t.id(), t1.id())) {
                    buffer.get(time).remove(t);
                    currentCapacity--;
                    newTime(System.currentTimeMillis() / 1000, t);
                    return true;
                }
            }
        }
        return false;
    }
}
