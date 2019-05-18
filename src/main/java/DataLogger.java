import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DataLogger {
    PrintWriter writer;
    boolean running = true;
    boolean isClosed = false;
    public DataLogger(){
        try {
            Files.delete(Paths.get("/home/pi/dronewebsite/lastrun.txt"));
        }catch(IOException e){
            System.out.println("Error deleting previous run");
        }

        try{
            writer = new PrintWriter(new BufferedWriter(new FileWriter(new File("/home/pi/dronewebsite/lastrun.txt"))));
        }catch(IOException e){
            System.out.println("Cannot create writer");
        }

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Writing logs");
                while(running && !isClosed){
                    log(Variables.angles[0],Variables.angles[1],(int)Variables.speed1,(int)Variables.speed2,(int)Variables.speed3,(int)Variables.speed3,Variables.lat,Variables.lon);
                    try {
                        Thread.sleep(50);
                    }catch(InterruptedException e){}
                }
                System.out.println("Logs ended");
            }
        });
        t.start();
    }

    public void log(double anglex, double angley, int speed1, int speed2, int speed3, int speed4,String lat, String lon){
        writer.println(anglex+" "+angley+" "+speed1+" "+speed2+" "+speed3+" "+speed4+" "+lat+" "+lon);
    }

    public void close(){
        isClosed = true;
        writer.close();
        try{
            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss");
            Date date = new Date();
            String curDate = dateFormat.format(date);

            Files.copy(Paths.get("/home/pi/dronewebsite/lastrun.txt"),Paths.get("/home/pi/dronewebsite/runs/"+curDate+".txt"));
        }catch(IOException e){
            System.out.println("Cannot move last run");
        }
    }

}
