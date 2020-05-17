package com.example.hw2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static com.google.android.gms.maps.GoogleMap.OnMapLoadedCallback;
import static com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import static com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, OnMapLoadedCallback, OnMarkerClickListener, OnMapLongClickListener, SensorEventListener {
    private  String MARKERS_JSON_FILE = "markers.json";
    private GoogleMap mMap;
    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    boolean isSensorWorking = false;
    Marker gpsMarker = null;
    List<MarkerListContent > markerList;

    private SensorManager sensorManager;
    private Sensor mSensor;
    private TextView sensorDisplay;
    private FloatingActionButton recordFab;
    private FloatingActionButton exitFab;
    private Button clearButton;

    private void createLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void startLocationUpdates(){
        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback ,null);
    }

    private void createLocationCallback(){
        locationCallback = new LocationCallback(){
            @Override
                public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    if (gpsMarker != null)
                        gpsMarker.remove();

                }
            }
        };
    }
    private void stopLocationUpdates(){
        if(locationCallback != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient( this);

        markerList = new ArrayList<>();

        clearButton =findViewById(R.id.clear);
        recordFab = findViewById(R.id.fab1);
        exitFab = findViewById(R.id.fab2);

        sensorDisplay = findViewById(R.id.sensor);
        sensorDisplay.setVisibility(View.INVISIBLE);
        recordFab.setVisibility(View.INVISIBLE);
        exitFab.setVisibility(View.INVISIBLE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = null;
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        recordFab.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v){
                sensorDisplay.setVisibility(View.VISIBLE);
                isSensorWorking = !isSensorWorking;
                startSensor(isSensorWorking);
            }
        });
        exitFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordFab.animate().translationY(120f).alpha(0f).setDuration(1000);
                exitFab.animate().translationY(120f).alpha(0f).setDuration(1000);
                recordFab.setVisibility(View.INVISIBLE);
                exitFab.setVisibility(View.INVISIBLE);
                sensorDisplay.setVisibility(View.INVISIBLE);
                startSensor(false);
            }
        });
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
               markerList.removeAll(markerList);
                saveMarkersToJson();
            }
        });
    }

    void startSensor(boolean isSensorWorking){
        if(mSensor !=null){
            if(isSensorWorking)
            { sensorDisplay.setVisibility(View.VISIBLE);
                sensorManager.registerListener(this,mSensor,SensorManager.SENSOR_DELAY_NORMAL);}
            else
            {sensorDisplay.setVisibility(View.INVISIBLE);
                sensorManager.unregisterListener(this);}
        }
    }

    @Override
    protected  void  onPause(){
        super.onPause();
        stopLocationUpdates();
        if(mSensor != null)
        sensorManager.unregisterListener( this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        restoreFromJson();
    }

    @Override
    public void onMapLoaded() {
        Log.i(MapsActivity.class.getSimpleName(),"MapLoaded");
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION );
            return;
        }
        createLocationRequest();
        createLocationCallback();
        startLocationUpdates();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
                mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.latitude,latLng.longitude))
                .icon(bitmapDescriptorFromVector(this,R.drawable.marker))
                .alpha(0.8f)
                .title(String.format("Position:(%.2f,%.2f)",latLng.latitude,latLng.longitude)));
        markerList.add(new MarkerListContent(latLng.latitude,latLng.longitude));
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        recordFab.setVisibility(View.VISIBLE);
        exitFab.setVisibility(View.VISIBLE);
        recordFab.animate().translationY(0f).alpha(1f).setDuration(1000);
        exitFab.animate().translationY(0f).alpha(1f).setDuration(1000);
        return false;
    }

    public void zoomInClick(View v){
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View v){
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, @DrawableRes int vectorDrawableResourceId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorDrawableResourceId);
        vectorDrawable.setBounds(40, 20, vectorDrawable.getIntrinsicWidth() , vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public void onSensorChanged(SensorEvent event){
        sensorDisplay.setText("Accelerator: \n X: "+event.values[0] +" Y: " + event.values[1]);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    public void saveMarkersToJson(){
        Gson gson = new Gson();
        String listJson = gson.toJson(markerList);
        FileOutputStream outputStream;
        try{
            outputStream = openFileOutput(MARKERS_JSON_FILE,MODE_PRIVATE);
            FileWriter writer = new FileWriter(outputStream.getFD());
            writer.write(listJson);
            writer.close();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }

    }

    public void restoreFromJson(){
        FileInputStream inputStream;
        Gson gson = new Gson();
        String readJson;

        try {
            inputStream = openFileInput(MARKERS_JSON_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[10000];
            int n;
            StringBuilder builder = new StringBuilder();
            while ((n = reader.read(buf))>=0){
                String tmp = String.valueOf(buf);
                String substring = (n<10000) ? tmp.substring(0,n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type collectionType = new TypeToken<List<MarkerListContent> >(){}.getType();
            List<MarkerListContent> o = gson.fromJson(readJson, collectionType);
            if (o != null){

                for(MarkerListContent mr : o){
                    markerList.add(mr);
                    mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(mr.mlatitude, mr.mlongitude))
                    .icon(bitmapDescriptorFromVector(this,R.drawable.marker))
                    .alpha(0.8f)
                    .title(String.format("Position:(%.2f,%.2f)",mr.mlatitude,mr.mlongitude)));
                }

            }
        }catch (FileNotFoundException e){

            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    @Override
    protected void onDestroy() {
        saveMarkersToJson();
        super.onDestroy();
    }


}
