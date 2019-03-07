import com.pi4j.io.spi.SpiChannel;
import com.pi4j.io.spi.SpiDevice;
import com.pi4j.io.spi.SpiFactory;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.IOException;
import java.util.ArrayList;

public class Spi {
    SpiDevice spi;
    //ArrayList<SpiCommand> commandList = new ArrayList();

    public Spi(){
        try {
            spi = SpiFactory.getInstance(SpiChannel.CS0, 100000, SpiDevice.DEFAULT_SPI_MODE);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void sendSpi(int pwm, int motor){
        byte[] commands = new byte[2];
        if(motor == 1){
            commands[0] = (byte)((byte)0b00000000 | (byte)(pwm & 0x1F)); // Low byte
            commands[1] = (byte)((byte)0b10000000 | (byte)(pwm >>5)&0x1F); // high byte
        }
        if(motor == 2){
            commands[0] = (byte)((byte)0b00100000 | (byte)(pwm & 0x1F));
            commands[1] = (byte)((byte)0b10100000 | (byte)(pwm >>5)&0x1F);

        }
        if(motor == 3){
            commands[0] = (byte)((byte)0b01000000 | (byte)(pwm & 0x1F));
            commands[1] = (byte)((byte)0b11000000 | (byte)(pwm >>5)&0x1F);
        }
        if(motor == 4){
            commands[0] = (byte)((byte)0b01100000 | (byte)(pwm & 0x1F));
            commands[1] = (byte)((byte)0b11100000 | (byte)(pwm >>5)&0x1F);
        }
       /* String s1 = String.format("%8s", Integer.toBinaryString(commands[0] & 0xFF)).replace(' ', '0');
        String s2 = String.format("%8s", Integer.toBinaryString(commands[1] & 0xFF)).replace(' ', '0');
        String s3 = String.format("%8s", Integer.toBinaryString(commands[2] & 0xFF)).replace(' ', '0');
        System.out.println(s1); // 10000001
        System.out.println(s2); // 10000001
        System.out.println(s3); // 10000001*/
        byte data[] = {commands[0],commands[1]};
        try {
            byte[] result = spi.write(data[0]);
            Thread.sleep(1);
             result = spi.write(data[1]);
            Thread.sleep(1);
        }catch(IOException e){
            e.printStackTrace();
        }catch(InterruptedException e2){}

    }

    public void newThread(byte data[]){
        Thread thread = new Thread("SPIThread") {
            public void run(){
                try {
                    byte[] result = spi.write(data, 0, 8);
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }
}
