import com.pi4j.io.i2c.I2CFactory;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static jdk.nashorn.internal.objects.NativeArray.sort;

public class Main {
    static double p;
    static double integrate;
    static double d;

    static int websitePort = 1337;



    public static void main(String argv[]){
        motorPID();
        startServer(argv);
    }

    public static void startServer(String argv[]){
        Server myServer;
        if (argv.length < 4) {
            System.out.println("Please specify a port number and PID constants");
        } else if(argv.length > 4) {
            System.out.println("Too many arguments.");
        }else{
            p = Double.parseDouble(argv[1]);
            integrate = Double.parseDouble(argv[2]);
            d = Double.parseDouble(argv[3]);
            myServer = new Server(Integer.parseInt(argv[0]));
            Thread t = new Thread(()-> {
                while(!myServer.isConnected()) {
                    myServer.listen();
                }
            });
            t.start();
            websiteSocket myWebsite;
            myWebsite = new websiteSocket(websitePort);
            Thread tWeb = new Thread(()-> {
                while(!myWebsite.isConnected()) {
                    myWebsite.listen();
                }
            });
            tWeb.start();

        }
    }

    public static void motorPID(){
        Thread t = new Thread(()->{
            Rolling rolling_acc_x = new Rolling(10);
            Rolling rolling_acc_y = new Rolling(10);
            Rolling rolling_acc_z = new Rolling(10);

            Rolling rolling_gyro_x = new Rolling(10);
            Rolling rolling_gyro_y = new Rolling(10);

            try {
                Thread.sleep(10);
            }catch(InterruptedException e){
                e.printStackTrace();
            }


            double pid_p_x = 0;
            double pid_i_x = 0;
            double pid_d_x = 0;
            double pid_p_y = 0;
            double pid_i_y = 0;
            double pid_d_y = 0;

            double kp_x = p; //PID constants X
            double ki_x = integrate;
            double kd_x = d;

            double kp_y = p; //PID constants Y
            double ki_y = integrate;
            double kd_y = d;

            double previous_error_x = 0;
            double previous_error_y = 0;
            double PID_x = 0;
            double PID_y = 0;

            double throttleX = 500;
            double throttleY = 500;
            double pidMax = 800;
            double pidMin = 200;

            double pwm_x_right = 0;
            double pwm_x_left = 0;
            double pwm_y_right = 0;
            double pwm_y_left = 0;

            double timePrev;
            double elapsedTime;
            double[] angles = new double[2];

            try {
                Mpu6050Controller.initialize();
            }catch(IOException a){
                a.printStackTrace();
            }catch(InterruptedException b){
                b.printStackTrace();
            }catch(I2CFactory.UnsupportedBusNumberException c){
                c.printStackTrace();
            }

            double time;
            double acc_angle_error_x = 0;
            double acc_angle_error_y = 0;

            double gyro_raw_error_x=0;
            double gyro_raw_error_y=0;
            double counter = 0;
            for(int i=0;i<200;i++){ //First 200 measurements are discarded for callibration
                int[] data = readSensor();
            }
            for(int i=0;i<300;i++) {
                int[] data = readSensor();
                if (data != null) {
                    int acc_x = data[3];
                    int acc_y = data[4];
                    int acc_z = data[5];
                    int gyro_x = data[0];
                    int gyro_y = data[1];

                    acc_angle_error_x += Math.toDegrees(Math.atan((acc_y /16384.0) / Math.sqrt(Math.pow((acc_x / 16384.0), 2) + Math.pow((acc_z / 16384.0), 2))));
                    acc_angle_error_y += Math.toDegrees(Math.atan(-1 * (acc_x /16384.0) / Math.sqrt(Math.pow((acc_y / 16384.0), 2) + Math.pow((acc_z / 16384.0), 2))));

                    gyro_raw_error_x += gyro_x / 16.4;
                    gyro_raw_error_y += gyro_y / 16.4;
                    // System.out.println("X :"+angles[0]+" Y :"+angles[1]);
                    counter++;
                } else {
                    System.out.println("Error reading gyroscope");
                }

            }
            Variables.gyro_raw_error_x = gyro_raw_error_x/counter;
            Variables.gyro_raw_error_y = gyro_raw_error_y/counter;
            Variables.acc_angle_error_x = acc_angle_error_x/counter;
            Variables.acc_angle_error_y = acc_angle_error_y/counter;

            time = System.nanoTime();
            Spi spi = new Spi();
            int cycles = 0;
            while(true) {
                int[] data = readSensor();
                timePrev = time;
                time = System.nanoTime();
                elapsedTime = (time-timePrev)/1000;
                elapsedTime = elapsedTime/1000;
                elapsedTime = elapsedTime/1000;
                if(data != null) {
                    int acc_x = data[3];
                    int acc_y = data[4];
                    int acc_z = data[5];
                    int gyro_x = data[0];
                    int gyro_y = data[1];

                    rolling_acc_x.add(acc_x);
                    rolling_acc_y.add(acc_y);
                    rolling_acc_z.add(acc_z);

                    rolling_gyro_x.add(gyro_x);
                    rolling_gyro_y.add(gyro_y);

                  /*  gyro_x = (int)Math.round(rolling_gyro_x.getAvg());
                    gyro_y = (int)Math.round(rolling_gyro_y.getAvg());

                    acc_x = (int)Math.round(rolling_acc_x.getAvg());
                    acc_y = (int)Math.round(rolling_acc_y.getAvg());
                    acc_z = (int)Math.round(rolling_acc_z.getAvg());*/

                    double acc_angleX = Math.toDegrees(Math.atan((acc_y /16384.0) / Math.sqrt(Math.pow((acc_x / 16384.0), 2) + Math.pow((acc_z / 16384.0), 2))))-Variables.acc_angle_error_x;
                    double acc_angleY = Math.toDegrees(Math.atan(-1 * (acc_x /16384.0) / Math.sqrt(Math.pow((acc_y / 16384.0), 2) + Math.pow((acc_z / 16384.0), 2))))-Variables.acc_angle_error_y;
                    double gyro_angleX = (gyro_x / 16.4) - Variables.gyro_raw_error_x;
                    double gyro_angleY = (gyro_y / 16.4)- Variables.gyro_raw_error_y;

                    angles[0] = (0.98 * (angles[0] + (gyro_angleX * elapsedTime))) + (0.02 * acc_angleX);
                    angles[1] = (0.98 * (angles[1] + (gyro_angleY * elapsedTime))) + (0.02 * acc_angleY);

                    Variables.angles[0] = angles[0];
                    Variables.angles[1] = angles[1];
                    // System.out.println("X :"+angles[0]+" Y :"+angles[1]);
                }else{
                    // System.out.println("Error reading gyroscope");
                }
                Variables.elapsedTime = elapsedTime;
                /////////////////////////PID
                double desiredAngleX = (double)Variables.x2/3;
                double desiredAngleY = (double)Variables.y2/3;
                double errorX = Variables.angles[0] - desiredAngleX;
                double errorY = Variables.angles[1] - desiredAngleY;

                pid_p_x = kp_x * errorX;
                pid_p_y = kp_y * errorY;

                //if (errorX > -10 && errorX < 10) {
                pid_i_x += ki_x*errorX;
                //}
                //if (errorY > -10 && errorY < 10) {
                pid_i_y += ki_y*errorY;
                //}

                //if(errorX <= -5 || errorX >= 5){
                pid_d_x = kd_x * ((errorX - previous_error_x) / elapsedTime);
                //}else {
                //    pid_d_x = 0;
                //}

                //if(errorY <= -5 || errorY >= 5){
                pid_d_y = kd_y * ((errorY - previous_error_y) / elapsedTime);
                //}else {
                //    pid_d_y = 0;
                //}

                PID_x = pid_d_x+pid_i_x+pid_p_x;
                PID_y = pid_d_y+pid_i_y+pid_p_y;

                if (PID_x <-(pidMax-pidMin)){
                    PID_x = -(pidMax-pidMin);
                }
                if (PID_x >(pidMax-pidMin)){
                    PID_x = (pidMax-pidMin);
                }

                if (PID_y <-(pidMax-pidMin)){
                    PID_y = -(pidMax-pidMin);
                }
                if (PID_y >(pidMax-pidMin)){
                    PID_y = (pidMax-pidMin);
                }

                pwm_x_left = throttleX+PID_x;
                pwm_x_right = throttleX-PID_x;

                pwm_y_left = throttleY+PID_y;
                pwm_y_right = throttleY-PID_y;

                if(pwm_x_right < pidMin)
                {
                    pwm_x_right = pidMin;
                }
                if(pwm_x_right > pidMax)
                {
                    pwm_x_right = pidMax;
                }

                if(pwm_x_left < pidMin)
                {
                    pwm_x_left = pidMin;
                }
                if(pwm_x_left > pidMax)
                {
                    pwm_x_left = pidMax;
                }

                if(pwm_y_right < pidMin)
                {
                    pwm_y_right = pidMin;
                }
                if(pwm_y_right > pidMax)
                {
                    pwm_y_right = pidMax;
                }

                if(pwm_y_left < pidMin)
                {
                    pwm_y_left = pidMin;
                }
                if(pwm_y_left > pidMax)
                {
                    pwm_y_left = pidMax;
                }


                //test 1
                /*
                pwm_y_left = 500+(Variables.x2*5);
                pwm_x_left = 500+(Variables.x2*5);
                pwm_y_right = 500+(Variables.x2*5);
                pwm_x_right = 500+(Variables.x2*5);
                System.out.println(Variables.x2);
                */


                SpiRunnable runnable = new SpiRunnable(spi,(int)pwm_x_right,(int)pwm_x_left,(int)pwm_y_right,(int)pwm_y_left);
                Thread thread = new Thread(runnable);
                thread.start();
                Variables.speed1 = (pwm_x_right/pidMax)*100;
                Variables.speed2 = (pwm_x_left/pidMax)*100;
                Variables.speed3 = (pwm_y_right/pidMax)*100;
                Variables.speed4 = (pwm_y_left/pidMax)*100;
                  /*  System.out.println("X Right :" + pwm_x_right);
                    System.out.println("X Left :" + pwm_x_left);
                    System.out.println("Y Right :" + pwm_y_right);
                    System.out.println("Y Left :" + pwm_y_left);
                    System.out.println("ErrorX :" + errorX);
                    System.out.println("ErrorY :" + errorY);
                    System.out.println("Elapsed :" + Variables.elapsedTime);
                    System.out.println("-----------------------");
                    System.out.println("");*/
                cycles = 0;
                previous_error_x = errorX;
                previous_error_y = errorY;

                try{
                    Thread.sleep(5);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }


    public static int[] readSensor() {
        int gx;
        int gy;
        int gz;
        int ax;
        int ay;
        int az;
        try {
            byte axh;
            byte axl;
            axh = Mpu6050Controller.readRegister(Mpu6050Registers.MPU6050_RA_ACCEL_XOUT_H);
            axl = Mpu6050Controller.readRegister(Mpu6050Registers.MPU6050_RA_ACCEL_XOUT_L);

            byte ayh;
            byte ayl;
            ayh = Mpu6050Controller.readRegister(Mpu6050Registers.MPU6050_RA_ACCEL_YOUT_H);
            ayl = Mpu6050Controller.readRegister(Mpu6050Registers.MPU6050_RA_ACCEL_YOUT_L);

            byte azh = 0;
            byte azl = 0;
            azh = Mpu6050Controller.readRegister(Mpu6050Registers.MPU6050_RA_ACCEL_ZOUT_H);
            azl = Mpu6050Controller.readRegister(Mpu6050Registers.MPU6050_RA_ACCEL_ZOUT_L);

            byte gxh;
            byte gxl;
            gxh = Mpu6050Controller.readRegister(Mpu6050Registers.MPU6050_RA_GYRO_XOUT_H);
            gxl = Mpu6050Controller.readRegister(Mpu6050Registers.MPU6050_RA_GYRO_XOUT_L);

            byte gyh;
            byte gyl;
            gyh = Mpu6050Controller.readRegister(Mpu6050Registers.MPU6050_RA_GYRO_YOUT_H);
            gyl = Mpu6050Controller.readRegister(Mpu6050Registers.MPU6050_RA_GYRO_YOUT_L);

            byte gzh=0;
            byte gzl=0;
          /*  gzh = Mpu6050Controller.readRegister(Mpu6050Registers.MPU6050_RA_GYRO_ZOUT_H);
            gzl = Mpu6050Controller.readRegister(Mpu6050Registers.MPU6050_RA_GYRO_ZOUT_L);*/

            gx = byteToInt(gxl, gxh);
            gy = byteToInt(gyl, gyh);
            gz = byteToInt(gzl, gzh);

            ax = byteToInt(axl, axh);
            ay = byteToInt(ayl, ayh);
            az = byteToInt(azl, azh);
            //System.out.println("Ax: " + ax + "  Ay: " + ay + "  Az: " + az + "  Gx: " + gx + "  Gx: " + gy + "  Gx: " + gz);
            int[] data = {gx,gy,gz,ax,ay,az};
            return data;
        } catch (IOException e) {
            // System.out.println("Error reading gyroscope");
            // e.printStackTrace();
        }
        return null;
    }

    public static int byteToInt(byte low, byte high) {
        int result;
        result = (high*256 + low);
        return result;
    }

}

