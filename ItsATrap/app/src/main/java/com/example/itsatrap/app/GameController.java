package com.example.itsatrap.app;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by maegereg on 5/10/14.
 */
public class GameController {

    private User curUser;
    private List<Plantable> userPlantables;
    private List<Plantable> enemyPlantables;
    private List<PlayerInfo> highScores;

    public GameController(User curUser, List<Plantable> userPlantables, List<Plantable> enemyPlantables, List<PlayerInfo> highScores)
    {
        this.curUser = curUser;
        this.userPlantables = userPlantables;
        this.enemyPlantables = enemyPlantables;
        this.highScores = highScores;
    }

    public GameController(User curUser, LocationManager locManager)
    {
        this.curUser = curUser;
        Location curLocation = locManager.getLastKnownLocation(locManager.getBestProvider(new Criteria(), true));
        LatLng curLoc = new LatLng(curLocation.getLatitude(), curLocation.getLongitude());
        getHighScoresAndEnemyMinesFromServer(curLoc);
        userPlantables = getMyMinesFromServer();
    }

    public void setHighScores(List<PlayerInfo> highScores)
    {
        this.highScores = highScores;
    }

    public void setEnemyPlantables(List<Plantable> enemyPlantables)
    {
        this.enemyPlantables = enemyPlantables;
    }

    public List<PlayerInfo> getHighScores()
    {
        return highScores;
    }

    public List<Plantable> getUserPlantables()
    {
        return userPlantables;
    }

    public void addUserPlantable(Plantable newPlantable)
    {
        userPlantables.add(newPlantable);
    }

    public void removeUserPlantable(Plantable toRemove)
    {
        userPlantables.remove(toRemove);
    }

    public void removeUserPlantable(int idToRemove)
    {
        for (int i = 0; i<userPlantables.size(); ++i)
        {
            if (userPlantables.get(i).getPlantableId() == idToRemove)
            {
                userPlantables.remove(i);
                return;
            }
        }
    }

    /*
        Returns a list of all enemy plantables within their radius of the provided location
     */
    public List<Plantable> checkForCollisions(LatLng currentLocation)
    {
        List<Plantable> results = new ArrayList<Plantable>();
        for (int i = 0; i<enemyPlantables.size(); ++i)
        {
            LatLng otherLocation = enemyPlantables.get(i).getLocation();
            float[] distance = new float[1];
            Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, otherLocation.latitude, otherLocation.longitude, distance);
            if (distance[0] < enemyPlantables.get(i).getRadius())
            {
                results.add(enemyPlantables.get(i));
            }
        }
        return results;
    }

    /*
        Returns a list of all enemy plantables within the given radius of the given location - used for sweeping
     */
    public List<Plantable> getEnemyPlantablesWithinRadius(LatLng currentLocation, float radius)
    {
        List<Plantable> results = new ArrayList<Plantable>();
        for (int i = 0; i<enemyPlantables.size(); ++i)
        {
            LatLng otherLocation = enemyPlantables.get(i).getLocation();
            float[] distance = new float[1];
            Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, otherLocation.latitude, otherLocation.longitude, distance);
            if (distance[0] < radius)
            {
                results.add(enemyPlantables.get(i));
            }
        }
        return results;
    }

    //Stub
    private void getHighScoresAndEnemyMinesFromServer(LatLng curLoc)
    {
        //Server magic goes here
        PlayerInfo[] hardCodedEntries = {new PlayerInfo("Jeff", 9001) , new PlayerInfo("DSM", 6), new PlayerInfo("Calder", 6), new PlayerInfo("Carissa", 6), new PlayerInfo("DermDerm", 5), new PlayerInfo("Tao", 5), new PlayerInfo("Carlton", 5), new PlayerInfo("Quinn", 5)};
        highScores = Arrays.asList(hardCodedEntries);
        enemyPlantables = new ArrayList<Plantable>();
    }

    //Stub
    private List<Plantable> getMyMinesFromServer()
    {
        //Server magic goes here
        ArrayList<Plantable> toReturn = new ArrayList<Plantable>();
        toReturn.add(new Plantable(0, 3, new LatLng(44.456799, -93.156410), new Date(), 100, 15));
        toReturn.add(new Plantable(1, 3, new LatLng(44.459832, -93.151389), new Date(), 100, 15));
        return toReturn;
    }

}
