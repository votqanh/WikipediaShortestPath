package cpen221.mp3.wikimediator;

import java.util.ArrayList;
import java.util.Objects;

public class Request {
    private final String requestString;
    private final ArrayList<Long> countList;

    public Request(String request, long firstTime) {
        requestString = request;
        countList = new ArrayList<>();
        countList.add(firstTime);
    }

    public Request deepFilteredCopy(long currentTime, long timeWindow) throws NoRecentRequestsException{

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


    public void addInstance(long time) {
        countList.add(time);
    }

    public String getRequestString() {
        return requestString;
    }

    public ArrayList<Long> getCountList() {
        return (ArrayList<Long>) countList.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Request request = (Request) o;
        return Objects.equals(getRequestString(), request.getRequestString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRequestString());
    }

    @Override
    public String toString() {
        return
                requestString + countList.size();
    }
}
