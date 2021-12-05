package cpen221.mp3;

import cpen221.mp3.wikimediator.WikiMediator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Task3Tests {
    private WikiMediator mediator = new WikiMediator(100, 10);

    public static void delay(int seconds)
    {
        try
        {
            Thread.sleep(seconds* 1000L);
        }
        catch(InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
    }


    @Test
    public void searchDog() {
        List<String> searchResults = mediator.search("dog",10);
        Assertions.assertTrue(searchResults.contains("Dog"));
        Assertions.assertEquals(10, searchResults.size());
    }

    @Test
    public void searchGibberish() {
        List<String> searchResults = mediator.search("zhk gdaireuuerwrhc awbuv illekawrbvaf jvhb ",10);
        Assertions.assertEquals(0, searchResults.size());
    }

    @Test
    public void searchLeast() {
        List<String> searchResults = mediator.search("dog ",1);
        Assertions.assertEquals(1, searchResults.size());
    }

    @Test
    public void getPageBegleri() {
        String pageText = mediator.getPage("begleri");
        Assertions.assertTrue(pageText.contains("begleri"));
    }

    @Test
    public void getPageBegleriCached() {
        String pageText = mediator.getPage("begleri");
        String pageText2 = mediator.getPage("begleri");
        String pageText3 = mediator.getPage("begleri");
        Assertions.assertTrue(pageText.contains("begleri"));
    }

    @Test
    public void getPageGibberish() {
        String pageText = mediator.getPage("awejhkeubyqci r bic whc nkh");
        Assertions.assertEquals(0, pageText.length());
    }

    @Test
    public void zeit() {
        ArrayList<String> list = new ArrayList<>();
        list.add("dog");
        list.add("fish");
        list.add("cat");

        mediator.search("cat",4);
        mediator.getPage("fish");
        mediator.search("dog",4);
        mediator.search("dog",4);
        mediator.getPage("fish");
        mediator.search("dog",4);
        Assertions.assertEquals(list, mediator.zeitgeist(5));
    }

    @Test
    public void zeitLimit() {
        ArrayList<String> list = new ArrayList<>();
        list.add("dog");
        list.add("fish");

        mediator.search("cat",4);
        mediator.getPage("fish");
        mediator.search("dog",4);
        mediator.search("dog",4);
        mediator.getPage("fish");
        mediator.search("dog",4);
        Assertions.assertEquals(list, mediator.zeitgeist(2));
    }

    @Test
    public void zeitEmpty() {
        ArrayList<String> list = new ArrayList<>();
        mediator.search("cat",4);
        mediator.getPage("fish");
        mediator.search("dog",4);
        mediator.search("dog",4);
        mediator.getPage("fish");
        mediator.search("dog",4);
        Assertions.assertEquals(list, mediator.zeitgeist(0));
    }

    @Test
    public void trending() {
        ArrayList<String> list = new ArrayList<>();
        list.add("dog");
        list.add("fish");
        list.add("cat");

        mediator.search("cat",4);
        mediator.getPage("fish");
        mediator.search("dog",4);
        mediator.search("dog",4);
        mediator.getPage("fish");
        mediator.search("dog",4);
        Assertions.assertEquals(list, mediator.trending(10,5));
    }

    @Test
    public void trendingMaxItems() {
        ArrayList<String> list = new ArrayList<>();
        list.add("dog");
        list.add("fish");

        mediator.search("cat",4);
        mediator.getPage("fish");
        mediator.search("dog",4);
        mediator.search("dog",4);
        mediator.getPage("fish");
        mediator.search("dog",4);
        Assertions.assertEquals(list, mediator.trending(5,2));
    }

    @Test
    public void trendingZeroTime() {
        ArrayList<String> list = new ArrayList<>();

        mediator.search("cat",4);
        mediator.getPage("fish");
        mediator.search("dog",4);
        mediator.search("dog",4);
        mediator.getPage("fish");
        mediator.search("dog",4);
        delay(1);
        Assertions.assertEquals(list, mediator.trending(0,5));
    }

    @Test
    public void trendingHalfTime() {
        ArrayList<String> list = new ArrayList<>();
        list.add("dog");
        list.add("fish");
        mediator.search("cat",4);
        mediator.getPage("fish");
        mediator.search("dog",4);
        delay(3);
        mediator.search("dog",4);
        mediator.getPage("fish");
        mediator.search("dog",4);
        Assertions.assertEquals(list, mediator.trending(3,5));
    }

    @Test
    public void trendingNoRecent() {
        ArrayList<String> list = new ArrayList<>();
        mediator.search("cat",4);
        mediator.getPage("fish");
        mediator.search("dog",4);
        mediator.search("dog",4);
        mediator.getPage("fish");
        mediator.search("dog",4);
        delay(3);
        Assertions.assertEquals(list, mediator.trending(2,5));
    }

    @Test
    public void peakLoad() {
        mediator.search("cat",4);
        mediator.getPage("fish");
        mediator.search("dog",4);
        mediator.search("dog",4);
        mediator.getPage("fish");
        delay(3);
        mediator.search("dog",4);
        mediator.trending(2,2);
        mediator.zeitgeist(4);
        Assertions.assertEquals(5, mediator.windowedPeakLoad(3));
    }

    @Test
    public void peakLoadOverloaded() {
        mediator.search("cat",4);
        mediator.getPage("fish");
        mediator.search("dog",4);
        mediator.search("dog",4);
        mediator.getPage("fish");
        delay(32);
        mediator.search("dog",4);
        mediator.trending(2,2);
        mediator.zeitgeist(4);
        Assertions.assertEquals(5, mediator.windowedPeakLoad());
    }

    @Test
    public void peakLoadNoRequests() {
        Assertions.assertEquals(0, mediator.windowedPeakLoad());
    }






}
