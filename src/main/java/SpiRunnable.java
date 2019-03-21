public class SpiRunnable implements Runnable {
    private int pwm_x_r;
    private int pwm_x_l;
    private int pwm_y_r;
    private int pwm_y_l;
    private Spi spi;

    public SpiRunnable(Spi spi,int pwm_x_r,int pwm_x_l, int pwm_y_r, int pwm_y_l) {
        this.pwm_x_r = pwm_x_r;
        this.pwm_x_l = pwm_x_l;
        this.pwm_y_r = pwm_y_r;
        this.pwm_y_l = pwm_y_l;
        this.spi = spi;
    }

    public void run() {
        spi.sendSpi(pwm_x_r,Variables.motor_x_right);
        spi.sendSpi(pwm_x_l,Variables.motor_x_left);
        spi.sendSpi(pwm_y_r,Variables.motor_y_right);
        spi.sendSpi(pwm_y_l,Variables.motor_y_left);
    }
}