package cpen221.mp3;

import cpen221.mp3.server.ClientRequest;
import cpen221.mp3.server.WikiMediatorClient;
import cpen221.mp3.server.WikiMediatorServer;
import cpen221.mp3.wikimediator.WikiMediator;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class Task4Tests {

    private static final String IP = "127.0.0.1";
    private static final int PORT = 9997;

    // One client sends a request to a server.
    // The order of a search result is not predictable.
    @Test
    public void singleRequest() {
        try {
            WikiMediatorServer server = new WikiMediatorServer(PORT, 10, new WikiMediator(24, 120));
            Thread serverThread = new Thread(server::serve);
            serverThread.start();

            WikiMediatorClient client = new WikiMediatorClient(IP, PORT, new ClientRequest("one", "Barack Obama", 12, 100));
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

            WikiMediatorClient client = new WikiMediatorClient(IP, PORT, new ClientRequest("one", "Barack Obama", 500, 1));
            assert Objects.equals(client.sendRequest(), "{\"id\":\"one\",\"status\":\"failed\",\"response\":\"Operation timed out\"}");
        } catch (IOException ioe) {
            System.out.println("IOException");
        }
    }

    // Three clients send three requests to the server, with maxClients being 1.
    // Clients 1 and 2 have unreasonable time needs, so they will time out.
    // Clients 1 and 2 send their requests at the same time. One will run and then time out, the other will instantly fail.
    // Which of Client 1 or 2 will fail is not predictable.
    // Client 3 sends its request after Clients 1 and 2 finish, so Client 3's request will succeed.
    // The order of Client 3's search result is not predictable.
    @Test
    public void tripleRequest() {
        try {
            WikiMediatorServer server = new WikiMediatorServer(PORT, 1, new WikiMediator(24, 120));
            Thread serverThread = new Thread(server::serve);
            serverThread.start();

            Thread t1 = new Thread(() -> {
                try {
                    WikiMediatorClient c1 = new WikiMediatorClient(IP, PORT, new ClientRequest("1", "Barack Obama", 50, 1));
                    System.out.println(c1.sendRequest());
                } catch (IOException ioe) {
                    System.out.println("IOException");
                }
            });
            Thread t2 = new Thread(() -> {
                try {
                    WikiMediatorClient c2 = new WikiMediatorClient(IP, PORT, new ClientRequest("2", "Barack Obama", 50, 1));
                    System.out.println(c2.sendRequest());
                } catch (IOException ioe) {
                    System.out.println("IOException");
                }
            });

            t1.start();
            t2.start();
            t1.join();
            t2.join();

            WikiMediatorClient c3 = new WikiMediatorClient(IP, PORT, new ClientRequest("3", "Barack Obama", 5, 100));
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

            WikiMediatorClient client = new WikiMediatorClient(IP, PORT, new ClientRequest("1", "Philosophy", "Barack Obama", 30));
            assert Objects.equals(client.sendRequest(), "{\"id\":\"1\",\"status\":\"success\",\"response\":[\"Philosophy\",\"Academic bias\",\"Barack Obama\"]}");
        } catch (IOException ioe) {
            System.out.println("IOException");
        }
    }

    // This test tests the save/load state functionality of the WikiMediator server and the zeitgeist function.
    // - Client 1 sends a search request for "Trampoline"
    // - Clients 2, 3 and 4 send search requests for "Trampolining"
    // - Clients 5 and 6 send search requests for "Springfree Trampoline"
    // - Client 7 sends a stop signal
    // - Server saves state and shuts down
    // - Server starts itself up again and loads state
    // - Client 8 sends a zeitgeist request, receiving ["Trampoline", "Springfree Trampoline", "Trampolining"]

    @Test
    public void saveLoadState() {
        try {
            new File("local/state.txt").delete();

            WikiMediatorServer server = new WikiMediatorServer(PORT, 1, new WikiMediator(24, 120));
            Thread serverThread = new Thread(server::serve);
            serverThread.start();

            WikiMediatorClient c1 = new WikiMediatorClient(IP, PORT, new ClientRequest("1", "Trampoline", 5, 100));
            c1.sendRequest();

            WikiMediatorClient c2 = new WikiMediatorClient(IP, PORT, new ClientRequest("2", "Trampolining", 5, 100));
            c2.sendRequest();

            WikiMediatorClient c3 = new WikiMediatorClient(IP, PORT, new ClientRequest("3", "Trampolining", 5, 100));
            c3.sendRequest();

            WikiMediatorClient c4 = new WikiMediatorClient(IP, PORT, new ClientRequest("4", "Trampolining", 5, 100));
            c4.sendRequest();

            WikiMediatorClient c5 = new WikiMediatorClient(IP, PORT, new ClientRequest("5", "Springfree Trampoline", 5, 100));
            c5.sendRequest();

            WikiMediatorClient c6 = new WikiMediatorClient(IP, PORT, new ClientRequest("6", "Springfree Trampoline", 5, 100));
            c6.sendRequest();

            WikiMediatorClient c7 = new WikiMediatorClient(IP, PORT, new ClientRequest("7", "stop"));
            assert Objects.equals(c7.sendRequest(), "{\"id\":\"7\",\"response\":\"bye\"}");

            WikiMediatorServer server2 = new WikiMediatorServer(PORT, 1, new WikiMediator(0, 0));
            Thread serverThread2 = new Thread(server2::serve);
            serverThread2.start();

            WikiMediatorClient c8 = new WikiMediatorClient(IP, PORT, new ClientRequest("8", 4, true, 100));
            assert Objects.equals(c8.sendRequest(), "{\"id\":\"8\",\"status\":\"success\",\"response\":[\"Trampolining\",\"Springfree Trampoline\",\"Trampoline\"]}");
        } catch (IOException ioe) {
            System.out.println("IOException");
        }
    }

//    @Test
//    public void testBFS() {
//        WikiMediator wm = new WikiMediator(5, 100);
//
//        wm.bfs("Philosophy", "Barack Obama");
//        Assertions.assertEquals(Arrays.asList("Philosophy", "Academic bias", "Barack Obama"), wm.getShortest());
//
//        wm.bfs("University of British Columbia", "Darfur crisis");
//        Assertions.assertEquals(Arrays.asList("University of British Columbia", "Justin Trudeau", "Darfur crisis"), wm.getShortest());
//
//        wm.bfs("Alea iacta est", "Malibu, California");
//        Assertions.assertEquals(Arrays.asList("Alea iacta est", "Caesar's Comet", "University of California, Los Angeles", "Malibu, California"), wm.getShortest());
//
//        wm.bfs("Jacques Cartier", "COVID-19");
//        Assertions.assertEquals(Arrays.asList("Jacques Cartier", "Canada", "COVID-19"), wm.getShortest());
//
//        wm.bfs("String theory", "Apple sauce");
//
//        System.out.println(wm.getShortest());
//    }
}
