import javax.xml.bind.SchemaOutputResolver;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class websiteSocket {
    private int port;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isConnected = false;

    public websiteSocket(int port){
        this.port = port;
        try {
            serverSocket = new ServerSocket(port);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void listen(){
        try {
            System.out.println("Waiting for website connection...");
            clientSocket = serverSocket.accept();
            isConnected = true;
            System.out.println("Website Connected");
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            while (clientSocket.isConnected()) {
                double newY = 0.7*Variables.angles[0] + 0.7*Variables.angles[1];
                double newX = -0.7*Variables.angles[0] + 0.7*Variables.angles[1];
                String webMessage = newX+" "+newY+" "+Variables.speed1+" "+Variables.speed2+" "+Variables.speed3+" "+Variables.speed4;
                out.println(webMessage);
                try {
                    Thread.sleep(100);
                }catch(InterruptedException e){}
            }
            System.out.println("Website Disconnected");
        }catch(IOException ie){
            // ie.printStackTrace();
            System.out.println("(Website) Error while listening.");
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

