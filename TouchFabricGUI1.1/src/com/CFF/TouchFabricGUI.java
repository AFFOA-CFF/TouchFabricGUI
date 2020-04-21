package com.CFF;

import com.google.gson.Gson;
import processing.core.*;
import processing.serial.*;
import javax.swing.*;
import jssc.SerialPortList;
import processing.data.XML;

import java.io.FileReader;
import java.io.IOException;

public class TouchFabricGUI extends PApplet {//need to have a secondary class extending PApplet to instantiate it, so that it overrides setup(), draw() and other event handlers.

    boolean setup = true; // toggle prompt for choosing serial port

    int lf = 10;  // linefeed in ASCII
    String myString = null; //raw serial data
    float[] data = new float[2]; //serial data split in 2 numbers
    Serial myPort;  // serial port

    float margin, headerHeight, rectsHeight, barsHeight, graphHeight;

    int rgb[][] = {
            { 200, 55, 110 }, //0: left axis
            {  20, 85, 140 }
    }; //1: right axis
    int bg = color(242);
    int header = color(255); //header text
    int stroke = color(190);
    int subtitleBG = color(20, 85, 140);
    int subtitle = color(255);
    int rectsFill = color(230, 245, 255);

    int numColumns = 8; // number of sensor rectangles
    int maxColumns = 40; // max number of sensor rectangles
    int columnInc = 2; // sensitivity increment
    float[][] rects = new float[maxColumns][4]; // coordinates for each rect


    float[][] readings = new float[4][2000]; // all readings
// 0: left readings
// 1: right readings
// 2: raw n value
// 3: n-normalized

    float[][] plot = new float[2][2000]; // scaled left-right readings

    float time[] = new float[2000];
    float startingLine = 0.95f; // how far across the screen the current data point is drawn

    volatile int vIdx = 1300;
    int idx = 1300;

    volatile boolean vRunning = true;
    boolean running = true;
    boolean console = false;
    boolean mousePause = false;
    boolean mouseReset = false;
    boolean mouseConsole = false;

    int inc = 30; // arrow increment

    PImage logo, pause, play, reset, settings;
    PFont font, fontBold;
    int headFontSize = 28; //header title
    int subFontSize = 14; //

    float mainWidth, frameWidth;

    //also adjust default values in reset() function
    float max = 45.0f;
    float maxLeft = max;
    float maxRight = max;

    String delim = " ";

    XML config;

    float[] redTrim = new float[2];  // Sensitivity box trim of the red signal
    float[] blueTrim = new float[2]; // Sensitivity box trim of the blue signal
    float[] sensitivity = new float[2]; // Pressure sensitivity
    float[] nRange = new float[2];      // Position range

    /**Runs once at the beginning to set up stuff needed like logos, components and other vars
     * Extending PAPPlet (which we did) needs that we implement this function
     * */

