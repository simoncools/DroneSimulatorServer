import com.pi4j.io.i2c.I2CFactory;
import java.io.IOException;

public class Main {
    static double p;
    static double i;
    static double d;
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
            i = Double.parseDouble(argv[2]);
            d = Double.parseDouble(argv[3]);
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
            double ki_x = i;
            double kd_x = d;

            double kp_y = p; //PID constants Y
            double ki_y = i;
            double kd_y = d;

            double previous_error_x = 0;
            double previous_error_y = 0;
            double PID_x = 0;
            double PID_y = 0;

            double throttleX = 800;
            double throttleY = 800;

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
            double acc_angle_error_x=0;
            double acc_angle_error_y=0;
            double gyro_raw_error_x=0;
            double gyro_raw_error_y=0;
            double counter = 0;
            for(int i=0;i<100;i++) {
                int[] data = readSensor();
                if (data != null) {
                    int acc_x = data[3];
                    int acc_y = data[4];
                    int acc_z = data[5];
                    int gyro_x = data[0];
                    int gyro_y = data[1];

                    acc_angle_error_x += Math.toDegrees(Math.atan((acc_y / 4096.0) / Math.sqrt(Math.pow((acc_x / 4096.0), 2) + Math.pow((acc_z / 4096.0), 2))));
                    acc_angle_error_y += Math.toDegrees(Math.atan(-1 * (acc_x / 4096.0) / Math.sqrt(Math.pow((acc_y / 4096.0), 2) + Math.pow((acc_z / 4096.0), 2))));
                    gyro_raw_error_x += gyro_x / 32.8;
                    gyro_raw_error_y += gyro_y / 32.8;
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

            time = System.currentTimeMillis();
            Spi spi = new Spi();
            int cycles = 0;
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

                    double acc_angleX = Math.toDegrees(Math.atan((acc_y /16384.0) / Math.sqrt(Math.pow((acc_x / 16384.0), 2) + Math.pow((acc_z / 16384.0), 2))))-Variables.acc_angle_error_x;
                    double acc_angleY = Math.toDegrees(Math.atan(-1 * (acc_x / 16384.0) / Math.sqrt(Math.pow((acc_y / 16384.0), 2) + Math.pow((acc_z / 16384.0), 2))))-Variables.acc_angle_error_y;
                    double gyro_angleX = (gyro_x / 131.0)-Variables.gyro_raw_error_x;
                    double gyro_angleY = (gyro_y / 131.0)-Variables.gyro_raw_error_y;

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
                double errorX = Variables.angles[0] - desiredAngleX;
                double errorY = Variables.angles[1] - desiredAngleY;

                pid_p_x = kp_x * errorX;
                pid_p_y = kp_y * errorY;

                if (errorX > -5 && errorX < 5) {
                    pid_i_x += ki_x*errorX;
                }
                if (errorY > -5 && errorY < 5) {
                    pid_i_y += ki_y*errorY;
                }

                pid_d_x = kd_x*((errorX-previous_error_x)/elapsedTime);
                pid_d_y = kd_y*((errorY-previous_error_y)/elapsedTime);

                PID_x = pid_d_x+pid_i_x+pid_p_x;
                PID_y = pid_d_y+pid_i_y+pid_p_y;

                if (PID_x <-400){
                    PID_x = -400;
                }
                if (PID_x >400){
                    PID_x = 400;
                }

                if (PID_y <-400){
                    PID_y = -400;
                }
                if (PID_y >400){
                    PID_y = 400;
                }

                pwm_x_left = throttleX+PID_x;
                pwm_x_right = throttleX-PID_x;

                pwm_y_left = throttleY+PID_y;
                pwm_y_right = throttleY-PID_y;

                if(pwm_x_right < 600)
                {
                    pwm_x_right = 600;
                }
                if(pwm_x_right > 1000)
                {
                    pwm_x_right = 1000;
                }

                if(pwm_x_left < 600)
                {
                    pwm_x_left = 600;
                }
                if(pwm_x_left > 1000)
                {
                    pwm_x_left = 1000;
                }

                if(pwm_y_right < 600)
                {
                    pwm_y_right = 600;
                }
                if(pwm_y_right > 1000)
                {
                    pwm_y_right = 1000;
                }

                if(pwm_y_left < 600)
                {
                    pwm_y_left = 600;
                }
                if(pwm_y_left > 1000)
                {
                    pwm_y_left = 1000;
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

                if(cycles >= 50) {
                    System.out.println("X Right :" + pwm_x_right);
                    System.out.println("X Left :" + pwm_x_left);
                    System.out.println("Y Right :" + pwm_y_right);
                    System.out.println("Y Left :" + pwm_y_left);
                    System.out.println("ErrorX :" + errorX);
                    System.out.println("ErrorY :" + errorY);
                    System.out.println("Elapsed :" + Variables.elapsedTime);
                    System.out.println("-----------------------");
                    System.out.println("");
                    cycles = 0;
                }else{
                    cycles++;
                }
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

            byte azh = 0;
            byte azl = 0;
            //azh = Mpu6050Controller.readRegister(Mpu6050Registers.MPU6050_RA_ACCEL_ZOUT_H);
            //azl = Mpu6050Controller.readRegister(Mpu6050Registers.MPU6050_RA_ACCEL_ZOUT_L);

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
            //gzh = Mpu6050Controller.readRegister(Mpu6050Registers.MPU6050_RA_GYRO_ZOUT_H);
            //gzl = Mpu6050Controller.readRegister(Mpu6050Registers.MPU6050_RA_GYRO_ZOUT_L);

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

