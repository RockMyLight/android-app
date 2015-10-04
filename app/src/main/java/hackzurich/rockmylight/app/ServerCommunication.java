package hackzurich.rockmylight.app;

import android.os.AsyncTask;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by kmisiunas on 15-10-03.
 */
public class ServerCommunication extends AsyncTask<String, String, String> {

    protected StepsBuffer buffer;


    public ServerCommunication(StepsBuffer buffer){
        this.buffer = buffer;
    }

    @Override
    protected String doInBackground(String... uri) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        String responseString = null;
        try {
            response = httpclient.execute(new HttpGet(uri[0]));
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                responseString = out.toString();
                out.close();
            } else{
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (ClientProtocolException e) {
            //TODO Handle problems..
            responseString = "";
        } catch (IOException e) {
            //TODO Handle problems..
            responseString = "";
        }
        return responseString;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        buffer.add(parse(result));
    }


    public LightStep [] parse(String st) {
        if (st.trim() == "{}"){
            buffer.clear();
            return new LightStep[0];
        } else {
            try {
                JSONObject obj = new JSONObject(st);
                buffer.setDevicesInNetwork(obj.getInt("num_of_clients"));

                JSONArray arr = obj.getJSONArray("frames");

                LightStep[] lightArr = new LightStep[arr.length()];

                for (int i = 0; i < arr.length(); i++) {
                    LightStep el = new LightStep(
                            arr.getJSONObject(i).getLong("timestamp"),
                            arr.getJSONObject(i).getString("color")
                    );
                    lightArr[i] = el;
                }

                return lightArr;

            } catch (Exception e) {
                Log.e("parse", "Could not parse JSON: " + e);
                return new LightStep[0];
            }
        }
    }

}

