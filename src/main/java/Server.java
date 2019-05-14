import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

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
                if(data.contains("JX1")){
                    String myData = data.replace("JX1 ","").replace("JY1 ","");
                    String[] dataXY = myData.split(" ");
                    Variables.x1 = Integer.parseInt(dataXY[0]);
                    Variables.y1 = Integer.parseInt(dataXY[1]);
                }else if(data.contains("JX2")){
                    String myData = data.replace("JX2 ","").replace("JY2 ","");
                    String[] dataXY = myData.split(" ");
                    Variables.x2 = Integer.parseInt(dataXY[0]);
                    Variables.y2 = Integer.parseInt(dataXY[1]);
                    double newX = 0.7*Variables.x2 + 0.7*Variables.y2;
                    double newY = 0.7*-Variables.y2 + 0.7*Variables.x2;
                    if(newX>100) newX=100;
                    if(newX<-100) newX=-100;
                    if(newY>100) newY=100;
                    if(newY<-100) newY=-100;
                    Variables.x2 = (int)newX;
                    Variables.y2 = (int)newY;

                }
              //  System.out.println("X1 :"+Variables.x1+" Y1 :"+Variables.y1);
               // System.out.println("X2 :"+Variables.x2+" Y2 :"+Variables.y2);
                data = in.readLine();
            }
            Variables.x1 = 0;
            Variables.x2 = 0;
            Variables.y1 = 0;
            Variables.y2 = 0;
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
