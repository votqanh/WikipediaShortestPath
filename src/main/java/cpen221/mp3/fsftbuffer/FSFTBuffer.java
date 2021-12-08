package cpen221.mp3.fsftbuffer;

import cpen221.mp3.server.WikiMediatorState;

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
        - Each object corresponds to 1 map entry.
        - lastAccessed is initially -1 if the object has not been accessed.
     */


    /* the default buffer size is 32 objects */
    public static final int DSIZE = 32;

    /* the default timeout value is 3600s */
    public static final int DTIMEOUT = 3600;

    private int capacity;
    private int timeout;
    private int currentCapacity;

    private Map<Long, LinkedHashMap<Long, T>> buffer = new LinkedHashMap<>();
    private List<String> bufferIds = new ArrayList<>();

    /**
     * Create a buffer with a fixed capacity and a timeout value.
     * Objects in the buffer that have not been refreshed within the
     * timeout period are removed from the cache.
     *
     * @param capacity the number of objects the buffer can hold,
     *                 is greater than 0
     * @param timeout  the duration, in seconds, an object should
     *                 be in the buffer before it times out,
     *                 is greater than 0
     */
    public FSFTBuffer(int capacity, int timeout) {
        this.capacity = capacity;
        this.timeout = timeout;
        this.currentCapacity = 0;
    }

    /**
     * Create a buffer with default capacity (32) and timeout (3600 s) values.
     */
    public FSFTBuffer() {
        this(DSIZE, DTIMEOUT);
    }

    /**
     * Add an object to the buffer.
     * If the buffer is full then remove the least recently accessed
     * object to make room for the new object.
     * If the buffer is full and no objects in the buffer have been accessed,
     * the new object is not added.
     *
     * @param t object to be added to the buffer
     * @return true if successful and false if it is
     *          already in the buffer or if the buffer is full
     *          (in the case where no objects have been accessed)
     */
    public synchronized boolean put(T t) {
        if (bufferIds.contains(t.id()))
            return false;

        // signal to get() that t exists and get() should wait until it's been added
        bufferIds.add(t.id());

        long currentTime = System.currentTimeMillis();

        removeStale(currentTime);

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

        // no need to check if another object is added at the same millisecond (which would overwrite existing entry)
        // because this method is synchronized and its runtime is definitely over 1 ms.

        //add object to new time
        newTime(currentTime, t, -1);
        currentCapacity++;
        return true;
    }

    private synchronized void newTime(long currentTime, T t, long lastAccessed) {
        // if t isn't in buffer
        if (!buffer.keySet().stream().map(x -> buffer.get(x).entrySet()).flatMap(Collection::stream)
                .map(Map.Entry::getValue).collect(Collectors.toList()).contains(t)) {
            LinkedHashMap<Long, T> m = new LinkedHashMap<>();
            m.put(lastAccessed, t);
            buffer.put(currentTime, m);
        }
    }

    private void removeStale(long currentTime) {
        Set<Long> times = new HashSet<>(buffer.keySet());

        for (long time : times) {
            if (currentTime >= time + timeout * 1000L) {
                LinkedHashMap<Long, T> time_object_map = buffer.get(time);
                buffer.remove(time);
                bufferIds.removeAll(time_object_map.keySet().stream().map(x -> time_object_map.get(x).id())
                        .collect(Collectors.toList()));
                currentCapacity -= time_object_map.size();
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

        removeStale(currentTime);

        // if object is in the cache, wait until it's been properly added
        if (bufferIds.contains(id)) {
            while (!buffer.keySet().stream().map(x -> buffer.get(x).entrySet()).flatMap(Collection::stream)
                    .map(e -> e.getValue().id()).collect(Collectors.toList()).contains(id));

            for (long time : buffer.keySet()) {
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

        removeStale(currentTime);

        if (!bufferIds.contains(id)) {
            return false;
        }

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

    /**
     * Below is a collection of observer methods that allow the creation of FSFTState object,
     * and a mutator method that loads the state of a past Buffer for use in servers.
     */
    public int getCapacity() { return capacity; }
    public int getTimeout() { return timeout; }
    public int getCurrentCapacity() { return currentCapacity; }
    public Map<Long, LinkedHashMap<Long, T>> getBuffer() { return new HashMap<>(buffer); }
    public List<String> getBufferIds() { return new ArrayList<>(bufferIds); }

    public void loadState(WikiMediatorState state) {
        this.capacity = state.capacity;
        this.timeout = state.timeout;
        this.currentCapacity = state.currentCapacity;
        this.buffer = state.buffer;
        this.bufferIds = state.bufferIds;
    }


}
