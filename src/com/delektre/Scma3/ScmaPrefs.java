package com.delektre.Scma3;

import org.androidannotations.annotations.sharedpreferences.DefaultInt;
import org.androidannotations.annotations.sharedpreferences.DefaultString;
import org.androidannotations.annotations.sharedpreferences.SharedPref;

/**
 * Created by t2r on 28.4.2014.
 */

// @SharedPref(value=SharedPref.Scope.UNIQUE)

@SharedPref
public interface ScmaPrefs {

    // The field name will have default value "John"
    @DefaultString("John")
    String name();

    // led pulsewidths
    @DefaultInt(500)
    int pulsewidth_led_green();

    @DefaultInt(500)
    int pulsewidth_led_red();

    @DefaultInt(500)
    int pulsewidth_led_blue();

    @DefaultInt(500)
    int pulsewidth_led_yellow();

    @DefaultInt(500)
    int pulsewidth_led_nir();

    @DefaultInt(500)
    int pulsewidth_led_white();

    @DefaultInt(500)
    int pulsewidth_led_focus();

    @DefaultInt(100)
    int pwm_led_red();

    @DefaultInt(100)
    int pwm_led_green();

    @DefaultInt(100)
    int pwm_led_blue();

    @DefaultInt(100)
    int pwm_led_yellow();

    @DefaultInt(100)
    int pwm_led_nir();

    @DefaultInt(100)
    int pwm_led_white();

    @DefaultInt(1)
    int cameraToUse();

    // The field lastUpdated will have default value 0


    long lastUpdated();

}
