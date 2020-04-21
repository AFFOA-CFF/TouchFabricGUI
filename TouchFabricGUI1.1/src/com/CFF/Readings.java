package com.CFF;

public class Readings {
    String reading;     //raw serial data
    float[] data = new float[2]; //serial data split in 2 numbers
    float[][] readings = new float[4][2000]; // all readings

    protected float leftReading;        //1.35      4 bytes
    protected float rightReading;       //21.35     4 bytes
    protected float raw_n_value;        //log diff of these two             log(left)-baseline  -  log(right) - baseline
    protected float n_normalized;       //  sigmoid(raw_n)

    public Readings(String _reading){
        this.reading = _reading;
    }

}
