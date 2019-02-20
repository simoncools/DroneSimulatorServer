import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Server {
    private int port;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isConnected = false;

    Server(int port){
        this.port = port;
        try {
            serverSocket = new ServerSocket(port);
            //serverSocket.setSoTimeout(5000);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void listen(){
        try {
            System.out.println("Listening for connections...");
            clientSocket = serverSocket.accept();
            isConnected = true;
            System.out.println("Client Connected");
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String data = in.readLine();
            while (data != null) {
                System.out.println("Message : " + data);
                data = in.readLine();
            }

            System.out.println("Client Disconnected");
        }catch(IOException ie){
           // ie.printStackTrace();
            System.out.println("Error while listening.");
            isConnected = false;
        }
        isConnected = false;
    }

    public void stop() throws IOException{
        System.out.println("Server Stopped");
        in.close();
        out.close();
        clientSocket.close();
        serverSocket.close();
    }

    public void setPort(int port){
        this.port = port;
    }

    public boolean isConnected() {
        return isConnected;
    }
}