    public void setup() {

        //size(displayWidth, displayHeight);

        surface.setResizable(true); // allow the canvas to be resized
        // renders hq if retina display
        config = loadXML("config.xml");

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

    /**Function doesn't get called currently.
     * Left in case for a future improvement if we port from XML to JSON
     * */
    public Object loadConfig(){
        try{
            Gson gson = new Gson();
            Object config = gson.fromJson(new FileReader("config.json"), Object.class);
            print(config);
            return config;
        }
        catch (IOException ioex){
            ioex.printStackTrace();
            return new Object();
        }
    }

    /**Once setup is done, draw runs continuously until the program is closed.
     * Like a while loop
     * You don't HAVE to use this, if you just want to draw once to the screen
     * */
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

    /**Creates/updates sensitivity bars
     * */
    public void sensitivityBars()
    {
        float offset = headerHeight + rectsHeight + margin * 3.25f;

        //  draw title
        fill(subtitleBG);
        noStroke();
        rect(margin, offset, mainWidth, margin);
        fill(subtitle);
        textFont(font, subFontSize);
        float textWidth = textWidth("SENSITIVITY MONITORS");
        text("SENSITIVITY MONITORS", frameWidth/2.0f - textWidth/2.0f, offset + margin * 0.75f);

        // draw container
        fill(255);
        stroke(stroke);
        strokeWeight(1);
        rect(margin, offset + margin*1.25f, mainWidth, barsHeight);

        float center = offset + margin * 2.25f; //top y-coord for rectangles to center vertically in container

        // scale raw data from 0 to edge of box
        float wLeft = map((-1)*readings[0][readings[0].length-1], 0, max, 0, (mainWidth/2.0f - margin/4.0f));
        float wRight = map((-1)*readings[1][readings[1].length-1], 0, max, 0, (mainWidth/2.0f - margin/4.0f));

        //draw bars for left and right
        noStroke();
        fill(rgb[0][0], rgb[0][1], rgb[0][2]);
        rect(frameWidth/2.0f - wLeft, center, wLeft, margin*2.5f);

        fill(rgb[1][0], rgb[1][1], rgb[1][2]);
        rect(frameWidth/2.0f, center, wRight, margin*2.5f);


        // draw sensitivity range boxes
        fill(255, 0, 0, 127);
        rect(frameWidth/2.0f-map(redTrim[1], 0, max, 0, (mainWidth/2.0f - margin/4.0f)), center-(margin*.1f), map(redTrim[1]-redTrim[0], 0, max, 0, (mainWidth/2.0f - margin/4.0f)), margin*2.7f);

        fill(0, 0, 255, 127);
        rect(frameWidth/2.0f+map(blueTrim[0], 0, max, 0, (mainWidth/2.0f - margin/4.0f)), center-(margin*.1f), map(blueTrim[1]-blueTrim[0], 0, max, 0, (mainWidth/2.0f - margin/4.0f)), margin*2.7f);




        //current value
        textFont(fontBold, subFontSize);
        textAlign(RIGHT);
        fill(rgb[0][0], rgb[0][1], rgb[0][2]);
        text(round((-1)*readings[0][readings[0].length-1]), frameWidth/2.0f - margin/4.0f, center - margin/4.0f);

        textAlign(LEFT);
        fill(rgb[1][0], rgb[1][1], rgb[1][2]);
        text(round((-1)*readings[1][readings[1].length-1]), frameWidth/2.0f+ margin/2.0f, center - margin/4.0f);

        //separation axis
        //  fill(10, 40, 75); //header color
        fill(0);
        rect(frameWidth/2.0f, center-margin*0.75f, margin/4.0f, margin * 4.5f);
        textFont(font, subFontSize);

        //axis labels (0 to max)
        textAlign(RIGHT);
        text(round(max), frameWidth - margin * 1.5f, offset + margin*1.25f + barsHeight - margin/2.0f);
        text(0, frameWidth/2.0f - margin/4.0f, offset + margin*1.25f + barsHeight - margin/2.0f);

        textAlign(LEFT);
        text(round(max), margin * 1.5f, offset + margin*1.25f + barsHeight - margin/2.0f);
        text(0, frameWidth/2.0f + margin/2.0f, offset + margin*1.25f + barsHeight - margin/2.0f);

        arrow(frameWidth/2.0f-margin, offset + barsHeight+margin/2.0f, margin*3.0f, offset + barsHeight + margin/2.0f);
        arrow(frameWidth/2.0f+margin, offset + barsHeight+margin/2.0f, frameWidth-margin*3.0f, offset + barsHeight + margin/2.0f);
    }//end sensitivity bars

    /**Creates arrows for scale in sensitivity graph
     * */
    public void arrow(float x1, float y1, float x2, float y2) {
        strokeWeight(1);
        stroke(0);
        line(x1, y1, x2, y2);
        pushMatrix();
        translate(x2, y2);
        float a = atan2(x1-x2, y2-y1);
        rotate(a);
        line(0, 0, -3, -3);
        line(0, 0, 3, -3);
        popMatrix();
    }

    /**Creates/updates sensitivity graph
     * */
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
    }

