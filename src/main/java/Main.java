import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String argv[]) {

        Server myServer;
        if (argv.length < 1) {
            System.out.println("Please specify a port number.");
        } else if(argv.length > 1) {
            System.out.println("Too many arguments.");
        }else{
                myServer = new Server(Integer.parseInt(argv[0]));
                    Thread t = new Thread(()-> {
                            while(!myServer.isConnected()) {
                                myServer.listen();
                            }
                    });
                    t.start();

        }
    }

}
