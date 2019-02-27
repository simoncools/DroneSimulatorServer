import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.i2c.I2CFactory;
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
    Mpu6050Controller controller;

    Server(int port){
        this.port = port;
        try {
            serverSocket = new ServerSocket(port);
        }catch(IOException e){
            e.printStackTrace();
        }
        controller = new Mpu6050Controller();
        /*
        try {
            controller.initialize();
        }catch(IOException a){
            a.printStackTrace();
        }catch(InterruptedException b){
            b.printStackTrace();
        }catch(I2CFactory.UnsupportedBusNumberException c){
            c.printStackTrace();
        }*/
    }

    public void listen(){
        GpioController gpio = GpioFactory.getInstance();
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
        gpio.shutdown();
        isConnected = false;
    }

    public void stop() throws IOException{
        System.out.println("Server Stopped");
        in.close();
        out.close();
        clientSocket.close();
        serverSocket.close();
    }

    public void readSensor(){
        int gx;
        int gy;
        int gz;
        int ax;
        int ay;
        int az;
        try {
            byte axh;
            byte axl;
            axh = controller.readRegister(Mpu6050Registers.MPU6050_RA_ACCEL_XOUT_H);
            axl = controller.readRegister(Mpu6050Registers.MPU6050_RA_ACCEL_XOUT_L);

            byte ayh;
            byte ayl;
            ayh = controller.readRegister(Mpu6050Registers.MPU6050_RA_ACCEL_YOUT_H);
            ayl = controller.readRegister(Mpu6050Registers.MPU6050_RA_ACCEL_YOUT_L);

            byte azh;
            byte azl;
            azh = controller.readRegister(Mpu6050Registers.MPU6050_RA_ACCEL_ZOUT_H);
            azl = controller.readRegister(Mpu6050Registers.MPU6050_RA_ACCEL_ZOUT_L);

            byte gxh;
            byte gxl;
            gxh = controller.readRegister(Mpu6050Registers.MPU6050_RA_GYRO_XOUT_H);
            gxl = controller.readRegister(Mpu6050Registers.MPU6050_RA_GYRO_XOUT_L);

            byte gyh;
            byte gyl;
            gyh = controller.readRegister(Mpu6050Registers.MPU6050_RA_GYRO_YOUT_H);
            gyl = controller.readRegister(Mpu6050Registers.MPU6050_RA_GYRO_YOUT_L);

            byte gzh;
            byte gzl;
            gzh = controller.readRegister(Mpu6050Registers.MPU6050_RA_GYRO_ZOUT_H);
            gzl = controller.readRegister(Mpu6050Registers.MPU6050_RA_GYRO_ZOUT_L);

            gx = byteToInt(gxl,gxh);
            gy = byteToInt(gyl,gyh);
            gz = byteToInt(gzl,gzh);

            ax = byteToInt(axl,axh);
            ay = byteToInt(ayl,ayh);
            az = byteToInt(azl,azh);
            System.out.println("Ax: "+ax+"  Ay: "+ay+"  Az: "+az+"  Gx: "+gx+"  Gx: "+gy+"  Gx: "+gz);
        }catch(IOException e){
            System.out.println("Error reading gyroscope");
            e.printStackTrace();
        }


    }

    public int byteToInt(byte low, byte high) {
        int result;
        byte[] bytes = {high,low};
        result = ((bytes[0] << 8) & 0x0000ff00) | (bytes[1] & 0x000000ff);
        return result;
    }

    public void setPort(int port){
        this.port = port;
    }

    public boolean isConnected() {
        return isConnected;
    }
}
