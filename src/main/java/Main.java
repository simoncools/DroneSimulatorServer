import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.wiringpi.SoftPwm;
import java.io.IOException;

public class Main {
    public static void main(String argv[]){
        motorPID();
        startServer(argv);
    }

    public static void startServer(String argv[]){
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

    public static void motorPID(){
        Thread t = new Thread(()->{
        /*
            try {
                Thread.sleep(10);
            }catch(InterruptedException e){
                e.printStackTrace();
            }
         */

            double pid_p_x = 0;
            double pid_i_x = 0;
            double pid_d_x = 0;
            double pid_p_y = 0;
            double pid_i_y = 0;
            double pid_d_y = 0;

            double kp_x = 1; //PID constants X
            double ki_x = 0.01;
            double kd_x = 1;

            double kp_y = 10; //PID constants Y
            double ki_y = 0.01;
            double kd_y = 3;

            double previous_error_x = 0;
            double previous_error_y = 0;
            double PID_x = 0;
            double PID_y = 0;

            double throttleX = 750;
            double throttleY = 750;

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

            double time = System.currentTimeMillis();
            Spi spi = new Spi();
            while(true) {
                timePrev = time;
                time = System.currentTimeMillis();
                elapsedTime = (time-timePrev)/1000;
                int[] data = readSensor();
                if(data != null) {
                    int acc_x = data[3];
                    int acc_y = data[4];
                    int acc_z = data[5];
                    int gyro_x = data[0];
                    int gyro_y = data[1];

                    double acc_angleX = Math.toDegrees(Math.atan((acc_y / 16384.0) / Math.sqrt(Math.pow((acc_x / 16384.0), 2) + Math.pow((acc_z / 16384.0), 2))));
                    double acc_angleY = Math.toDegrees(Math.atan(-1 * (acc_x / 16384.0) / Math.sqrt(Math.pow((acc_y / 16384.0), 2) + Math.pow((acc_z / 16384.0), 2))));
                    double gyro_angleX = gyro_x / 131.0;
                    double gyro_angleY = gyro_y / 131.0;

                    angles[0] = (0.98 * (angles[0] + (gyro_angleX * elapsedTime))) + (0.02 * acc_angleX);
                    angles[1] = (0.98 * (angles[1] + (gyro_angleY * elapsedTime))) + (0.02 * acc_angleY);
                    Variables.angles = angles;
                    Variables.elapsedTime = elapsedTime;
                   // System.out.println("X :"+angles[0]+" Y :"+angles[1]);
                }else{
                   // System.out.println("Error reading gyroscope");
                }

                /////////////////////////PID
                double desiredAngleX = (double)Variables.x2 / 3;
                double desiredAngleY = (double)Variables.y2 / 3;
                double errorX = Variables.angles[0] - desiredAngleX + Variables.xoffset;
                double errorY = Variables.angles[1] - desiredAngleY + Variables.yoffset;

                pid_p_x = kp_x * errorX;
                pid_p_y = kp_y * errorY;

                if (errorX > -3 && errorX < 3) {
                   pid_i_x += ki_x*errorX;
                }
                if (errorY > -3 && errorY < 3) {
                    pid_i_y += ki_y*errorY;
                }

                pid_d_x = kd_x*((errorX-previous_error_x)/elapsedTime);
                pid_d_y = kd_y*((errorY-previous_error_y)/elapsedTime);

                PID_x = pid_d_x+pid_i_x+pid_p_x;
                PID_y = pid_d_y+pid_i_y+pid_p_y;

                if (PID_x <-500){
                    PID_x = -500;
                }
                if (PID_x >500){
                    PID_x = 500;
                }

                if (PID_y <-500){
                    PID_y = -500;
                }
                if (PID_y >500){
                    PID_y = 500;
                }

                pwm_x_left = throttleX+PID_x;
                pwm_x_right = throttleX-PID_x;

                pwm_y_left = throttleY+PID_y;
                pwm_y_right = throttleY-PID_y;

                if(pwm_x_right < 500)
                {
                    pwm_x_right = 500;
                }
                if(pwm_x_right > 1000)
                {
                    pwm_x_right = 1000;
                }

                if(pwm_x_left < 500)
                {
                    pwm_x_left = 500;
                }
                if(pwm_x_left > 1000)
                {
                    pwm_x_left = 1000;
                }

                if(pwm_y_right < 500)
                {
                    pwm_y_right = 500;
                }
                if(pwm_y_right > 1000)
                {
                    pwm_y_right = 1000;
                }

                if(pwm_y_left < 500)
                {
                    pwm_y_left = 500;
                }
                if(pwm_y_left > 1000)
                {
                    pwm_y_left = 1000;
                }

               /* ////////////////////////////////
                //SPI test code for motor controller
                //////////////////////////////////
                //test 1
                pwm_x_left=875;
                pwm_x_right=625;
                pwm_y_left=500;
                pwm_y_right=250;
                //test 2
                pwm_x_left=800 + Variables.x2;
                pwm_x_right=800 - Variables.x2;
                pwm_y_left=800 + Variables.y2;
                pwm_y_right=800 - Variables.y2;
                //test 3
                pwm_x_left=800 + Variables.x2 + Variables.y2;
                pwm_x_right=800 - Variables.x2 - Variables.y2;
                pwm_y_left=800 + Variables.x2 - Variables.y2 ;
                pwm_y_right=800 - Variables.x2 + Variables.y2;
                //
                pwm_y_left = 500+(Variables.x2*5);
                pwm_x_left = 500+(Variables.x2*5);
                pwm_y_right = 500+(Variables.x2*5);
                pwm_x_right = 500+(Variables.x2*5);
                //System.out.println(Variables.x2);
               */

                spi.sendSpi((int)pwm_x_right,Variables.motor_x_right);
                spi.sendSpi((int)pwm_x_left,Variables.motor_x_left);
                spi.sendSpi((int)pwm_y_right,Variables.motor_y_right);
                spi.sendSpi((int)pwm_y_left,Variables.motor_y_left);
              /*  System.out.println("X Right :"+pwm_x_right);
                System.out.println("X Left :"+pwm_x_left);
                System.out.println("Y Right :"+pwm_y_right);
                System.out.println("Y Left :"+pwm_y_left);
                System.out.println("ErrorX :"+errorX);
                System.out.println("ErrorY :"+errorY);
                System.out.println("Elapsed :"+Variables.elapsedTime);
                System.out.println("-----------------------");
                System.out.println("");*/

                previous_error_x = errorX;
                previous_error_y = errorY;
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

            byte azh;
            byte azl;
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

            byte gzh;
            byte gzl;
            gzh = Mpu6050Controller.readRegister(Mpu6050Registers.MPU6050_RA_GYRO_ZOUT_H);
            gzl = Mpu6050Controller.readRegister(Mpu6050Registers.MPU6050_RA_GYRO_ZOUT_L);

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
