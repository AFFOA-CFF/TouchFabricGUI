package com.CFF;

public class Model {

    public float[] positionPressure(float r1, float r2)
    {
        float rl = (r1 < 1.0f)? 1.0f : r1;
        float rr = (r2 < 1.0f)? 1.0f : r2;

        float n = log(rl) - log(rr);
        nMin = (n < nMin)? n : nMin;
        nMax = (n > nMax)? n : nMax;
        //update max/min n-value with each calculation

        float nNormal = map(n, nMin, nMax, 0, 1); //map n-value to standard 0-1 scale for easy analysis
        float p = (r1 + r2) / 2.0f - 17.0f; //avg pressure between left/right

        if (running) {
            readings[2][readings[2].length-1] = n;
            readings[3][readings[3].length-1] = nNormal;
            //store n and n-normal in readings array

            for (int i = idx; i < time.length - 1; i++)
            {
                readings[2][i] = readings[2][i + 1]; // Shift the readings to the left so can put the newest reading in
                readings[3][i] = readings[3][i + 1];
            }
        }//end if(running)

        return new float[] {nNormal, p};
    }//end positionPressure


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
    }//end keyPressed

    public void mousePressed()
    {
        if (mousePause) vRunning = !vRunning;
        else if (mouseReset) reset();
        else if (mouseConsole) console = !console;
    }

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

}