    /**Scales app for horizontal resize
     * */
    public void xScale()
    {
        for (int i = idx; i < time.length; i++)
        {
            time[i] = map(i, idx, time.length-1, margin, startingLine*mainWidth);
        }
    }

    /**Scales app for vertical resize
     * */
    public void yScale()
    {
        for (int i = 0; i < time.length; i++)
        {
            plot[0][i] = headerHeight + rectsHeight + barsHeight + graphHeight + margin * 4.75f + map(readings[0][i], 0, max, 0, graphHeight-margin/4.0f);
            plot[1][i] = headerHeight + rectsHeight + barsHeight + graphHeight + margin * 4.75f + map(readings[1][i], 0, max, 0, graphHeight-margin/4.0f);
        }
    }


    /**Detects/updates toch location
     * */
    public void touchLocation() {
        //draw title
        float offset = headerHeight + margin;
        fill(subtitleBG);
        noStroke();
        rect(margin, offset, mainWidth, margin);
        fill(subtitle);
        textFont(font, subFontSize);
        text("TOUCH LOCATION", frameWidth/2.0f - textWidth("TOUCH LOCATION")/2.0f, offset + margin *0.75f);

        //draw container
        fill(255);
        stroke(stroke);
        rect(margin, offset + margin*1.25f, mainWidth, rectsHeight);

        float pWidth = 0.7f; // percentage of the width of each segment
        float pHeight = 1.0f; //

        float x = margin + (mainWidth/(2.0f*numColumns))*(1.0f - pWidth); // x position
        float y = offset + margin*1.25f; // y position
        float w = pWidth*mainWidth/numColumns; // rectangle width
        float h = rectsHeight; // rectangle height

        stroke(stroke);
        strokeWeight(1);
        fill(rectsFill);
        //draw rectangles numbered numColumns to 0 (j) and store their coordinates in the rects[j][] array
        for (int j = numColumns-1; j >= 0; j--) {
            rect(x, y, w, h);
            rects[j][0] = x;
            rects[j][1] = y;
            rects[j][2] = w;
            rects[j][3] = h;

            x += mainWidth/numColumns;
        }//end for

        float[] temp = new float[2];
        temp = positionPressure((-1)*readings[0][readings[0].length-1]-redTrim[0], (-1)*readings[1][readings[1].length-1]-blueTrim[0]);
        println((-1)*readings[0][readings[0].length-1]-redTrim[0]);
        println((-1)*readings[1][readings[1].length-1]-blueTrim[0]);
        println(temp);
        updateLocation(temp[0], temp[1]);
    }//end touchLocation


    float nMin = 1.0f;
    float nMax = 1.0f;
    //you may need to adjust default max/min as necessary
    //also adjust default values in reset() function


    /**Calculates position pressure based on R/B value data from the serial
     * */
    public float[] positionPressure(float red, float blue)
    {
        float rl = (red < 1.0f)? 1.0f : red;
        float rr = (blue < 1.0f)? 1.0f : blue;

        float n = log(rl) - log(rr);
        nMin = (n < nMin)? n : nMin;
        nMax = (n > nMax)? n : nMax;
        //update max/min n-value with each calculation

        float nLoc = map(n, nMin, nMax, 0, 1); //map n-value to standard 0-1 scale for easy analysis; normalized reading
        float pressure = (red + blue) / 2.0f - 17.0f; //avg pressure between left/right

        if (running) {
            readings[2][readings[2].length-1] = n;
            readings[3][readings[3].length-1] = nLoc;
            //store n and n-normal in readings array

            for (int i = idx; i < time.length - 1; i++)
            {
                readings[2][i] = readings[2][i + 1]; // Shift the readings to the left so can put the newest reading in
                readings[3][i] = readings[3][i + 1];
            }
        }

        return new float[] {nLoc, pressure};
    }

