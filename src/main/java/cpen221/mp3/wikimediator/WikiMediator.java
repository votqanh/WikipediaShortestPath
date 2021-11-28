package cpen221.mp3.wikimediator;

import cpen221.mp3.fsftbuffer.FSFTBuffer;
import org.fastily.jwiki.core.Wiki;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;


public class WikiMediator {

    /* TODO: Implement this datatype


        You must implement the methods with the exact signatures
        as provided in the statement for this mini-project.

        You must add method signatures even for the methods that you
        do not plan to implement. You should provide skeleton implementation
        for those methods, and the skeleton implementation could return
        values like null.

     */
    private final Wiki wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();
    private final FSFTBuffer<WikiPage> wikiBuffer;
    //private final HashMap<String,ArrayList<Long>> requestTracker = new HashMap<>();
    private final ArrayList<Request> basicRequestsTracker = new ArrayList<>();
    private final ArrayList<Long> allRequestsTracker = new ArrayList<>();

    /**
     *
     * @param capacity
     * @param stalenessInterval
     */
    public WikiMediator(int capacity, int stalenessInterval) {
        wikiBuffer = new FSFTBuffer<WikiPage>(capacity, stalenessInterval);

    }

    /**
     *  Given a query, return up to limit page titles that match the query string (per Wikipedia's search service).
     * @param query
     * @param limit
     * @return
     */
    public List<String> search(String query, int limit) {
        long currentTime = System.currentTimeMillis() / 1000;
        ArrayList<String> searchResults =  wiki.search(query, limit);
        searchResults.forEach(title -> wikiBuffer.put(new WikiPage(title, wiki.getPageText(title))));
        trackRequest(query, currentTime);
        allRequestsTracker.add(currentTime);
        System.out.println(basicRequestsTracker);
        return searchResults;
    }

    /**
     * Given a pageTitle, return the text associated with the Wikipedia page that matches pageTitle.
     * @param pageTitle
     * @return
     */
    public String getPage(String pageTitle) {
        long currentTime = System.currentTimeMillis() / 1000;
        String text;
        try {
            text = wikiBuffer.get(pageTitle).getText();
        } catch (NoSuchElementException e) {
            text = wiki.getPageText(pageTitle);
        }
        trackRequest(pageTitle, currentTime);
        allRequestsTracker.add(currentTime);
        System.out.println(basicRequestsTracker);
        return text;
    }

    /**
     * Return the most common Strings used in search and getPage requests, with items being sorted in non-increasing count order. When many requests have been made, return only limit items.
     * @param limit
     * @return
     */
    public List<String> zeitgeist(int limit) {
        long currentTime = System.currentTimeMillis() / 1000;
        allRequestsTracker.add(currentTime);
        return basicRequestsTracker.stream().sorted((r1, r2) -> r2.getCountList().size() - r1.getCountList().size())
                .limit(limit).map(request -> request.getRequestString()).collect(Collectors.toList());
    }

    /**
     * Similar to zeitgeist(), but returns the most frequent requests made in the last timeLimitInSeconds seconds. This method should report at most maxItems of the most frequent requests.
     * @param timeLimitInSeconds
     * @param maxItems
     * @return
     */
    public List<String> trending(int timeLimitInSeconds, int maxItems) {
        long currentTime = System.currentTimeMillis() / 1000;
        ArrayList<Request> filteredRequestTracker = new ArrayList<>();
        basicRequestsTracker.stream().forEach(request -> {
            try {
                filteredRequestTracker.add(request.deepFilteredCopy(currentTime,timeLimitInSeconds));
            } catch (NoRecentRequestsException ignored) {}
        });


        allRequestsTracker.add(currentTime);
        return filteredRequestTracker.stream().sorted((r1,r2) -> r2.getCountList().size() - r1.getCountList().size())
                .limit(maxItems).map(request -> request.getRequestString()).collect(Collectors.toList());
    }

    /**
     * What is the maximum number of requests seen in any time window of a given length? The request count is to include all requests made using the public API of WikiMediator, and therefore counts all five methods listed as basic page requests.
     * (There is one more request that appears later, shortestPath, and that should also be included if you do implement that method.)
     * @param timeWindowInSeconds
     * @return
     */
    public int windowedPeakLoad(int timeWindowInSeconds) {
        long currentTime = System.currentTimeMillis() / 1000;
        ArrayList<Long> requestsInWindow;
        for (long time:allRequestsTracker) {
            //add time and some more
        }
        allRequestsTracker.add(currentTime);
        return -1;
    }

    /**
     * This is an overloaded version of the previous method where the time window defaults to 30 seconds. (Calls to this method also affect peak load.)
     * @return
     */
    public int windowedPeakLoad() {
        return windowedPeakLoad(30);
    }

    private void trackRequest(String request, long time) {
//        if (!requestTracker.containsKey(request)) {
//            requestTracker.put(request, new ArrayList<>());
//        }
//        else {
//            requestTracker.get(request).add(time);
//        }
        for (int i = 0; i< basicRequestsTracker.size(); i++) {
            if (basicRequestsTracker.get(i).getRequestString().equals(request)) {
                basicRequestsTracker.get(i).addInstance(time);
                return;
            }
        }
        basicRequestsTracker.add(new Request(request, time));

    }

}
