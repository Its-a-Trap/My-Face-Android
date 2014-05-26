package com.example.itsatrap.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.location.Location;
import android.location.LocationManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MapActivity extends Activity implements GoogleMap.OnMapClickListener,
        GoogleMap.OnInfoWindowClickListener, LocationListener, GoogleMap.OnMarkerClickListener
{

    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ArrayAdapter listAdapter;

    private GoogleMap map;
    private GameController gameController;
    private Marker plantableToPlace;
    private HashMap<Marker, Plantable> markerData;

    private SharedPreferences sharedPrefs;

    private List<Marker> currentlyDisplayedEnemyPlantables;

    private Date lastSweeped;
    private List<Marker> sweepMinesVisible;

    private MapActivity thisref;

    //The sweep cooldown, in minutes
    private final int SWEEP_COOLDOWN = 30;
    //The amount of time sweeped mines should be visible, in seconds
    private final int SWEEP_DURATION = 30;
    //The radius of the sweep, in meters
    private final int SWEEP_RADIUS = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        //Initialize internal state information
        currentlyDisplayedEnemyPlantables = new ArrayList<Marker>();
        sweepMinesVisible = new ArrayList<Marker>();

        plantableToPlace = null;

        thisref = this;

        sharedPrefs = getSharedPreferences(getString(R.string.SharedPrefName), 0);

        //Create the game controller object
        gameController = new GameController(new User(sharedPrefs.getString(getString(R.string.PrefsEmailString), ""), "537e48763511c15161a1ed9b", ""), (LocationManager) getSystemService(Context.LOCATION_SERVICE), this);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        listAdapter = new ScoreArrayAdapter(this, R.layout.drawer_list_item, gameController.getHighScores());
        drawerList.setAdapter(listAdapter);
        // Get a handle to the Map Fragment
        map = ((MapFragment) getFragmentManager()
                .findFragmentById(R.id.map)).getMap();

        markerData = new HashMap<Marker, Plantable>();

        if  (map != null)
        {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            LatLng curLoc = getCurLatLng();

            map.setMyLocationEnabled(true);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(curLoc, 13));

            //Set the listener
            map.setOnMapClickListener(this);
            map.setOnInfoWindowClickListener(this);
            map.setOnMarkerClickListener(this);

            //Add map markers for previously set mines
            List<Plantable> userPlantables = gameController.getUserPlantables();
            for (Plantable plantable : userPlantables)
            {
                Marker marker = map.addMarker(new MarkerOptions()
                        .position(plantable.getLocation())
                        .title(getString(R.string.yourTrap))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                markerData.put(marker, plantable);
            }

            //Set this activity to listen for location changes
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            locationManager.requestLocationUpdates(1000, 1, criteria, this, null);
        }
    }

    private LatLng getCurLatLng()
    {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location curLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(new Criteria(), true));
        LatLng curLoc = new LatLng(curLocation.getLatitude(), curLocation.getLongitude());
        return curLoc;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void sweep(View view)
    {
        if (lastSweeped != null && new Date().getTime() - lastSweeped.getTime() < 1000*60*SWEEP_COOLDOWN)
        {
            long minutesLeft = (new Date().getTime() - lastSweeped.getTime())/1000/60 + 1;
            Toast.makeText(this, "Can't sweep again for "+minutesLeft+" minutes.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Plantable> enemyTraps = gameController.getEnemyPlantablesWithinRadius(getCurLatLng(), SWEEP_RADIUS);
        // TODO: make swept enemy traps disappear eventually
        // (and what if they are triggered before disappearing?)
        for (Plantable plantable : enemyTraps)
        {
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(plantable.getLocation())
                    .title(getString(R.string.watchOut))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            sweepMinesVisible.add(marker);
        }

        lastSweeped = new Date();

        Timer sweepTimer = new Timer(true);
        Date afterSweepDuration = new Date();
        afterSweepDuration.setTime(afterSweepDuration.getTime()+1000*SWEEP_DURATION);

        sweepTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                thisref.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        thisref.removeSweepedMines();
                    }
                });
            }
        }, afterSweepDuration);
    }

    @Override
    public void onMapClick(LatLng latLng)
    {
        // If you're out of plantable items, don't let them do it
        if (gameController.getNumUserPlantablesLeft() <= 0)
        {
            Toast.makeText(this, "No traps left", Toast.LENGTH_SHORT).show();
        }
        else
        {
            if (plantableToPlace == null)
            {
                plantableToPlace = map.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(getString(R.string.placeTrap))
                        .alpha((float) 0.4)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                plantableToPlace.showInfoWindow();
            }
            else
            {
                plantableToPlace.remove();
                plantableToPlace = null;
            }
        }
    }

    @Override
    public void onInfoWindowClick(Marker marker)
    {
        marker.setAlpha(1);
        marker.setDraggable(false);
        marker.setTitle(getString(R.string.yourTrap));
        marker.hideInfoWindow();
        Plantable newPlantable = gameController.addUserPlantable(marker.getPosition());
        if (newPlantable != null)
        {
            markerData.put(marker, newPlantable);
        }
        plantableToPlace = null;
    }

    @Override
    public boolean onMarkerClick(Marker marker)
    {
        if (marker.getTitle().equals(getString(R.string.yourTrap)))
        {
            // get the Plantable to remove from the marker
            Plantable trapToRemove = markerData.get(marker);
            gameController.removeUserPlantable(trapToRemove);
            marker.remove();
        }


        /* if marker is yours:
            get the Plantable trapToRemove from the marker somehow??? -- compare their locations?
            remove the trap
            undraw the marker
         */
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {

        gameController.updateLocation(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    public void updateHighScores()
    {
        listAdapter.notifyDataSetChanged();
    }

    public void updateMyMines()
    {
        for (Marker marker : currentlyDisplayedEnemyPlantables)
        {
            marker.remove();
        }

        currentlyDisplayedEnemyPlantables.clear();

        List<Plantable> myPlantables = gameController.getUserPlantables();
        synchronized (myPlantables)
        {
            //Add map markers for previously set mines
            for (int i = 0; i < myPlantables.size(); ++i) {
                currentlyDisplayedEnemyPlantables.add(map.addMarker(new MarkerOptions()
                        .position(myPlantables.get(i).getLocation())
                        .title("It's a trap!")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))));
            }
        }
    }

    public void removeSweepedMines()
    {
        for (Marker sweepMine : sweepMinesVisible)
        {
            sweepMine.remove();
        }
    }


}
