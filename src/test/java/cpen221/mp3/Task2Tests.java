package cpen221.mp3;

import cpen221.mp3.fsftbuffer.Bufferable;
import cpen221.mp3.fsftbuffer.FSFTBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

// Uses class T in Task1Tests

public class Task2Tests {
    @Test
    public void testManyPut() {
        try {
            FSFTBuffer<T> buffer = new FSFTBuffer<>(5000, 100000);
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < 5000; i++) {
                int finalI = i;
                Thread t = new Thread(() -> {
                    buffer.put(new T(finalI));
                });
                threads.add(t);
                t.start();
            }
            for (Thread t : threads) {
                t.join();
            }
            Assertions.assertEquals(buffer.getCurrentCapacity(), 5000);
        } catch (InterruptedException ioe) {
            System.out.println("InterruptedException");
        }
    }
}
