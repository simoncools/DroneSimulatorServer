import javax.xml.bind.SchemaOutputResolver;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;

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
                boolean doReplay = Variables.replay;
                if(Variables.controllerConnected && doReplay==false) {
                    double newY = 0.7 * Variables.angles[0] + 0.7 * Variables.angles[1];
                    double newX = -0.7 * Variables.angles[0] + 0.7 * Variables.angles[1];
                    String webMessage = newX + " " + newY + " " + Variables.speed1 + " " + Variables.speed2 + " " + Variables.speed3 + " " + Variables.speed4 + " "+Variables.lat+" "+Variables.lon;
                    out.println(webMessage);
                }

                if(doReplay == true){
                    System.out.println("Starting replay");
                    FileInputStream fstream = new FileInputStream("/home/pi/dronewebsite/lastrun.txt");
                    BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                    String strLine;
                    while((strLine = br.readLine())!=null){
                        String[] parts = strLine.split(" ");
                        double anglex = Double.parseDouble(parts[0]);
                        double angley = Double.parseDouble(parts[1]);
                        int speed1 = Integer.parseInt(parts[2]);
                        int speed2 = Integer.parseInt(parts[3]);
                        int speed3 = Integer.parseInt(parts[4]);
                        int speed4 = Integer.parseInt(parts[5]);
                        String lat = parts[6];
                        String lon = parts[7];

                        double x = 0.7*anglex + 0.7*angley;
                        double y = -0.7*anglex + 0.7*angley;
                        String webMessage = x+" "+y+" "+speed1+" "+speed2+" "+speed3+" "+speed4+" "+lat+" "+lon;
                        out.println(webMessage);
                        try{
                            Thread.sleep(50);
                        }catch(InterruptedException e){}
                    }
                    System.out.println("Replay finished");
                    Variables.replay = false;
                    fstream.close();
                }
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

