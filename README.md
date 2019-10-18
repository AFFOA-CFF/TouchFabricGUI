## TouchFabricGUI
A [Processing](https://processing.org/download/) GUI that displays the location of touch reported from the [Touch Fabric Sensor](https://github.com/PaFDC/CTS-Maker-Kit). The repository contains the following files described in the following sections:

* TouchFabricGUI.pde
* data/config.xml

### TouchFabricGUI.pde
The touch visualizer application. Run this program using the Processing IDE.

#### Controls

* Pause: `Space`. Pauses or resumes updating.
* Reset: `r`. Resets the maximum red and blue signal range to the default range.
* Display console: `d`. Displays or hides the raw output from the touch sensor. The console shows the red (left) and blue (right) readings and the normalized position.
* Set number of bars: `Up` and `Down` arrow keys. Increase or decrease the number of bars under Touch Location by 2. The range is between 2 and 40.
* Set scrolling graph range: `Left` and `Right`. Set the number of points displayed on the scrolling graph under Sensitivity Monitors.

### data/config.xml
The touch visualizer configuration file. This file is provided as a way to change application settings outside of modifying the source code or store working versions of settings within a separate file. The file contains XML fields for the following values within the application:

* `redTrim`. The trimmed range of the red output signal. The range is defined inclusively between `min` and `max`, where `max > min` and `min >= 0`.
* `blueTrim`. The trim range of the blue output signal. The range is defined inclusively between `min` and `max`, where `max > min` and `min >= 0`.
* `numColumns`. The integer number of bars that appear under the Touch Location header. The number of bars is defined by `val`, where `2 <= val <= 40`.
* `sensitivity`. The pressure sensitivity range of the combined red and blue outputs. The range is defined inclusively by `min` and `max`, where `max > min` and `min >= 0`.
* `nRange`. The normalized position localization range. The range is defined inclusively between `min` and `max`, where `max > min`, `min >= 0` and `max <= 1`.

#### Calibration
Determining working values for the above settings may require trial-and-error testing. The default config.xml file contains working parameters for a touch sensor module connected to a laminated fabric sensor. It is recommended that a backup copy of the config.xml file is made before alterations are saved.
