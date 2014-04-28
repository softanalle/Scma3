package com.delektre.Scma3;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import com.google.code.microlog4android.Logger;
import com.google.code.microlog4android.LoggerFactory;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;
/*
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.sharedpreferences.Pref;
*/


/**
 * Created by t2r on 28.4.2014.
 */


public class IOIOPalvelu extends IOIOService {


    // ScmaPrefs_ scmaPrefs;

    public static final int MSG_IOIO_READY = 1;
    public static final int MSG_IOIO_FAILURE = 2;

    protected static final Logger logger = LoggerFactory.getLogger();

    // IOIO pin settings
    private static final int IOIO_PIN_BOARD1_UP = 6;
    private static final int IOIO_PIN_BOARD2_UP = 5;

    private static final int IOIO_PIN_LED_WHITE = 11;
    private static final int IOIO_PIN_LED_YELLOW = 10;
    private static final int IOIO_PIN_LED_NIR = 12;
    private static final int IOIO_PIN_LED_GREEN = 2;
    private static final int IOIO_PIN_LED_BLUE = 3;
    private static final int IOIO_PIN_LED_RED = 4;

    private boolean hasIOIO = false;
    private static int delayCounter = 0;


    private static final int LED_INDEX_GREEN = 0;
    private static final int LED_INDEX_BLUE = 1;
    private static final int LED_INDEX_RED = 2;
    private static final int LED_INDEX_WHITE = 3;
    private static final int LED_INDEX_YELLOW = 4;
    private static final int LED_INDEX_NIR = 5;

    private static final int LED_INDEX_FOCUS = 6;
    private static final int LED_INDEX_CALIBRATE = 7;

    public static final String KEY_PREF_FOCUSCOLOR = "conf_focusled_color";
    // public static final String KEY_PREF_DEFAULT_PULSEWIDTH = "conf_pwm_default";
    public static final String KEY_PREF_RED_PULSEWIDTH = "conf_red_pwm";
    public static final String KEY_PREF_GREEN_PULSEWIDTH = "conf_green_pwm";
    public static final String KEY_PREF_BLUE_PULSEWIDTH = "conf_blue_pwm";
    public static final String KEY_PREF_YELLOW_PULSEWIDTH = "conf_yellow_pwm";
    public static final String KEY_PREF_WHITE_PULSEWIDTH = "conf_white_pwm";
    public static final String KEY_PREF_NIR_PULSEWIDTH = "conf_nir_pwm";

    public static final String KEY_PREF_FOCUS_PULSEWIDTH = "conf_focus_pwm";

//      public static final String KEY_PREF_SAVE_JPEG = "conf_write_jpeg";
    //public static final String KEY_PREF_SAVE_RAW = "conf_write_raw";

    //public static final String KEY_PREF_PREVIEW_SCALE = "conf_preview_scale";

    public static final String KEY_PREF_LOGLEVEL = "conf_log_level";


    private boolean mShutdown = false;
    private boolean powerState_ = false;

    private boolean mFocusOn = false;
    private int mPulseWidth[];      // PWM pulse widths for the leds
    private boolean[] mLedState;    // led status (on/off)
    private int[] mDefaultPulseWidth; // default pulse width for leds

    private int mFocusCount = 0;

    private final int defaultPulseWidth = 500;
    private static int mFocusLedIndex = LED_INDEX_WHITE;
    private final int defaultFocusPulseWidth = 300;

    private static int ledCount = 6;


    // if we need to do soft reset to IOIO, set this boolean to true
    private boolean doIOIOreset = false;


    private final Object lock_ = new Object(); // do we need this?

    // private AnalogInput input_;
    private PwmOutput pwmOutput1_;  // board-1-1 = pin 10
    private PwmOutput pwmOutput2_;  // board-1-2 = pin 11
    private PwmOutput pwmOutput3_;  // board-1-3 = pin 12
    private PwmOutput pwmOutput4_;  // board-2-1 = pin 2
    private PwmOutput pwmOutput5_;  // board-2-2 = pin 3
    private PwmOutput pwmOutput6_;  // board-2-3 = pin 4

