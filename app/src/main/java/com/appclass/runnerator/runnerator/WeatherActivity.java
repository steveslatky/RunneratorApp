package com.appclass.runnerator.runnerator;

//AndroidWebServices
// 	Mickey Moorhead
//	8/14/14


import java.net.HttpURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import com.google.gson.*;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.os.Build;

public class WeatherActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment()).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container,
                    false);
            return rootView;
        }
    }
    public void update(View view) throws Exception{
        weather wupdate = new weather();
        wupdate.execute();
    }


    //Get weather from wunderground
    private class weather extends AsyncTask<String, Void, JsonArray>{
        @Override
        protected JsonArray doInBackground(String... params){
            try{
                //zip
                URL getZip = new URL("http://api.wunderground.com/api/6e4b4a87938f8cf0/geolookup/q/autoip.json");
                URLConnection connect = getZip.openConnection();

                JsonParser jp = new JsonParser();
                InputStreamReader read  = new InputStreamReader(connect.getInputStream());
                JsonElement root = jp.parse(read);
                JsonObject rootobj = root.getAsJsonObject();
                String zip = rootobj.get("location").getAsJsonObject().get("zip").getAsString();


                //weather
                URL hourly = new URL("http://api.wunderground.com/api/6e4b4a87938f8cf0/hourly/q/"
                        + zip
                        + ".json");
                connect = hourly.openConnection();

                jp = new JsonParser();
                read  = new InputStreamReader(connect.getInputStream());
                root = jp.parse(read);
                rootobj = root.getAsJsonObject();
                JsonArray hour = rootobj.get("hourly_forecast").getAsJsonArray();

                return hour;
            }
            catch (MalformedURLException e){

            }
            catch (IOException i){

            }
            return null;
        }
        protected void onPostExecute(JsonArray result){
            TableLayout tl = (TableLayout) findViewById(R.id.weatherTable);
            tl.removeAllViews(); //clear tableview for every update

            //Setting headers
            TableRow tr = new TableRow(getApplicationContext());
            TextView time = new TextView(getApplicationContext());
            time.setText("Time");
            time.setTextSize(20);
            TextView condition = new TextView(getApplicationContext());
            condition.setText("Condition");
            condition.setTextSize(20);
            TextView pic = new TextView(getApplicationContext());
            tr.addView(time);
            tr.addView(condition);
            tr.addView(pic);
            tl.addView(tr);


            //go through hours
            for (int i=0; i<result.size(); i++){
                //System.out.println(result.get(i));
                JsonObject hour = result.get(i).getAsJsonObject();
                tr = new TableRow(getApplicationContext());

                time = new TextView(getApplicationContext());
                JsonObject timeobj = hour.get("FCTTIME").getAsJsonObject();
                int hr = timeobj.get("hour").getAsInt();

                //MT convert
                if (hr>12)
                    hr = hr-12;
                if (hr==0)
                    hr = 12;

                //Formatted as M/DD #PM
                time.setText(timeobj.get("mon").getAsString()
                        + "/"
                        + timeobj.get("mday").getAsString()
                        + " "
                        + hr
                        + timeobj.get("ampm").getAsString()
                        + "   ");


                condition = new TextView(getApplicationContext());
                //Condition, temp, and humidity are lumped together
                condition.setText(hour.get("condition").getAsString()+ "\t\t" + "\n"
                        + hour.get("temp").getAsJsonObject().get("english").getAsString()+ " F\t\t"
                        + hour.get("humidity").getAsString()+ "%");


                ImageView picture = new ImageView(getApplicationContext());
                bmi bitmap = new bmi();
                bitmap.execute(hour); //spin off new thread for image
                Bitmap bit = null;
                try {
                    bit = bitmap.get();
                }
                catch (Exception e){

                }
                picture.setImageBitmap(bit);


                //Add info to TableRow and put in TableLayout
                tr.addView(time);
                tr.addView(condition);
                tr.addView(picture);
                tl.addView(tr);
            }
        }
    }

    // New thread for getting image
    //Adapted from http://stackoverflow.com/questions/4945115/set-url-image-to-image-view
    private class bmi extends AsyncTask<JsonObject, Void, Bitmap>{
        protected Bitmap doInBackground(JsonObject... params){
            try{

                URL url = new URL(params[0].get("icon_url").getAsString());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();

                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                return myBitmap;

            } catch (IOException e) {
                e.printStackTrace();
                Log.e("Exception",e.getMessage());
            }
            return null;
        }
        protected void onPostExecute(Bitmap result){

        }
    }
}