    /** Updates location on GUI
     * */
    public void updateLocation(float n, float p) {
        int index;
        float lowerThreshold = 10.0f;
        float upperThreshold = 20.0f;
        //for touch location-
        //pale color fills in when avg pressure is at lower threshold;
        //darker color fills past upper threshold

        index = round(map(n, nRange[0], nRange[1], 0, numColumns-1)); //map n to nearest rectangle index (0-11)
        index = (index < 0)? 0 : index;
        index = (index > numColumns-1)? numColumns : index;

        //upper & lower pressure threshold for coloring the rectangles
        //if (p > lowerThreshold && p < upperThreshold) fill(light);
        //else if (p >= upperThreshold) fill(dark);
        //else fill(rectsFill);

        int Sscale = 255;

        int s = round(map((redTrim[0] + blueTrim[0])/2 + p, sensitivity[0], sensitivity[1], 0, Sscale));
        s = (s < 0)? 0 : s;
        s = (s > Sscale)? Sscale : s;
        fill(color(255, 0, 0, s));


        //use index to pull coordinates from corresponding rects[][] array to fill in the correct rectangle
        float x = rects[index][0];
        float y = rects[index][1];
        float w = rects[index][2];
        float h = rects[index][3];
        rect(x, y, w, h);
    }//end updateLocation

    public void console()
    {

        float yOffset = headerHeight + margin;
        float xOffset = frameWidth;
        float consoleWidth = width * 0.3f - margin;

        //draw title
        fill(subtitleBG);
        stroke(stroke);
        rect(xOffset, yOffset, consoleWidth, margin);
        fill(subtitle);
        textFont(font, subFontSize);
        float textWidth = textWidth("RAW SENSITIVITY DATA");
        text("RAW SENSITIVITY DATA", frameWidth + (consoleWidth/2.0f - textWidth/2.0f), yOffset + margin * 0.75f);

        //draw container
        stroke(stroke);
        rect(xOffset, yOffset + margin*1.25f, consoleWidth, height - headerHeight - margin*3.25f);

        //draw labels
        fill(bg);

        rect(xOffset, yOffset+margin*1.25f, consoleWidth, margin);
        rect(xOffset, yOffset+margin*2.25f, consoleWidth, margin);

        float textY = yOffset + margin * 2.0f;
        float textX = frameWidth + margin/2.0f;
        fill(0);
        textFont(font, subFontSize);
        text("N-min: ", textX, textY);
        text(nMin, textX+textWidth("N-min: "), textY);
        text("N-max: ", textX+textWidth("N-min:0000__|__"), textY);
        text(nMax, textX+textWidth("N-min:0000___N-max:__"), textY);
        textY += margin;
        textFont(font, subFontSize);
        text("L", frameWidth+margin/2.0f, textY);
        text("R", frameWidth + margin*2.5f, textY);
        text("N", frameWidth + margin*4.75f, textY);
        text("N-n", frameWidth+margin*6.75f, textY);
        textFont(font, subFontSize);
        textY += margin;


        //number of data points to show at a time
        //shorter list for shorter window heights
        //(could be more refined)
        int numData = 20;
        if (height < 500) numData = 10;



        //print 20 most recent values from readings[][] array
        //[0][] = left, [1][] = right
        //[2][] = raw n-value, [3][] = n-normal
        for (int i = 0; i < 4; i++)
        {
            for (int j = time.length-1; j > time.length-numData; j--)
            {
                if (i < 2) text(round((-1)*readings[i][j]), textX, textY);
                else text(readings[i][j], textX, textY);
                textY += subFontSize*1.5f;
            }

            textY = yOffset + margin*4.0f;
            if (i == 1) textX += margin*1.5f;
            else if (i == 2) textX += margin*2.5f;
            else textX += margin*2.0f;
        }
    }//end console()

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

    /**Draws buttons
     * */
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

    /**Detects Keyboard entries
     * */
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

    /**Detects mouse click
     * */
    public void mousePressed()
    {
        if (mousePause) vRunning = !vRunning;
        else if (mouseReset) reset();
        else if (mouseConsole) console = !console;
    }
    public void settings() {  size(850, 650);  pixelDensity(displayDensity()); }

}
