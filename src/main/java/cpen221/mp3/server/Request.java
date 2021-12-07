package cpen221.mp3.server;

public class Request {

    public String id;
    public String type;

    public String query;
    public int limit; // Used in 2 methods

    public String pageTitle;

    public int timeLimitInSeconds;
    public int maxItems;

    public int timeWindowInSeconds = -1;

    public int timeout;

    public Request(String id, String query, int limit, int timeout) {
        type = "search";
        this.id = id;
        this.query = query;
        this.limit = limit;
        this.timeout = timeout;
    }

    public Request(String id, String pageTitle, int timeout) {
        type = "getPage";
        this.id = id;
        this.pageTitle = pageTitle;
        this.timeout = timeout;
    }

    // Since there are two methods with one int parameter, we need to distinguish them
    public Request(String id, int num, boolean isZeitgeist, int timeout) {
        if (isZeitgeist) {
            type = "zeitgeist";
            this.id = id;
            this.limit = num;
            this.timeout = timeout;
        } else {
            type = "windowedPeakLoad";
            this.id = id;
            this.timeWindowInSeconds = num;
            this.timeout = timeout;
        }
    }

    public Request(String id, int timeLimitInSeconds, int maxItems, int timeout) {
        type = "trending";
        this.id = id;
        this.timeLimitInSeconds = timeLimitInSeconds;
        this.maxItems = maxItems;
        this.timeout = timeout;
    }

    public Request(String id, int timeout) {
        type = "windowedPeakLoad";
        this.id = id;
        this.timeout = timeout;
    }
}
