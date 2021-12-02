package cpen221.mp3.fsftbuffer;
import java.lang.System;
import java.util.*;
import java.util.stream.Collectors;

public class FSFTBuffer<T extends Bufferable> {

    /*
        AF:
        - Last access time of an object is greater than the time it was added.
        - touch() and update() do not change last access time.
        - An object cannot be added to the buffer if it is already in the buffer.

        RI:
        - {@code buffer} entries are sorted by the order they were added.
        - {@code bufferIds} contains ids of all objects currently in buffer.
        - Objects created at the same millisecond are put in the same map entry for ease of removal.
        - lastAccessed is initially -1 if the object has not been accessed.
     */


    /* the default buffer size is 32 objects */
    public static final int DSIZE = 32;

    /* the default timeout value is 3600s */
    public static final int DTIMEOUT = 3600;

    private final int capacity;
    private final int timeout;
    private int currentCapacity;

    private final Map<Long, LinkedHashMap<Long, T>> buffer = new LinkedHashMap<>();
    private final List<String> bufferIds = new ArrayList<>();

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
     * Add an object to the buffer.
     * If the buffer is full then remove the least recently accessed
     * object to make room for the new object.
     *
     *
     * @param t object to be added to the buffer
     * @return true if successful and false if it is
     *          already in the buffer
     */
    public synchronized boolean put(T t) {
        // signal to get() that t exists and get() should wait until it's been added
        if (bufferIds.contains(t.id()))
            return false;

        bufferIds.add(t.id());

        long currentTime = System.currentTimeMillis();

        removeStale();

        // remove least recently accessed
        if (currentCapacity >= capacity) {
            List<Long> accessTimes = buffer.keySet().stream().map(x -> buffer.get(x).keySet())
                    .flatMap(Collection::stream).filter(x -> x != -1).collect(Collectors.toList());

            // no object has been accessed = none to evict = no more space
            if (accessTimes.isEmpty())
                return false;

            long leastRecent = Collections.min(accessTimes);

            outer:
            for (long time : buffer.keySet()) {
                LinkedHashMap<Long, T> time_object_map = buffer.get(time);
                for (long lastAccessed : time_object_map.keySet()) {
                    if (lastAccessed == leastRecent) {
                        time_object_map.remove(lastAccessed);
                        currentCapacity--;
                    }

                    if (time_object_map.size() == 0) {
                        buffer.remove(time);
                    }

                    break outer;
                }
            }
        }

        // if another object is added in the same millisecond
        for (long time : buffer.keySet()) {
            if (time == currentTime) {
                buffer.get(time).put((long) -1, t);
                currentCapacity++;
                return true;
            }
        }

        //else add object to new time
        newTime(currentTime, t, -1);
        currentCapacity++;
        return true;
    }

    private synchronized void newTime(long currentTime, T t, long lastAccessed) {
        if (!buffer.keySet().stream().map(x -> buffer.get(x).entrySet()).flatMap(Collection::stream)
                .map(Map.Entry::getValue).collect(Collectors.toList()).contains(t)) {
            LinkedHashMap<Long, T> m = new LinkedHashMap<>();
            m.put(lastAccessed, t);
            buffer.put(currentTime, m);
        }
    }

    private void removeStale() {
        Set<Long> times = new HashSet<>(buffer.keySet());
        for (long time : times) {
            if (time >= time + timeout * 1000L) {
                LinkedHashMap<Long, T> time_object_map = buffer.get(time);
                buffer.remove(time);
                bufferIds.removeAll(time_object_map.keySet().stream().map(x -> time_object_map.get(x).id())
                        .collect(Collectors.toList()));
                currentCapacity -= time_object_map.size();
            } else {
                break;
            }
        }
    }

    /**
     * @param id the identifier of the object to be retrieved
     * @return the object that matches the identifier from the
     * buffer
     * @throws NoSuchElementException if object is not in the buffer
     */
    public synchronized T get(String id) throws NoSuchElementException {
        long currentTime = System.currentTimeMillis();

        removeStale();

        // if object is in the cache, wait until it's been properly added
        if (bufferIds.contains(id)) {
            while (true) {
                for (long time : buffer.keySet()) {             // TODO: change to start where we last left off
                    LinkedHashMap<Long, T> time_object_map = buffer.get(time);
                    for (long lastAccessed : time_object_map.keySet()) {
                        T t = time_object_map.get(lastAccessed);
                        if (Objects.equals(t.id(), id)) {
                            time_object_map.remove(lastAccessed);

                            if (time_object_map.size() == 0) {
                                buffer.remove(time);
                            }

                            newTime(currentTime, t, currentTime);
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
    public boolean touch(String id) {
        long currentTime = System.currentTimeMillis();

        removeStale();

        for (long time : buffer.keySet()) {
            LinkedHashMap<Long, T> time_object_map = buffer.get(time);
            for (long lastAccessed : time_object_map.keySet()) {
                T t = time_object_map.get(lastAccessed);
                if (Objects.equals(id, t.id())) {
                    time_object_map.remove(lastAccessed);

                    if (time_object_map.size() == 0) {
                        buffer.remove(time);
                    }

                    newTime(currentTime, t, lastAccessed);
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
        return touch(t.id());
    }
}
