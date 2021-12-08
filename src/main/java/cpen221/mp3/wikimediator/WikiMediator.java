package cpen221.mp3.wikimediator;

import cpen221.mp3.fsftbuffer.FSFTBuffer;
import cpen221.mp3.server.WikiMediatorState;
import org.fastily.jwiki.core.Wiki;

import java.util.*;
import java.io.*;
import com.google.gson.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class WikiMediator {
    private final Wiki wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();
    private FSFTBuffer<WikiPage> wikiBuffer;
    private List<Request> requestsTracker = Collections.synchronizedList(new ArrayList<>());
    private List<Long> allRequestsTracker = Collections.synchronizedList(new ArrayList<>());

    private List<List<String>> path;

    /* Representation Invariant */
    // wikiBuffer, requestTracker, and allRequestTracker are not null
    // wikiBuffer and requestsTracker contain no null elements
    // allRequestTracker.size() >= requestsTracker.size()

    /* Abstraction Function */
    // WikiMediator represents a mediator service for Wikipedia which allows users to search, access and
    // cache Wikipedia's pages, as well as providing metrics for the use of this service.

    /**
     * Private method to check that the representation invariant holds, not present in any of the final
     * code for performance reasons.
     */
    private void checkRep() {
        assert !wikiBuffer.equals(null);
        assert !requestsTracker.equals(null);
        assert !allRequestsTracker.equals(null);
        for (Request r : requestsTracker) {
            assert !r.equals(null);
        }
        assert allRequestsTracker.size() >= requestsTracker.size();
    }


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
     * @param pageTitle1 a Wikipedia page title
     * @param pageTitle2 a Wikipedia page title
     * @param timeout in seconds, is greater than 0
     * @return the path including {@code pageTitle1} and {@code pageTitle2},
     *          or {@code pageTitle1} if {@code pageTitle1} and {@code pageTitle2} are the same
     * @throws TimeoutException if no path is found
     */
    public List<String> shortestPath(String pageTitle1, String pageTitle2, int timeout) throws TimeoutException {
        long currentTime = System.currentTimeMillis() / 1000;
        allRequestsTracker.add(currentTime);

        if (Objects.equals(pageTitle1, pageTitle2)) {
            return List.of(pageTitle1);
        }

        BFS search = new BFS(this, pageTitle1, pageTitle2);
        Thread t = new Thread(search);
        Timer timer = new Timer();
        timer.schedule(new Timeout(t, timer), timeout * 1000L);
        t.start();

        while(t.isAlive());

        if (path.isEmpty()) {
            throw new TimeoutException();
        }

        return path.get(0);
    }

    //TODO: make method private before submitting
    /**
     * Find all paths between two Wikipedia pages using BFS
     *
     * @param start the starting page
     * @param target the target page
     */
    public void bfs(String start, String target) {
        path = new ArrayList<>();

        List<String> children = wiki.getLinksOnPage(true, start);
        Collections.sort(children);

        // 1 degree of separation
        if (children.contains(target)) {
            path.add(Arrays.asList(start, target));
            return;
        }

        List<String> linksToTarget = wiki.whatLinksHere(target);
        int degrees = 2;

        while (true) {
            for (String c : children) {
                if (degrees == 2 && linksToTarget.contains(c)) {
                    path.add(Arrays.asList(start, c, target));
                    return;
                }

                if (degrees >= 3) {
                    List<String> grandchildren = wiki.getLinksOnPage(true, c);
                    Collections.sort(grandchildren);

                    for (String gc : grandchildren) {
                        if (degrees == 3 && linksToTarget.contains(gc)) {
                            path.add(Arrays.asList(start, c, gc, target));
                            return;
                        }

                        if (degrees >= 4) {
                            List<String> greatgrandchildren = wiki.getLinksOnPage(true, gc);
                            Collections.sort(greatgrandchildren);

                            for (String ggc : greatgrandchildren) {
                                if (degrees == 4 && linksToTarget.contains(ggc)) {
                                    path.add(Arrays.asList(start, c, gc, ggc, target));
                                    return;
                                }

                                if (degrees >= 5) {
                                    //great-great-gc
                                    List<String> g2grandchildren = wiki.getLinksOnPage(true, ggc);
                                    Collections.sort(g2grandchildren);

                                    for (String g2gc : g2grandchildren) {
                                        if (degrees == 5 && linksToTarget.contains(g2gc)) {
                                            path.add(Arrays.asList(start, c, gc, ggc, g2gc, target));
                                            return;
                                        }

                                        if (degrees >= 6) {
                                            List<String> g3grandchildren = wiki.getLinksOnPage(true, g2gc);
                                            Collections.sort(g3grandchildren);

                                            for (String g3gc : g3grandchildren) {
                                                if (degrees == 6 && linksToTarget.contains(g3gc)) {
                                                    path.add(Arrays.asList(start, c, gc, ggc, g2gc, g3gc, target));
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            degrees++;
        }
    }

    public void saveState() {
        try {
            File stateFile = new File("local/state.txt");
            if (stateFile.createNewFile()) {
                Gson gson = new GsonBuilder().create();
                FileWriter writer = new FileWriter(stateFile);
                writer.write(gson.toJson(new WikiMediatorState(this)));
                writer.close();
            } else {
                // File already exists
            }
        } catch (IOException ioe) {
            System.out.println("IOException in creating state");
        }
    }

    public void loadState() {
        try {
            File stateFile = new File("local/state.txt");
            Scanner scanner = new Scanner(stateFile);
            while (scanner.hasNextLine()) {
                Gson gson = new GsonBuilder().create();
                String line = scanner.nextLine();
                WikiMediatorState state = gson.fromJson(line, WikiMediatorState.class);
                wikiBuffer = new FSFTBuffer<>();
                wikiBuffer.loadState(state);
                requestsTracker = state.requestsTracker;
                allRequestsTracker = state.allRequestsTracker;
            }
            System.out.println("Load state found!");
        } catch (FileNotFoundException fnfe) {
            System.out.println("Load state not found, starting fresh.");
        }
    }

    // TODO: remove before submitting
    public List<String> getShortest() {
        return new ArrayList<>(path.get(0));
    }

    /**
     * Below is a collection of observer methods that allow the construction of WikiMediatorState,
     * and a method that uses a WikiMediatorState to load a state.
     */
    public FSFTBuffer getFSFTBuffer() { return wikiBuffer; }
    public List getRequestsTracker() { return requestsTracker; }
    public List getAllRequestTracker() { return allRequestsTracker; }
}

class BFS implements Runnable {
    private final String start;
    private final String target;
    private final WikiMediator wm;

    public BFS(WikiMediator wm, String start, String target) {
        this.start = start;
        this.target = target;
        this.wm = wm;
    }

    @Override
    public void run() {
        wm.bfs(start, target);
    }
}

class Timeout extends TimerTask {
    private final Thread t;
    private final Timer timer;

    public Timeout(Thread t, Timer timer) {
        this.t = t;
        this.timer = timer;
    }

    public void run() {
        if (t != null && t.isAlive()) {
            t.interrupt();
            timer.cancel();
        }
    }
}