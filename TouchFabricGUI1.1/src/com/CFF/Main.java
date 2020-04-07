package com.CFF;

import processing.core.PApplet;

public class Main {
    public static void main(String[] passedArgs) {
        String[] appletArgs = new String[] { "--present", "--window-color=#666666", "--stop-color=#cccccc", "TouchFabricGUI" };
        if (passedArgs != null) {
            PApplet.main(com.CFF.TouchFabricGUI.class.getCanonicalName());
        } else {
            PApplet.main(appletArgs);
        }
    }
}
