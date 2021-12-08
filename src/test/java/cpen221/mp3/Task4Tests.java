package cpen221.mp3;

import cpen221.mp3.server.Request;
import cpen221.mp3.server.WikiMediatorClient;
import cpen221.mp3.server.WikiMediatorServer;
import cpen221.mp3.wikimediator.WikiMediator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

public class Task4Tests {

    private static final String IP = "127.0.0.1";
    private static final int PORT = 9999;

    // One client sends a request to a server.
    @Test
    public void singleRequest() {
        try {
            WikiMediatorServer server = new WikiMediatorServer(PORT, 10, new WikiMediator(24, 120));
            Thread serverThread = new Thread(server::serve);
            serverThread.start();

            WikiMediatorClient client = new WikiMediatorClient(IP, PORT, new Request("one", "Barack Obama", 12, 100));
            System.out.println(client.sendRequest());
        } catch (IOException ioe) {
            System.out.println("IOException");
        }
    }

    // One client sends a hefty request with a very demanding timeout. The server responds with a failure.
    @Test
    public void timeout() {
        try {
            WikiMediatorServer server = new WikiMediatorServer(PORT, 10, new WikiMediator(24, 120));
            Thread serverThread = new Thread(server::serve);
            serverThread.start();

            WikiMediatorClient client = new WikiMediatorClient(IP, PORT, new Request("one", "Barack Obama", 50, 1));
            System.out.println(client.sendRequest());
        } catch (IOException ioe) {
            System.out.println("IOException");
        }
    }

    // Three clients send three requests to the server, with maxClients being 1.
    // Clients 1 and 2 have unreasonable time needs, so they will time out.
    // Clients 1 and 2 send their requests at the same time. One will run and then time out, the other will instantly fail.
    // Client 3 sends its request after Clients 1 and 2 finish, so Client 3's request will succeed.
    @Test
    public void tripleRequest() {
        try {
            WikiMediatorServer server = new WikiMediatorServer(PORT, 1, new WikiMediator(24, 120));
            Thread serverThread = new Thread(server::serve);
            serverThread.start();

            Thread t1 = new Thread(() -> {
                try {
                    WikiMediatorClient c1 = new WikiMediatorClient(IP, PORT, new Request("1", "Barack Obama", 50, 1));
                    System.out.println(c1.sendRequest());
                } catch (IOException ioe) {
                    System.out.println("IOException");
                }
            });
            Thread t2 = new Thread(() -> {
                try {
                    WikiMediatorClient c2 = new WikiMediatorClient(IP, PORT, new Request("2", "Barack Obama", 50, 1));
                    System.out.println(c2.sendRequest());
                } catch (IOException ioe) {
                    System.out.println("IOException");
                }
            });

            t1.start();
            t2.start();
            t1.join();
            t2.join();

            WikiMediatorClient c3 = new WikiMediatorClient(IP, PORT, new Request("3", "Barack Obama", 5, 100));
            System.out.println(c3.sendRequest());
        } catch (IOException ioe) {
            System.out.println("IOException");
        } catch (InterruptedException ie) {
            System.out.println("InterruptedException");
        }
    }

    // One client sends a shortestPath request to a server.
    @Test
    public void singleRequestShortestPath() {
        try {
            WikiMediatorServer server = new WikiMediatorServer(PORT, 10, new WikiMediator(24, 120));
            Thread serverThread = new Thread(server::serve);
            serverThread.start();

            WikiMediatorClient client = new WikiMediatorClient(IP, PORT, new Request("1", "String Theory", "Apple Sauce", 500));
            System.out.println(client.sendRequest());
        } catch (IOException ioe) {
            System.out.println("IOException");
        }
    }

    @Test
    public void testBFS() {
        WikiMediator wm = new WikiMediator(5, 100);

        wm.bfs("Philosophy", "Barack Obama");
        Assertions.assertEquals(Arrays.asList("Philosophy", "Academic bias", "Barack Obama"), wm.getShortest());

        wm.bfs("University of British Columbia", "Darfur crisis");
        Assertions.assertEquals(Arrays.asList("University of British Columbia", "Justin Trudeau", "Darfur crisis"), wm.getShortest());

        wm.bfs("Alea iacta est", "Malibu, California");
        Assertions.assertEquals(Arrays.asList("Alea iacta est", "Caesar's Comet", "University of California, Los Angeles", "Malibu, California"), wm.getShortest());

        wm.bfs("Jacques Cartier", "COVID-19");
        Assertions.assertEquals(Arrays.asList("Jacques Cartier", "Canada", "COVID-19"), wm.getShortest());

//        System.out.println(wm.getShortest());
    }
}
