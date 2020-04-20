package com.CFF;

import com.google.gson.Gson;
import jssc.SerialPortList;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import processing.data.XML;
import processing.serial.Serial;

import javax.swing.*;
import java.io.FileReader;
import java.io.IOException;

public class View extends PApplet{

    //for serial port prompt

//    boolean setup = true;
//// toggle prompt for choosing serial port
//
//    int lf = 10;  // linefeed in ASCII
//    String myString = null; //raw serial data
//    float[] data = new float[2]; //serial data split in 2 numbers
//    Serial myPort;  // serial port
//
//    float margin, headerHeight, rectsHeight, barsHeight, graphHeight;
//
//    int rgb[][] = {
//            { 200, 55, 110 }, //0: left axis
//            {  20, 85, 140 }
//    }; //1: right axis
//    int bg = color(242);
//    int light = color(245, 195, 215); //light pressure
//    int dark = color(200, 55, 110); //heavy pressure
//    int header = color(255); //header text
//    int stroke = color(190);
//    int subtitleBG = color(20, 85, 140);
//    int subtitle = color(255);
//    int rectsFill = color(230, 245, 255);
//
//    int numColumns = 8; // number of sensor rectangles
//    int maxColumns = 40; // max number of sensor rectangles
//    int columnInc = 2; // sensitivity increment
//    float[][] rects = new float[maxColumns][4]; // coordinates for each rect
//
//
//    float[][] readings = new float[4][2000]; // all readings
//// 0: left readings
//// 1: right readings
//// 2: raw n value
//// 3: n-normalized
//
//    float[][] plot = new float[2][2000]; // scaled left-right readings
//
//    float time[] = new float[2000];
//    float startingLine = 0.95f; // how far across the screen the current data point is drawn
//
//    volatile int vIdx = 1300;
//    int idx = 1300;
//
//    volatile boolean vRunning = true;
//    boolean running = true;
//    boolean console = false;
//    boolean mousePause = false;
//    boolean mouseReset = false;
//    boolean mouseConsole = false;
//
//    int inc = 30; // arrow increment
//
//    PImage logo, pause, play, reset, settings;
//    PFont font, fontBold;
//    int headFontSize = 28; //header title
//    int subFontSize = 14; //
//
//    float mainWidth, frameWidth;
//
//    //also adjust default values in reset() function
//    float max = 45.0f;
//    float maxLeft = max;
//    float maxRight = max;
//
//    String delim = " ";
//
//
//
//    float[] redTrim = new float[2];  // Sensitivity box trim of the red signal
//    float[] blueTrim = new float[2]; // Sensitivity box trim of the blue signal
//    float[] sensitivity = new float[2]; // Pressure sensitivity
//    float[] nRange = new float[2];      // Position range
//
//    XML config;

    Object config;

    public Object loadConfig(String fileName){
        try{
            Gson gson = new Gson();
            Object config = gson.fromJson(new FileReader(fileName), Object.class);
            print(config);
            return config;
        }
        catch (IOException ioex){
            ioex.printStackTrace();
            return new Object();
        }
    }

    //you still need to have a secondary class extending PApplet to instantiate it, so that it overrides setup(), draw() and other event handlers.
    public void setup() {

        surface.setResizable(true); // allow the canvas to be resized
        // renders hq if retina display
//        config = loadXML("config.xml");

        config = loadConfig("config.json");
        XML redTrimChild = config.getChild("redTrim");
        redTrim[0] = redTrimChild.getFloat("min");
        redTrim[1] = redTrimChild.getFloat("max");

        XML blueTrimChild = config.getChild("blueTrim");
        blueTrim[0] = blueTrimChild.getFloat("min");
        blueTrim[1] = blueTrimChild.getFloat("max");

        XML numColumnsChild = config.getChild("numColumns");
        numColumns = numColumnsChild.getInt("val");

        XML sensitivityChild = config.getChild("sensitivity");
        sensitivity[0] = sensitivityChild.getFloat("min");
        sensitivity[1] = sensitivityChild.getFloat("max");

        XML nRangeChild = config.getChild("nRange");
        nRange[0] = nRangeChild.getFloat("min");
        nRange[1] = nRangeChild.getFloat("max");

        background(bg);
        logo = loadImage("logo.png");
        pause = loadImage("pause.png");
        play = loadImage("play.png");
        reset = loadImage("reset.png");
        settings = loadImage("list.png");

        font = loadFont("Helvetica-16.vlw");
        fontBold = loadFont("Helvetica-Bold-30.vlw");

        //println((Object)Serial.list()); //print available serial ports to console

        try {
            Object selection;
            String port = "";
            int i = SerialPortList.getPortNames().length;

            if (i > 0) {
                //if there are multiple ports available, ask which one to use
                selection = JOptionPane.showInputDialog(frame, "Select serial port number to use:\n", "Setup", JOptionPane.PLAIN_MESSAGE, null, Serial.list(), Serial.list()[0]);
                if (selection == null) exit();

                println(selection);
                port = selection.toString();
                println(port);
                myPort = new Serial(this, port, 115200);

                myPort.clear(); // throw out the first reading, in case we started reading in the middle of a string from the sender
                myString = myPort.readStringUntil(lf);
                myString = null;
                xScale();
                yScale();
                //println(time);
            } else {
                JOptionPane.showMessageDialog(frame, "Device is not connected to the PC");
                exit();
            }
        }
        catch (Exception e)
        { //Print the type of error
            JOptionPane.showMessageDialog(frame, "COM port is not available (may\nbe in use by another program)");
            println("Error:", e);
            exit();
        }
    }//end setup



