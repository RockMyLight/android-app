package hackzurich.rockmylight.app;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

/**
 * badly nested!!!! not long term
 *
 * Created by kmisiunas on 15-10-03.
 */
public class GPSListener implements LocationListener {

    private LightShow ls;

    public GPSListener(LightShow ls){
        this.ls = ls;
    }

    @Override
    public void onLocationChanged(Location loc) {
        long dt = System.currentTimeMillis() - loc.getTime();
        ls.setDt(dt); //time sync
        ls.setLocation(loc.getLatitude(), loc.getLongitude());

        String longitude = "Longitude: " + loc.getLongitude();
        Log.v("position", longitude);
        String latitude = "Latitude: " + loc.getLatitude();
        Log.v("position", latitude);

        /*------- To get city name from coordinates -------- */
//        String cityName = null;
//        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
//        List<Address> addresses;
//        try {
//            addresses = gcd.getFromLocation(loc.getLatitude(),
//                    loc.getLongitude(), 1);
//            if (addresses.size() > 0)
//                System.out.println(addresses.get(0).getLocality());
//            cityName = addresses.get(0).getLocality();
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
//        String s = longitude + "\n" + latitude + "\n\nMy Current City is: "
//                + cityName;
//        editLocation.setText(s);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onProviderEnabled(String provider) {}

}
