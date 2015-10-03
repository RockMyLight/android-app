package hackzurich.rockmylight.app;

import java.io.Serializable;

/**
 * // immutable
 *
 * Created by kmisiunas on 15-10-03.
 */
public class LightStep implements Serializable {

    private long timestamp;
    private String color;

    public LightStep(long timestamp, String color) {
        this.timestamp = timestamp;
        this.color = color;
    }



    public String getColor() {
        return color;
    }


    public long getTimestamp() {
        return timestamp;
    }
}
