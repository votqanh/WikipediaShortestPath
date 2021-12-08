package cpen221.mp3.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.*;

// Purely for testing purposes

public class WikiMediatorClient {

    private final PrintWriter out;
    private final BufferedReader in;

    private final ClientRequest request;

    public WikiMediatorClient(String ip, int port, ClientRequest request) throws IOException {
        this.request = request;
        Socket clientSocket = new Socket(ip, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public String sendRequest() throws IOException {
        Gson gson = new GsonBuilder().create();
        out.println(gson.toJson(request));
        return in.readLine();
    }
}