    public void draw() {
        //since window is resizable, the height of each element is based on
        //a fraction of the height of the window (using the 'margin' as a base unit).
        margin = height / 26.0f;
        headerHeight = margin * 4.0f;
        rectsHeight = margin * 5.0f;
        barsHeight = margin * 5.0f;
        graphHeight = margin * 5.0f;

        //frameWidth is the width of the window containing the main program elements
        //when the console is on, the program takes up less of the window so the console can show on the side
        if (console) frameWidth = width * 0.7f;
        else frameWidth = width;
        mainWidth = frameWidth - margin * 2.0f;


        while (myPort.available() > 0) {
            myString = myPort.readStringUntil(lf);
        }
        if (myString != null) {
            data = PApplet.parseFloat(split(myString, delim)); //split raw data into 2 numbers
            println(data);

            maxLeft = (data[0] > maxLeft)? data[0] : maxLeft;
            maxRight = (data[1] > maxRight)? data[1] : maxRight;
            max = (maxLeft > maxRight)? maxLeft : maxRight;

            //update max reading values as you go

            if (running) {
                readings[0][readings[0].length-1] = (-1) * data[0];
                readings[1][readings[1].length-1] = (-1) * data[1];

                // Map the element so it fits in our graph
                plot[0][plot[0].length-1] = map(readings[0][readings[0].length-1], 0, max, 0, graphHeight);
                plot[1][plot[1].length-1] = map(readings[1][readings[1].length-1], 0, max, 0, graphHeight);


                for (int j = 0; j < 2; j++) {
                    for (int i = idx; i < time.length-1; i++) {
                        readings[j][i] = readings[j][i+1]; // Shift the readings to the left so can put the newest reading in
                        plot[j][i] = plot[j][i+1];
                    }
                }
            }//end if(running)
        }//end if(myString != null)

        background(bg);

        if (width < 540 || height < 400) {
            headFontSize = 14;
            subFontSize = 10;
        } else {
            headFontSize = 28;
            subFontSize = 14;
        }

        //draw header
        noStroke();
        fill(header);
        rect(0, 0, width, headerHeight);
        textFont(fontBold, headFontSize);
        fill(10, 40, 75);
        text("Fabric Touch Sensor Visualizer", margin, headerHeight/2.0f + (headFontSize/2.0f));
        fill(stroke);
        rect(0, headerHeight, width, 2);
        if (width > 650 && height > 400) image(logo, width-200-margin, headerHeight/2.0f - 28, 200, 56);

        touchLocation();
        sensitivityBars();
        sensitivityGraph();
        if (console) console();
        drawButtons();
    }//end draw

    public void xScale()
    {
        for (int i = idx; i < time.length; i++)
        {
            time[i] = map(i, idx, time.length-1, margin, startingLine*mainWidth);
        }
    }//end xScale

    public void yScale()
    {
        for (int i = 0; i < time.length; i++)
        {
            plot[0][i] = headerHeight + rectsHeight + barsHeight + graphHeight + margin * 4.75f + map(readings[0][i], 0, max, 0, graphHeight-margin/4.0f);
            plot[1][i] = headerHeight + rectsHeight + barsHeight + graphHeight + margin * 4.75f + map(readings[1][i], 0, max, 0, graphHeight-margin/4.0f);
        }
    }//end yScale





