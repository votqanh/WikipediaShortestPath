package cpen221.mp3.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cpen221.mp3.wikimediator.WikiMediator;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;

public class WikiMediatorServer {

    private final ServerSocket serverSocket;
    private final WikiMediator wikiMediator;

    private boolean active = true;
    private int numClients = 0;
    private final int maxClients;

    /**
     * Start a server at a given port number, with the ability to process
     * up to n requests concurrently.
     *
     * @param port the port number to bind the server to, 9000 <= {@code port} <= 9999
     * @param n the number of concurrent requests the server can handle, 0 < {@code n} <= 32
     * @param wikiMediator the WikiMediator instance to use for the server, {@code wikiMediator} is not {@code null}
     */
    public WikiMediatorServer(int port, int n, WikiMediator wikiMediator) throws IOException {

        System.out.println("Starting...");
        this.maxClients = n;
        this.wikiMediator = wikiMediator;
        serverSocket = new ServerSocket(port);
        System.out.println("Server created");
    }

    /**
     * Go into a state where the server can continuously handle client requests
     * until a quit signal is received.
     */

    public void serve() {

        while (active) {

            Thread handler = new Thread(() -> {
                try {
                    try (Socket socket = serverSocket.accept()) {
                        handle(socket, changeNumClients(1));
                    }
                } catch (IOException ioe) {
                    throw new RuntimeException();
                }
            });

            handler.start();
        }
    }

    /**
     * Handle one request from a client.
     *
     * @param socket The client socket that the request is received from.
     * @param overflow Whether maxClients has been exceeded by numClients during the time the request was received.
     */

    private void handle(Socket socket, boolean overflow) throws IOException, IllegalArgumentException {

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

        String input = in.readLine();
        Gson gson = new GsonBuilder().create();
        Request request = gson.fromJson(input, Request.class);

        Response response = new Response();
        response.id = request.id;
        response.status = overflow ? "failed" : "success";

        System.out.println("Handling request from id " + request.id);

        Thread timeoutThread = new Thread(new MyRunnable(Thread.currentThread(), socket, in, out, response.id, request.timeout));
        timeoutThread.start();

        try {
            Method m;

            if (overflow) {
                response.response = "Client overflow";
            } else {
                switch (request.type) {
                    case "search":
                        m = WikiMediator.class.getDeclaredMethod(request.type, String.class, int.class);
                        response.response = m.invoke(wikiMediator, request.query, request.limit);
                        break;
                    case "getPage":
                        m = WikiMediator.class.getDeclaredMethod(request.type, String.class);
                        response.response = m.invoke(wikiMediator, request.pageTitle);
                        break;
                    case "zeitgeist":
                        m = WikiMediator.class.getDeclaredMethod(request.type, int.class);
                        response.response = m.invoke(wikiMediator, request.limit);
                        break;
                    case "trending":
                        m = WikiMediator.class.getDeclaredMethod(request.type, int.class, int.class);
                        response.response = m.invoke(wikiMediator, request.timeLimitInSeconds, request.maxItems);
                        break;
                    case "windowedPeakLoad":
                        if (request.timeWindowInSeconds == -1) {
                            m = WikiMediator.class.getDeclaredMethod(request.type);
                            response.response = m.invoke(wikiMediator);
                        } else {
                            m = WikiMediator.class.getDeclaredMethod(request.type, int.class);
                            response.response = m.invoke(wikiMediator, request.timeWindowInSeconds);
                        }
                        break;
                    case "shortestPath":
                        m = WikiMediator.class.getDeclaredMethod(request.type, String.class, String.class, int.class);
                        response.response = m.invoke(wikiMediator, request.pageTitle, request.pageTitle2, request.timeout);
                        break;
                    case "stop":
                        response.status = null; // So that GSON skips this field
                        response.response = "bye";
                        active = false;
                }
            }

            out.println(gson.toJson(response));
            in.close();
            out.close();
            socket.close();
            changeNumClients(-1);

        } catch (NoSuchMethodException nsme) {
            System.out.println("Invalid method requested: " + request.type);
            out.println("Error");
        } catch (IllegalAccessException iae) {
            System.out.println("Illegal access exception: " + request.type);
            out.println("Error");
        } catch (InvocationTargetException ite) {
            System.out.println("Invocation target exception: " + request.type);
            out.println("Error");
        } catch (InterruptedIOException ie) {
            System.out.println("Timed out");
        }
    }

    // Used to run threads that detect when operations time out (needed in order to pass parameters)
    public class MyRunnable implements Runnable {
        private final Thread thread;
        private final Socket socket;
        private final BufferedReader in;
        private final PrintWriter out;
        private final String id;
        private final long timeout;
        public MyRunnable(Thread thread, Socket socket, BufferedReader in, PrintWriter out, String id, long timeout) {
            this.thread = thread;
            this.socket = socket;
            this.in = in;
            this.out = out;
            this.id = id;
            this.timeout = timeout;
        }
        public void run() {
            detectTimeout(thread, socket, in, out, id, timeout);
        }
    }

    private void detectTimeout(Thread mainThread, Socket socket, BufferedReader in, PrintWriter out, String id, long timeout) {
        try {
            long startTime = System.currentTimeMillis();
            timeout *= 1000;
            while (mainThread.isAlive()) {
                if (System.currentTimeMillis() - startTime > timeout) {
                    mainThread.interrupt();
                    Response response = new Response();
                    response.id = id;
                    response.status = "failed";
                    response.response = "Operation timed out";
                    out.println(new GsonBuilder().create().toJson(response));
                    out.close();
                    in.close();
                    socket.close();
                    changeNumClients(-1);
                    break;
                }
            }
        } catch (IOException ioe) {
            System.out.println("Something was already closed...");
        }
    }

    /**
     * Edit the global variable, numClients. Synchronized so that multiple requests do not overlap.
     *
     * @param amount The amount to change numClients by. Should be either 1 or -1.
     * @return Whether the new numClients value is greater than maxClients.
     */

    private synchronized boolean changeNumClients(int amount) {
        numClients += amount;
        boolean overflow = numClients > maxClients;
        System.out.println("Number of clients has " + ((amount > 0) ? "increased" : "decreased") + " to " + numClients + ". Overflow: " + overflow);
        return overflow;
    }
}
