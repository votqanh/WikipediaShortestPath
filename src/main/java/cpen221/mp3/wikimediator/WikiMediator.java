package cpen221.mp3.wikimediator;

import cpen221.mp3.fsftbuffer.FSFTBuffer;
import org.fastily.jwiki.core.Wiki;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
    private final ArrayList<Request> requestsTracker = new ArrayList<>();
    private final ArrayList<Long> allRequestsTracker = new ArrayList<>();

    /**
     * @param capacity
     * @param stalenessInterval
     */
    public WikiMediator(int capacity, int stalenessInterval) {
        wikiBuffer = new FSFTBuffer<>(capacity, stalenessInterval);

    }

    /**
     * Given a query, return up to limit page titles that match the query string (per Wikipedia's search service).
     *
     * @param query
     * @param limit
     * @return
     */
    public List<String> search(String query, int limit) {
        long currentTime = System.currentTimeMillis() / 1000;
        ArrayList<String> searchResults = wiki.search(query, limit);
        searchResults.forEach(title -> wikiBuffer.put(new WikiPage(title, wiki.getPageText(title))));
        trackRequest(query, currentTime);
        allRequestsTracker.add(currentTime);
        System.out.println(requestsTracker);
        return searchResults;
    }

    /**
     * Given a pageTitle, return the text associated with the Wikipedia page that matches pageTitle.
     *
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
            if (!text.equals("")) {
                wikiBuffer.put(new WikiPage(pageTitle, text));
            }
        }
        trackRequest(pageTitle, currentTime);
        allRequestsTracker.add(currentTime);
        System.out.println(requestsTracker);
        return text;
    }

    /**
     * Return the most common Strings used in search and getPage requests, with items being sorted in non-increasing count order. When many requests have been made, return only limit items.
     *
     * @param limit
     * @return
     */
    public List<String> zeitgeist(int limit) {
        long currentTime = System.currentTimeMillis() / 1000;
        allRequestsTracker.add(currentTime);
        return requestsTracker.stream().sorted((r1, r2) -> r2.getCountList().size() - r1.getCountList().size())
                .limit(limit).map(Request::getRequestString).collect(Collectors.toList());
    }

    /**
     * Similar to zeitgeist(), but returns the most frequent requests made in the last timeLimitInSeconds seconds. This method should report at most maxItems of the most frequent requests.
     *
     * @param timeLimitInSeconds
     * @param maxItems
     * @return
     */
    public List<String> trending(int timeLimitInSeconds, int maxItems) {
        long currentTime = System.currentTimeMillis() / 1000;
        ArrayList<Request> filteredRequestTracker = new ArrayList<>();
        requestsTracker.stream().forEach(request -> {
            try {
                filteredRequestTracker.add(request.deepFilteredCopy(currentTime, timeLimitInSeconds));
            } catch (NoRecentRequestsException ignored) {
            }
        });


        allRequestsTracker.add(currentTime);
        return filteredRequestTracker.stream().sorted((r1, r2) -> r2.getCountList().size() - r1.getCountList().size())
                .limit(maxItems).map(Request::getRequestString).collect(Collectors.toList());
    }

    /**
     * What is the maximum number of requests seen in any time window of a given length? The request count is to include all requests made using the public API of WikiMediator, and therefore counts all five methods listed as basic page requests.
     * (There is one more request that appears later, shortestPath, and that should also be included if you do implement that method.)
     *
     * @param timeWindowInSeconds
     * @return
     */
    public int windowedPeakLoad(int timeWindowInSeconds) {
        long currentTime = System.currentTimeMillis() / 1000;
        ArrayList<Long> requestsInWindow;
        int peakLoad = 0;
        for (int i = 0; i < allRequestsTracker.size(); i++) {
            requestsInWindow = new ArrayList<>();
            for (int j = i; j < allRequestsTracker.size(); j++) {
                if (allRequestsTracker.get(j) >= allRequestsTracker.get(i) + timeWindowInSeconds) {
                    break;
                }
                requestsInWindow.add(allRequestsTracker.get(j));
            }
            if (requestsInWindow.size() > peakLoad) {
                peakLoad = requestsInWindow.size();
            }

        }
        allRequestsTracker.add(currentTime);
        return peakLoad;
    }

    /**
     * This is an overloaded version of the previous method where the time window defaults to 30 seconds. (Calls to this method also affect peak load.)
     *
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
        for (Request value : requestsTracker) {
            if (value.getRequestString().equals(request)) {
                value.addInstance(time);
                return;
            }
        }
        requestsTracker.add(new Request(request, time));

    }

    /**
     * Find the shortest and lexicographically smallest path between two Wikipedia pages
     * Search ends at timeout but the method does not necessarily return at timeout
     *
     * @param pageTitle1, is not null
     * @param pageTitle2, is not null
     * @param timeout, is greater than 0
     * @return the path including the start and target pages
     * @throws TimeoutException if no path is found
     */

    public List<String> shortestPath(String pageTitle1, String pageTitle2, int timeout) throws TimeoutException {
        if (Objects.equals(pageTitle1, pageTitle2)) {
            return Arrays.asList(pageTitle1, pageTitle2);
        }

        List<String> firstDepth = wiki.getLinksOnPage(pageTitle1);

        ForkJoinPool forkJoinPool = new ForkJoinPool();
        try {
             forkJoinPool.submit(() -> firstDepth.parallelStream()
                            .map(title -> BFS(title, pageTitle2)).collect(Collectors.toList()))
                            .get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            if (limit == 1000) {
                throw new TimeoutException();
            }
        }

        realPaths.sort(new ListComparator<>());

        List<String> shortestPath = realPaths.get(realPaths.size() - 1);
        shortestPath.add(0, pageTitle1);
        shortestPath.add(pageTitle2);

        return shortestPath;
    }

    private boolean BFS(String start, String target) {
        List<String> children = wiki.getLinksOnPage(start);
        int depth = 0;

        if (children.contains(target)) {
            updateLimit(depth);
            List<String> path =  Arrays.asList(start, target);
            realPaths.add(path);
            return true;
        }

        List<List<String>> paths = new ArrayList<>();

        for (String c : children) {
            List<String> p = Arrays.asList(start, c);
            paths.add(p);
        }

        while (depth <= limit) {
            List<List<String>> tempPaths = new ArrayList<>();
            boolean found = false;

            for (List<String> p : paths) {
                List<String> grandchildren = wiki.getLinksOnPage(p.get(p.size() - 1));

                if (grandchildren.contains(target)) {
                    updateLimit(depth);
                    p.add(target);
                    realPaths.add(p);
                    found = true;
                    continue;
                }

                if (!found) {
                    for (String gc : grandchildren) {
                        List<String> temp = new ArrayList<>(p);
                        temp.add(gc);
                        tempPaths.add(temp);
                    }
                }
            }

            paths = new ArrayList<>(tempPaths);
            depth++;
        }

        return true;
    }

    private int limit = 1000;
    private final List<List<String>> realPaths = new ArrayList<>();

    private synchronized void updateLimit(int limit) {
        this.limit = Math.min(limit, this.limit);
    }
}

class ListComparator<T extends Comparable<T>> implements Comparator<List<T>> {

    @Override
    public int compare(List<T> o1, List<T> o2) {
        for (int i = 0; i < Math.min(o1.size(), o2.size()); i++) {
            int c = o1.get(i).compareTo(o2.get(i));
            if (c != 0) {
                return c;
            }
        }
        return Integer.compare(o1.size(), o2.size());
    }

}