package cpen221.mp3;

import cpen221.mp3.fsftbuffer.Bufferable;
import cpen221.mp3.fsftbuffer.FSFTBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

class T implements Bufferable {
    private final int s;

    public T (int s) {
        this.s = s;
    }

    public String id() {
        return String.valueOf(s);
    }
}

public class Task1Tests {
    @Test
    public void testDefault() {
        FSFTBuffer<T> buffer = new FSFTBuffer<>();

        for (int i = 1; i <= 32; i++) {
            T s = new T(i);
            Assertions.assertTrue(buffer.put(s));
        }

        T s = new T(33);
        Assertions.assertFalse(buffer.put(s));
    }

    @Test
    public void testUpdate() throws InterruptedException {
        FSFTBuffer<T> buffer = new FSFTBuffer<>(3, 3);

        T a = new T(1);
        Assertions.assertTrue(buffer.put(a));
        Assertions.assertFalse(buffer.put(a));

        T b = new T(2);
        Assertions.assertTrue(buffer.put(b));

        T c = new T(3);
        Assertions.assertTrue(buffer.put(c));

        TimeUnit.SECONDS.sleep(1);

        Assertions.assertTrue(buffer.update(a));

        TimeUnit.SECONDS.sleep(2);

        Assertions.assertEquals(a, buffer.get("1"));
        Assertions.assertFalse(buffer.update(b));
        Assertions.assertFalse(buffer.update(c));
    }

    @Test
    public void testRemoval() {
        FSFTBuffer<T> buffer = new FSFTBuffer<>(3, 3);

        T a = new T(1);
        Assertions.assertTrue(buffer.put(a));

        T b = new T(2);
        Assertions.assertTrue(buffer.put(b));

        T c = new T(3);
        Assertions.assertTrue(buffer.put(c));

        Assertions.assertEquals(c, buffer.get("3"));
        Assertions.assertEquals(b, buffer.get("2"));

        T d = new T(4);
        Assertions.assertTrue(buffer.put(d));


        Assertions.assertFalse(buffer.touch("1"));
        Assertions.assertThrows(NoSuchElementException.class, () -> buffer.get("1"));

    }
}
