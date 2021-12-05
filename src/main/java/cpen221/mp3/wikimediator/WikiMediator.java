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

    private final Wiki wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();
    private final FSFTBuffer<WikiPage> wikiBuffer;
    private final List<Request> requestsTracker = Collections.synchronizedList(new ArrayList<>());
    private final List<Long> allRequestsTracker = Collections.synchronizedList(new ArrayList<>());

    /**
     * Creates a new WikiMediator instance that is capable of caching Wikipedia pages, with cache specifications
     * determined by capacity and stalenessInterval.
     *
     * @param capacity the maximum number of Wikipedia pages that can be cached at any given time, must be > 0.
     * @param stalenessInterval the maximum time, in seconds, that each Wikipedia page remains in the cache before
     *                          being removed, must be > 0.
     */
    public WikiMediator(int capacity, int stalenessInterval) {
        wikiBuffer = new FSFTBuffer<>(capacity, stalenessInterval);

    }

    /**
     * Given a query, return up to limit page titles that match the query string (per Wikipedia's search service).
     *
     * @param query the String to search Wikipedia for.
     * @param limit the maximum number of page titles to return, must be > 0.
     * @return a List with a length of limit containing page titles found by searching Wikipedia for query.
     * If less than limit pages are found, the length of the List equals the number of pages found.
     */
    public List<String> search(String query, int limit) {
        long currentTime = System.currentTimeMillis() / 1000;
        ArrayList<String> searchResults = wiki.search(query, limit);
        trackRequest(query, currentTime);
        allRequestsTracker.add(currentTime);
        return searchResults;
    }

    /**
     * Given a pageTitle, return the text associated with the Wikipedia page that matches pageTitle.
     *
     * @param pageTitle the title of a Wikipedia page.
     * @return the text of the Wikipedia page with the title pageTitle. If no page with the title pageTitle is found,
     * return an empty String.
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
     * Given a limit, return the most common Strings used in search() and getPage() requests,
     * with items being sorted in non-increasing count order.
     * When many requests have been made, return only limit items.
     *
     * @param limit the maximum number of Strings in the returned List, must be >= 0.
     * @return a List containing Strings used in search() and getPage() requests,
     * with items being sorted by how often they were used in such requests,
     * in non-increasing count order. The size of the List is at most limit.
     */
    public List<String> zeitgeist(int limit) {
        long currentTime = System.currentTimeMillis() / 1000;
        allRequestsTracker.add(currentTime);
        //sort requestTracker based on how many times each request was made and map the requests to their String forms
        synchronized (requestsTracker) {
            return requestsTracker.stream().sorted((r1, r2) -> r2.getCountList().size() - r1.getCountList().size())
                    .limit(limit).map(Request::getRequestString).collect(Collectors.toList());
        }
    }

    /**
     * Similar in function to zeitgeist(), but only considers Strings used in search() and getPage() requests
     * made in the last timeLimitInSeconds seconds.
     *
     * @param timeLimitInSeconds a number of seconds such that only search() and getPage() requests in the timeframe
     *                           [currentTime - timeLimitInSeconds, currentTime] are considered, must be >= 0.
     * @param maxItems the maximum number of Strings in the returned List, must be >= 0.
     * @return a List containing Strings used in search() and getPage() requests within the last timeLimitInSeconds
     * seconds, with items being sorted by how often they were used in the last timeLimitInSeconds seconds,
     * in non-increasing count order. The size of the List is at most maxItems.
     */
    public List<String> trending(int timeLimitInSeconds, int maxItems) {
        long currentTime = System.currentTimeMillis() / 1000;
        ArrayList<Request> filteredRequestTracker = new ArrayList<>();
        //create a filteredRequestTracker by filtering out all request times that are outside the time range
        synchronized (requestsTracker) {
            requestsTracker.forEach(request -> {
                try {
                    filteredRequestTracker.add(request.deepFilteredCopy(currentTime, timeLimitInSeconds));
                } catch (NoRecentRequestsException ignored) {}
            });
        }
        allRequestsTracker.add(currentTime);
        synchronized (requestsTracker) {
            return filteredRequestTracker.stream().sorted((r1, r2) -> r2.getCountList().size() - r1.getCountList().size())
                    .limit(maxItems).map(Request::getRequestString).collect(Collectors.toList());
        }
    }

    /**
     * Given certain length of time, return the maximum number of requests made to this
     * in any time window of the given length, not counting this current request.
     *
     * @param timeWindowInSeconds a length of time in seconds, must be >= 0.
     * @return the maximum number of requests made to this in any time window of timeWindowInSeconds length,
     * not counting this current request.
     */
    public int windowedPeakLoad(int timeWindowInSeconds) {
        long currentTime = System.currentTimeMillis() / 1000;
        ArrayList<Long> requestsInWindow;
        int peakLoad = 0;
        //try all possible starting points for the time window to determine peakLoad
        synchronized (allRequestsTracker) {
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
        }
        allRequestsTracker.add(currentTime);
        return peakLoad;
    }

    /**
     * An overloaded version of the previous method where the time window defaults to 30 seconds.
     *
     * @return the maximum number of requests made to this in any time window of 30 seconds,
     * not counting this current request.
     */
    public int windowedPeakLoad() {
        return windowedPeakLoad(30);
    }

    /**
     * A helper method to track Strings used in search() and getPage() requests for use when executing
     * zeitgeist() and trending().
     *
     * @param request the String for which getPage() or search() was called.
     * @param time the system time at which getPage() or search() was called, in seconds.
     */
    private void trackRequest(String request, long time) {
        synchronized (requestsTracker) {
            for (Request value : requestsTracker) {
                if (value.getRequestString().equals(request)) {
                    value.addInstance(time);
                    return;
                }
            }
        }
        requestsTracker.add(new Request(request, time));

    }

    /**
     * Find the shortest path between two Wikipedia pages.
     * If two paths are equally long, the lexicographically smaller one is returned.
     * Search ends at timeout but the method does not necessarily return at timeout.
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
        long currentTime = System.currentTimeMillis() / 1000;
        allRequestsTracker.add(currentTime);

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

        sortByLengthThenLex();

        List<String> shortestPath = realPaths.get(0);
        shortestPath.add(0, pageTitle1);
        shortestPath.add(pageTitle2);

        return shortestPath;
    }

    private void sortByLengthThenLex() {
        // sort by length
        realPaths.sort(Comparator.comparingInt(List::size));

        int size = realPaths.get(0).size();
        realPaths = realPaths.stream().filter(x -> x.size() == size).collect(Collectors.toList());

        // sort by lexicographic order
        realPaths.sort(Comparator.comparing(l -> l.get(0)));
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
    private List<List<String>> realPaths = new ArrayList<>();

    private synchronized void updateLimit(int limit) {
        this.limit = Math.min(limit, this.limit);
    }
}