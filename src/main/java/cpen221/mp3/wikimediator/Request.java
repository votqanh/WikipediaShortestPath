package cpen221.mp3.wikimediator;

import java.util.ArrayList;

public class Request {
    private final String requestString;
    private final ArrayList<Long> countList;

    /* Representation Invariant */
    // requestString and countList are not null
    // countList.size() >= 1

    /* Abstraction Function */
    // An instance of Request represents a unique String input to search() or getPage() and
    // the corresponding times search() or getPage() has been called on that particular String, represented
    // by an ArrayList of system times in seconds.

    /**
     * Creates an instance of Request from a String and the first time the String was requested.
     *
     * @param request the String parameter of getPage() or search().
     * @param firstTime the first time getPage() or search() was called with request as a parameter,
     *                  in system time with the unit of seconds.
     */
    public Request(String request, long firstTime) {
        requestString = request;
        countList = new ArrayList<>();
        countList.add(firstTime);
    }

    /**
     * Creates a new Request from an old one, where only instances of getPage() or search() called with request
     * as a parameter that occurred in the time interval [currentTime - timeWindow, currentTime] are kept.
     *
     * @param currentTime the time representing the end of the time interval, in system time with the unit seconds.
     * @param timeWindow the time, such that currentTime - timeWindow represents the start of the time interval,
     *                   in system time with the unit seconds.
     * @return an instance of Request similar to this except that instances of getPage() or search() called with request
     * as a parameter that occurred outside the time interval [currentTime - timeWindow, currentTime] are removed.
     * @throws NoRecentRequestsException if after applying the filter, no time instances remain.
     */
    public Request deepFilteredCopy(long currentTime, long timeWindow) throws NoRecentRequestsException {

        long filteredFirstTime = -1;
        for (int i=0;i<this.getCountList().size();i++) {
            if (this.getCountList().get(i) >= currentTime - timeWindow) {
                filteredFirstTime = this.getCountList().get(i);
                break;
            }
        }
        if (filteredFirstTime == -1) {
            throw new NoRecentRequestsException();
        }
        Request filteredRequest = new Request(this.getRequestString(),filteredFirstTime);
        this.getCountList().forEach(time -> {if (time >= currentTime-timeWindow) filteredRequest.addInstance(time);});
        return filteredRequest;
    }

    /**
     * Add an instance of time for which the String request was passed as a parameter to getPage() or search().
     *
     * @param time the system time in seconds at which the String request was passed as a parameter to
     *             getPage() or search().
     */
    public void addInstance(long time) {
        countList.add(time);
    }

    /**
     * @return the request String associated with this.
     */
    public String getRequestString() {
        return requestString;
    }

    /**
     * @return an ArrayList containing all the times a request for
     */
    public ArrayList<Long> getCountList() {
        return (ArrayList<Long>) countList.clone();
    }

    //just for testing
    @Override
    public String toString() {
        return
                requestString + countList.size();
    }
}
