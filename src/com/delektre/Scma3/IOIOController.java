package com.delektre.Scma3;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.google.code.microlog4android.Logger;
import com.google.code.microlog4android.LoggerFactory;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

/**
 * Created by t2r on 14.4.2014.
 * <p/>
 * Implemntation and encapsulation of communication with IOIO Hardware
 */

public class IOIOController extends IOIOActivity {

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

    class Looper extends BaseIOIOLooper {
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
        public void setup() throws ConnectionLostException {
            logger.debug("IOIO.setup()");
            led_ = ioio_.openDigitalOutput(IOIO.LED_PIN, true);
            powout1_ = ioio_.openDigitalOutput(IOIO_PIN_BOARD1_UP, true);
            powout2_ = ioio_.openDigitalOutput(IOIO_PIN_BOARD2_UP, true);
            //input_ = ioio_.openAnalogInput(40);

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

            Message m = Message.obtain();
            m.what = MSG_IOIO_READY;
            _messagehandler.handleMessage(m);
        }


        @Override
        public void loop() throws ConnectionLostException, InterruptedException {

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
                            //Thread.sleep(50);
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
            Thread.sleep(10);
        }

        /*
         * Actions what we wish to do, when (if) IOIO is disconnected, eg. due to battery failure or something else
         * @see ioio.lib.util.BaseIOIOLooper#disconnected()
         */
        @Override
        public void disconnected() {
            logger.debug("IOIO.disconnected()");
            //enableUi(false);

            logger.error("Connection error. Connection to led controller (IOIO) was lost.");
            Message m = new Message();
            m.what = MSG_IOIO_FAILURE;
            _messagehandler.sendMessage(m);
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /*
     * Create the IOIO looper.
     * @see ioio.lib.util.android.IOIOActivity#createIOIOLooper()
     */
    @Override
    protected IOIOLooper createIOIOLooper() {
        logger.debug("createIOIOLooper()");
        return new Looper();
    }


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger.debug(this.getLocalClassName() + ".onCreate()");

        getSettings();

        mPulseWidth = new int[ledCount];
        mDefaultPulseWidth = new int[ledCount];
        for (int i = 0; i < ledCount; i++) {
            mDefaultPulseWidth[i] = defaultPulseWidth;
        }

        configureLeds();

    }

    private void getSettings() {
        String dump = "before";
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        try {
            //saveModeJPEG = sharedPref.getBoolean(KEY_PREF_SAVE_JPEG, true);
            //saveModeRAW = sharedPref.getBoolean(KEY_PREF_SAVE_RAW, false);
            dump = "green";
            mPulseWidth[LED_INDEX_GREEN] = Integer.parseInt(sharedPreferences.getString(KEY_PREF_GREEN_PULSEWIDTH, String.valueOf(mDefaultPulseWidth[LED_INDEX_GREEN])));
            dump = "blue";
            mPulseWidth[LED_INDEX_BLUE] = Integer.parseInt(sharedPreferences.getString(KEY_PREF_BLUE_PULSEWIDTH, String.valueOf(mDefaultPulseWidth[LED_INDEX_BLUE])));
            dump = "red";
            mPulseWidth[LED_INDEX_RED] = Integer.parseInt(sharedPreferences.getString(KEY_PREF_RED_PULSEWIDTH, String.valueOf(mDefaultPulseWidth[LED_INDEX_RED])));
            dump = "yellow";
            mPulseWidth[LED_INDEX_YELLOW] = Integer.parseInt(sharedPreferences.getString(KEY_PREF_YELLOW_PULSEWIDTH, String.valueOf(mDefaultPulseWidth[LED_INDEX_YELLOW])));
            dump = "white";
            mPulseWidth[LED_INDEX_WHITE] = Integer.parseInt(sharedPreferences.getString(KEY_PREF_WHITE_PULSEWIDTH, String.valueOf(mDefaultPulseWidth[LED_INDEX_WHITE])));
            dump = "nir";
            mPulseWidth[LED_INDEX_NIR] = Integer.parseInt(sharedPreferences.getString(KEY_PREF_NIR_PULSEWIDTH, String.valueOf(mDefaultPulseWidth[LED_INDEX_NIR])));

            dump = "focusColor";

            mFocusLedIndex = Integer.parseInt(sharedPreferences.getString(KEY_PREF_FOCUSCOLOR, "3"));

        } catch (ClassCastException cce) {
            Toast.makeText(getApplicationContext(), "Invalid setting for '" + dump + "': " + cce, Toast.LENGTH_LONG).show();
            logger.error("Invalid setting for '" + dump + "': " + cce);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Exeption: " + e, Toast.LENGTH_LONG).show();
            logger.error("Exeption: " + e);
        }

    }

    private void configureLeds() {
        try {
            System.arraycopy(mDefaultPulseWidth, 0, mPulseWidth, 0, ledCount);
        } catch (NullPointerException npe) {
            logger.error("Array copy error in configureLeds(): " + npe.getMessage());
        }
        //for (int i=0;i<ledCount;i++) {
        //    mPulseWidth[i] = mDefaultPulseWidth[i];
        //}
    }

    protected void onPause() {
        super.onPause();
        logger.debug(this.getLocalClassName() + ".onPause()");
    }

    protected void onResume() {
        super.onResume();
        logger.debug(this.getLocalClassName() + ".onResume()");
    }

    public Handler _messagehandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            logger.debug("new MessageHandler()");

            super.handleMessage(msg);

            switch (msg.what) {
                case MSG_IOIO_READY:
                    hasIOIO = true;
                    break;
                default:
            }
        }
    };
    public void setLedState(int index, boolean state) {

    }
}