    public void sensitivityGraph() {
        idx = vIdx; // Set the index equal to the volatile index to prevent overwriting during the loop
        running = vRunning; // Set the running flag equal to the volatile running flag
        xScale();
        yScale();
        //xScale and yScale remap the readings in case the window size has changed

        //draw graph container
        float offset = headerHeight + rectsHeight + barsHeight + margin * 4.75f;
        strokeWeight(1);
        stroke(stroke);
        fill(255);
        rect(margin, offset, mainWidth, graphHeight);

        // plot all the points
        for (int j = 0; j < 2; j++)
        {
            for (int i = idx; i < time.length - 1; i++)
            {
                stroke(rgb[j][0], rgb[j][1], rgb[j][2], map(i, idx, time.length, 0, 255));
                strokeWeight(2);
                line(time[i], plot[j][i], time[i+1], plot[j][i+1]);
            }
            fill(rgb[j][0], rgb[j][1], rgb[j][2]);
            ellipse(time[time.length-1], plot[j][plot[j].length-1], 5, 5);
        }

        //draw axis
        fill(0);
        text(0, margin*2.5f, offset+graphHeight-margin/2.0f);
        text(round(max), margin*2.5f, offset+margin);

        arrow(margin*2.0f, offset+graphHeight-margin/2.0f, margin*2.0f, offset+margin*0.75f);

        textAlign(LEFT);
        strokeWeight(1);
    }//end sensitivity graph


    public void drawButtons()
    {
        if (mouseY > (height-margin*1.6f) && mouseY < (height-margin*0.6f)) {
            if (mouseX < (frameWidth - margin *4.2f ) && mouseX > (frameWidth - margin * 5.2f))
            {
                mousePause = true;
                mouseReset = false;
                mouseConsole = false;
            } else if (mouseX < (frameWidth - margin * 2.7f) && mouseX > (frameWidth - margin * 3.7f))
            {
                mousePause = false;
                mouseReset = true;
                mouseConsole = false;
            } else if (mouseX < frameWidth-margin && mouseX > (frameWidth - margin * 2.2f))
            {
                mousePause = false;
                mouseReset = false;
                mouseConsole = true;
            } else
            {
                mousePause = false;
                mouseReset = false;
                mouseConsole = false;
            }
        }
        tint(255, 190);
        if (mousePause) tint(255, 100);
        if (running) image(pause, frameWidth-margin*5.2f, height-margin*1.6f, margin, margin);
        else image(play, frameWidth-margin*5.2f, height-margin*1.6f, margin, margin);

        tint(255, 190);
        if (mouseReset) tint(255, 100);
        image(reset, frameWidth-margin*3.7f, height-margin*1.6f, margin, margin);

        tint(255, 190);
        //console button remains grey if console is open
        //switches color on hover
        if (console ^ mouseConsole) tint(255, 100);
        image(settings, frameWidth-margin*2.2f, height-margin*1.6f, margin, margin);

        noTint();
    }

    public void keyPressed()
    {
        if (key == CODED)
        {
            if (keyCode == LEFT) {
                vIdx += inc;
                if (vIdx > time.length - inc)
                {
                    vIdx = time.length - inc;
                }
            } else if (keyCode == RIGHT) {
                vIdx -= inc;
                if (vIdx < inc)
                {
                    vIdx = inc;
                }
            } else if (keyCode == UP) {
                numColumns += columnInc;
                if (numColumns > maxColumns) {
                    numColumns = maxColumns;
                }
            } else if (keyCode == DOWN) {
                numColumns -= columnInc;
                if (numColumns < columnInc) {
                    numColumns = columnInc;
                }
            }
        } else if (key == ' ')
        {
            vRunning = !vRunning;
        } else if (key == 'r')
        {
            reset();
        } else if (key == 'd')
        {
            console = !console;
        }
    }


    public void mousePressed()
    {
        if (mousePause) vRunning = !vRunning;
        else if (mouseReset) reset();
        else if (mouseConsole) console = !console;
    }

    public void reset()
    {
        max = 45.0f;
        maxLeft = 45.0f;
        maxRight = 45.0f;

        nMin = 1.0f;
        nMax = 1.0f;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < time.length; j++)
            {
                plot[i][j] = 0;
                readings[i][j] = 0;
            }
        }

        xScale();
        yScale();
    }

}
