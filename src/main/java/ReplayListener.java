import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ReplayListener {
    private int port;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isConnected = false;

    ReplayListener(int port){
        this.port = port;
        try {
            serverSocket = new ServerSocket(port);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void listen(){
        try {
            System.out.println("Replay listener started");
            clientSocket = serverSocket.accept();
            System.out.println("Replay client connected");
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String data = in.readLine();
            while (data != null) {
                System.out.println("Replay!");
                Variables.replay = true;
                data = in.readLine();
            }
            System.out.println("Client Disconnected");

            }catch(IOException ie){
            // ie.printStackTrace();
            System.out.println("Error while listening.");
           }
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
