package cpen221.mp3;

import cpen221.mp3.fsftbuffer.FSFTBuffer;
import cpen221.mp3.wikimediator.WikiMediator;
import cpen221.mp3.wikimediator.WikiPage;
import org.fastily.jwiki.core.Wiki;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class Task1Tests {

    private FSFTBuffer<WikiPage> buffer1 = new FSFTBuffer<>(5,5);
    private FSFTBuffer<WikiPage> buffer2 = new FSFTBuffer<>();

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

    }
}
