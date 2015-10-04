package hackzurich.rockmylight.app;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kmisiunas on 15-10-03.
 */
public class StepsBuffer {

    private List<LightStep> array;

    protected int devicesInNetwork  = 0;

    public StepsBuffer(){
        array = new ArrayList<LightStep>();
    }

    public LightStep getNext(){
        if ( array.size() > 0 ) {
            LightStep el = array.get(0);
            array.remove(0);
            return el;
        } else {
            return null;
        }
    }

    public void add(LightStep [] addArr){
        if(addArr.length > 0) {
            long tDrop = addArr[0].getTimestamp();
            // drop olf elements form the buffer that have overlapping elements
            for (int i = 0; i < array.size(); i++) {
                if (array.get(i).getTimestamp() >= tDrop) {
                    array.remove(i);
                    i--;
                }
            }
            // add new elements
            for (int i = 0; i < addArr.length; i++) {
                array.add(addArr[i]);
            }
        }
        removeStale();
        //debug
//        String debug = "";
//        for(int i=0; i<array.size(); i++){
//            debug = debug + ", "+ (array.get(i).getTimestamp() -System.currentTimeMillis());
//        }
//        Log.i("timestamps", "\n\n=>   "+debug);
    }

    public long getNextTimestamp(){
        if ( array.size() > 0 ) {
            return array.get(0).getTimestamp();
        } else {
            return 1000; // todo: panic?
        }
    }

    public void removeStale(){
        for( int i=0; i<array.size(); i++){
            if(array.get(i).getTimestamp() <= System.currentTimeMillis()){
                array.remove(i);
                i--;
            }
        }
    }

    public void setDevicesInNetwork(int n) { devicesInNetwork = n;}
    public int getDevicesInNetwork(){ return devicesInNetwork; }

    public void clear() {
        array = null;
        devicesInNetwork = 0;
    }
}