    private DigitalOutput led_;
    private DigitalOutput powout1_;
    private DigitalOutput powout2_;

    @Override
    protected IOIOLooper createIOIOLooper() {
        IOIO ioio_ = null;

        return new BaseIOIOLooper() {
            //private DigitalOutput led;

            @Override
            protected void setup() throws ConnectionLostException, InterruptedException {
                // led = ioio_.openDigitalOutput(IOIO.LED_PIN);

                logger.debug("IOIO.setup()");
                led_ = ioio_.openDigitalOutput(IOIO.LED_PIN, true);
                powout1_ = ioio_.openDigitalOutput(IOIO_PIN_BOARD1_UP, true);
                powout2_ = ioio_.openDigitalOutput(IOIO_PIN_BOARD2_UP, true);
                //input_ = ioio_.openAnalogInput(40);

                /*
                pwmOutput1_ = ioio_.openPwmOutput(IOIO_PIN_LED_GREEN, scmaPrefs.pwm_led_green().get());
                pwmOutput2_ = ioio_.openPwmOutput(IOIO_PIN_LED_BLUE, scmaPrefs.pwm_led_blue().get());
                pwmOutput3_ = ioio_.openPwmOutput(IOIO_PIN_LED_RED, scmaPrefs.pwm_led_red().get());

                pwmOutput4_ = ioio_.openPwmOutput(IOIO_PIN_LED_WHITE, scmaPrefs.pwm_led_white().get());
                pwmOutput5_ = ioio_.openPwmOutput(IOIO_PIN_LED_YELLOW, scmaPrefs.pwm_led_yellow().get());
                pwmOutput6_ = ioio_.openPwmOutput(IOIO_PIN_LED_NIR, scmaPrefs.pwm_led_nir().get());
*/

                pwmOutput1_ = ioio_.openPwmOutput(IOIO_PIN_LED_GREEN, 100);
                pwmOutput2_ = ioio_.openPwmOutput(IOIO_PIN_LED_BLUE, 100);
                pwmOutput3_ = ioio_.openPwmOutput(IOIO_PIN_LED_RED, 100);

                pwmOutput4_ = ioio_.openPwmOutput(IOIO_PIN_LED_WHITE, 100);
                pwmOutput5_ = ioio_.openPwmOutput(IOIO_PIN_LED_YELLOW, 100);
                pwmOutput6_ = ioio_.openPwmOutput(IOIO_PIN_LED_NIR, 100);


                powout1_.write(false);
                powout2_.write(false);

                pwmOutput1_.setPulseWidth(0);
                pwmOutput2_.setPulseWidth(0);
                pwmOutput3_.setPulseWidth(0);
                pwmOutput4_.setPulseWidth(0);
                pwmOutput5_.setPulseWidth(0);
                pwmOutput6_.setPulseWidth(0);

                logger.debug("Hardware: " +
                        ioio_.getImplVersion(IOIO.VersionType.HARDWARE_VER));
                logger.debug("Boot loader: " +
                        ioio_.getImplVersion(IOIO.VersionType.BOOTLOADER_VER));
                logger.debug("App firmware: " +
                        ioio_.getImplVersion(IOIO.VersionType.APP_FIRMWARE_VER));
                logger.debug("IOIO Lib: " +
                        ioio_.getImplVersion(IOIO.VersionType.IOIOLIB_VER));

                //mFocusCount = 0;
                //mFocusOn = false;

                //enableUi(true);
                logger.debug("IOIO.setup() done");

                /*
                Message m = Message.obtain();
                m.what = MSG_IOIO_READY;
                _messagehandler.handleMessage(m);
                */
                mPulseWidth = new int[ledCount];
                for (int i = 0; i < ledCount; i++) {
                    mPulseWidth[i] = 500;
                }
            }

            /**
             * @param delayMilliSeconds delay to sleep in milliseconds
             */

            void shortDelay(int delayMilliSeconds) {
                try {
                    Thread.sleep(delayMilliSeconds);
                } catch (InterruptedException ie) {
                    logger.debug(ie.getStackTrace());
                }
            }

            /*
                 * Actions what we wish to do, when (if) IOIO is disconnected, eg. due to battery failure or something else
                 * @see ioio.lib.util.BaseIOIOLooper#disconnected()
                 */
            @Override
            public void disconnected() {

                //Global.setIOIOStatus(false);
                //Global.setStatusUpdate("IOIO Disconnect");

                logger.debug("IOIO.disconnected()");


                logger.error("Connection error. Connection to led controller (IOIO) was lost.");
                //Message m = new Message();
                //m.what = MSG_IOIO_FAILURE;
                //_messagehandler.sendMessage(m);


                shortDelay(20);

            }

            @Override
            public void loop() throws ConnectionLostException, InterruptedException {
                long start = System.nanoTime();

                if (delayCounter > 0) {
                    delayCounter--;
                } else {
                    ioio_.beginBatch();
                    try {
                        synchronized (lock_) {
                            if (!mShutdown) {
                                powout1_.write(powerState_);
                                powout2_.write(powerState_);

                            }
                        }


                        led_.write(!powerState_);

                        //Thread.sleep(5);

                        synchronized (lock_) {
                            if (mFocusOn) {
                                mFocusCount++;
                                if (mFocusCount < 20) {
                                    mLedState[mFocusLedIndex] = true;
                                } else {
                                    mLedState[mFocusLedIndex] = false;
                                    mFocusOn = false;
                                    mFocusCount = 0;
                                }
                            }

                            pwmOutput1_.setPulseWidth(!mLedState[0] ? 0 : mPulseWidth[0]);
                            pwmOutput2_.setPulseWidth(!mLedState[1] ? 0 : mPulseWidth[1]);
                            pwmOutput3_.setPulseWidth(!mLedState[2] ? 0 : mPulseWidth[2]);
                            pwmOutput4_.setPulseWidth(!mLedState[3] ? 0 : mPulseWidth[3]);
                            pwmOutput5_.setPulseWidth(!mLedState[4] ? 0 : mPulseWidth[4]);
                            pwmOutput6_.setPulseWidth(!mLedState[5] ? 0 : mPulseWidth[5]);


                            if (mShutdown) {
                                // wait-time, required for making sure IOIO has turned power off from the (led)controller board
                                shortDelay(50);
                                delayCounter += 4;
                                powout1_.write(powerState_);
                                powout2_.write(powerState_);
                            }
                            led_.write(!powerState_);
                        }
                    } catch (Exception e) {
                        logger.error(e.toString());

                    } finally {
                        ioio_.endBatch();
                    }
                }

                        /*
                         *  if we need (for some reason) to do softreset to IOIO
                         */
                if (doIOIOreset)

                {
                    ioio_.softReset();
                    doIOIOreset = false;

                    logger.debug("IOIO reset completed");
                }

                //visualize(powerState_, mLedState);

                // sleep, ie. give other threads time to respond for default 10ms

                shortDelay(20);





            /*
            * Example code from google
            *
             */

/*
//Code is constantly running and sending commands to IOIO
                long start = System.nanoTime();

                // >>>>>>>>              //I'd probably put a flag here to indicate a good connection

//Sets Global Variable so I know how much time is left with the current cycle speed
                Global.setioioOverhead(sleepTime);
                long runtime;
                long period = 250;
                long delay = period * 1000000;

//Blink the Status LED so we know we're alive
                led.write(false);
                Thread.sleep(100);
                led.write(true);
//Altimeter Interface
/**Runs loop at a specified rate
 **sleepTime is the amount of extra time the loop has after it's completed it's actions before
 **it's required to run again. runtime is the amount of time it took to execute the loop.
 **/
                /*
                runtime = System.nanoTime() - start;
                if (runtime < 0) runtime = runtime + Long.MAX_VALUE;
                sleepTime = (int) (delay - runtime) / 1000000;
                Thread.sleep(sleepTime);
                */

            }
        }

                ;
    }

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        IOIOPalvelu getService() {
            return IOIOPalvelu.this;
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
        //return null;
    }

    public void setLedMode(int index, boolean mode) {
        mLedState[index] = mode;
    }
}


