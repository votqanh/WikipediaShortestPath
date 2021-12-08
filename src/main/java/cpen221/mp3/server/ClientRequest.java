package cpen221.mp3.server;

import java.util.Objects;

public class ClientRequest {

    public String id;
    public String type;

    public String query;
    public int limit; // Used in 2 methods

    public String pageTitle;
    public String pageTitle2;

    public int timeLimitInSeconds;
    public int maxItems;

    public int timeWindowInSeconds = -1;

    public int timeout;

    public ClientRequest(String id, String query, int limit, int timeout) {
        type = "search";
        this.id = id;
        this.query = query;
        this.limit = limit;
        this.timeout = timeout;
    }

    public ClientRequest(String id, String pageTitle, int timeout) {
        type = "getPage";
        this.id = id;
        this.pageTitle = pageTitle;
        this.timeout = timeout;
    }

    // Since there are two methods with one int parameter, we need to distinguish them
    public ClientRequest(String id, int num, boolean isZeitgeist, int timeout) {
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

    public ClientRequest(String id, int timeLimitInSeconds, int maxItems, int timeout) {
        type = "trending";
        this.id = id;
        this.timeLimitInSeconds = timeLimitInSeconds;
        this.maxItems = maxItems;
        this.timeout = timeout;
    }

    public ClientRequest(String id, int timeout) {
        type = "windowedPeakLoad";
        this.id = id;
        this.timeout = timeout;
    }

    public ClientRequest(String id, String pageTitle1, String pageTitle2, int timeout) {
        type = "shortestPath";
        this.id = id;
        this.pageTitle = pageTitle1;
        this.pageTitle2 = pageTitle2;
        this.timeout = timeout;
    }

    public ClientRequest(String id, String type) {
        this.id = id;
        if (Objects.equals(type, "stop")) {
            this.type = "stop";
        }
    }
}
