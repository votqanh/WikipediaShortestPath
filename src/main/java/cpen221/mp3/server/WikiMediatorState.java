package cpen221.mp3.server;

import cpen221.mp3.wikimediator.Request;
import cpen221.mp3.wikimediator.WikiMediator;

import java.util.*;

public class WikiMediatorState {

    public int capacity;
    public int timeout;
    public int currentCapacity;
    public Map buffer;
    public List bufferIds;

    public List<Request> requestsTracker;
    public List<Long> allRequestsTracker;

    public WikiMediatorState(WikiMediator wm) {
        capacity = wm.getFSFTBuffer().getCapacity();
        timeout = wm.getFSFTBuffer().getTimeout();
        currentCapacity = wm.getFSFTBuffer().getCurrentCapacity();
        buffer = new HashMap(wm.getFSFTBuffer().getBuffer());
        bufferIds = new ArrayList(wm.getFSFTBuffer().getBufferIds());
        requestsTracker = wm.getRequestsTracker();
        allRequestsTracker = wm.getAllRequestTracker();
    }
}
