package cpen221.mp3.wikimediator;

import cpen221.mp3.fsftbuffer.FSFTBuffer;

import java.util.List;

public class WikiMediator {

    /* TODO: Implement this datatype


        You must implement the methods with the exact signatures
        as provided in the statement for this mini-project.

        You must add method signatures even for the methods that you
        do not plan to implement. You should provide skeleton implementation
        for those methods, and the skeleton implementation could return
        values like null.

     */
    private final int capacity;
    private final int stalenessInterval;

    /**
     *
     * @param capacity
     * @param stalenessInterval
     */
    public WikiMediator(int capacity, int stalenessInterval) {
        this.capacity = capacity;
        this.stalenessInterval = stalenessInterval;

    }

    /**
     *  Given a query, return up to limit page titles that match the query string (per Wikipedia's search service).
     * @param query
     * @param limit
     * @return
     */
    List<String> search(String query, int limit) {
        return null;
    }

    /**
     * Given a pageTitle, return the text associated with the Wikipedia page that matches pageTitle.
     * @param pageTitle
     * @return
     */
    String getPage(String pageTitle) {
        return null;
    }

    /**
     * Return the most common Strings used in search and getPage requests, with items being sorted in non-increasing count order. When many requests have been made, return only limit items.
     * @param limit
     * @return
     */
    List<String> zeitgeist(int limit) {
        return null;
    }

    /**
     * Similar to zeitgeist(), but returns the most frequent requests made in the last timeLimitInSeconds seconds. This method should report at most maxItems of the most frequent requests.
     * @param timeLimitInSeconds
     * @param maxItems
     * @return
     */
    List<String> trending(int timeLimitInSeconds, int maxItems) {
        return null;
    }

    /**
     * What is the maximum number of requests seen in any time window of a given length? The request count is to include all requests made using the public API of WikiMediator, and therefore counts all five methods listed as basic page requests.
     * (There is one more request that appears later, shortestPath, and that should also be included if you do implement that method.)
     * @param timeWindowInSeconds
     * @return
     */
    int windowedPeakLoad(int timeWindowInSeconds) {
        return -1;
    }

    /**
     * This is an overloaded version of the previous method where the time window defaults to 30 seconds. (Calls to this method also affect peak load.)
     * @return
     */
    int windowedPeakLoad() {
        return -1;
    }

}
