package cpen221.mp3;

import cpen221.mp3.server.Request;
import cpen221.mp3.server.WikiMediatorClient;
import cpen221.mp3.server.WikiMediatorServer;
import cpen221.mp3.wikimediator.WikiMediator;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class Task4Tests {

    private static final String IP = "127.0.0.1";
    private static final int PORT = 9012;

    // One client sends a request to a server.
    @Test
    public void singleRequest() {
        try {
            WikiMediatorServer server = new WikiMediatorServer(PORT, 10, new WikiMediator(24, 120));
            Thread serverThread = new Thread(new Runnable() {
                public void run() {
                    server.serve();
                }
            });
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
            Thread serverThread = new Thread(new Runnable() {
                public void run() {
                    server.serve();
                }
            });
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
            Thread serverThread = new Thread(new Runnable() {
                public void run() {
                    server.serve();
                }
            });
            serverThread.start();

            Thread t1 = new Thread(new Runnable() {
                public void run() {
                    try {
                        WikiMediatorClient c1 = new WikiMediatorClient(IP, PORT, new Request("1", "Barack Obama", 50, 1));
                        System.out.println(c1.sendRequest());
                    } catch (IOException ioe) {
                        System.out.println("IOException");
                    }
                }
            });
            Thread t2 = new Thread(new Runnable() {
                public void run() {
                    try {
                        WikiMediatorClient c2 = new WikiMediatorClient(IP, PORT, new Request("2", "Barack Obama", 50, 1));
                        System.out.println(c2.sendRequest());
                    } catch (IOException ioe) {
                        System.out.println("IOException");
                    }
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
            Thread serverThread = new Thread(new Runnable() {
                public void run() {
                    server.serve();
                }
            });
            serverThread.start();

            WikiMediatorClient client = new WikiMediatorClient(IP, PORT, new Request("1", "University of British Columbia", "Santa J. Ono", 100));
            System.out.println(client.sendRequest());
        } catch (IOException ioe) {
            System.out.println("IOException");
        }
    }
}
