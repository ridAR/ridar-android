package com.ridar.akhilraj.ridar;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.common.RoadElement;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;

import java.lang.ref.WeakReference;
import java.util.List;
import android.view.View;
import android.widget.Toast;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Maneuver;
import com.here.android.mpa.routing.RoutingError;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;

public class MainActivity extends AppCompatActivity {


    // map embedded in the map fragment
    private Map map = null;

    // map fragment embedded in this activity
    private MapFragment mapFragment = null;

    // TextView for displaying the current map scheme
    private TextView textViewResult = null;

    // MapRoute for this activity
    private MapRoute mapRoute = null;

    private long simulationSpeed = 10;

    private  NavigationManager navigationManager = null;

    private long EDA = -1;

    private Boolean StateBlink = false;
    private Boolean StateSolid = false;

    private 


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewResult = (TextView) findViewById(R.id.title);
        textViewResult.setText(R.string.textview_routecoordinates_2waypoints);



        //Map Setup
        mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.mapfragment);
        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(
                    OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    // retrieve a reference of the map from the map fragment
                    map = mapFragment.getMap();
                    map.setMapScheme(Map.Scheme.CARNAV_TRAFFIC_DAY);

                    map.setCenter(new GeoCoordinate(12.9716, 77.5946, 0.0), Map.Animation.NONE);

                    map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);
                    map.getPositionIndicator().setVisible(true);
                    navigationManager = NavigationManager.getInstance();
                } else {
                    System.out.println("ERROR: Cannot initialize Map Fragment");
                }
            }
        });

    }

    private NavigationManager.NewInstructionEventListener newInstructionEventListener = new NavigationManager.NewInstructionEventListener() {

        @Override
        public void onNewInstructionEvent() {
            Maneuver maneuver = navigationManager.getNextManeuver();
            if (maneuver != null) {
                if (maneuver.getAction() == Maneuver.Action.END) {
                    //notify the user that the route is complete
                }

                Log.d("NAVIGATION:GETTURN :", maneuver.getTurn().toString());
                Log.d("NAVIGATION:DISTTONXTM:", String.valueOf(maneuver.getDistanceToNextManeuver()));
                Log.d("NAVIGATION:COORD:",maneuver.getCoordinate().toString());
                Log.d("NAVIGATION:Road",maneuver.getNextRoadName());

                Log.d("SIGNAL","STOP");
                StateBlink = false;
                StateSolid = false;
            }
        }
    };

    private NavigationManager.PositionListener positionListener = new NavigationManager.PositionListener() {
        @Override
        public void onPositionUpdated(GeoPosition geoPosition) {
            super.onPositionUpdated(geoPosition);

            map.setCenter(geoPosition.getCoordinate(), Map.Animation.NONE);

            EDA = navigationManager.getNextManeuverDistance();
            Log.d("EDA:",String.valueOf(EDA));


            if(StateSolid == false){
                if(EDA<50){
                   //SOLID
                    Log.d("SIGNAL:","SIGNALSOLID");
                    StateBlink = false;
                    StateSolid = true;
               }else if(EDA<200 && StateBlink==false){
                   //BLINK
                    Log.d("SIGNAL:","SIGNALBLINK");
                    StateBlink = true;
               }
            }
        }
    };
    private CoreRouter.Listener routerListener = new CoreRouter.Listener()
            {
                public void onCalculateRouteFinished(List<RouteResult> routeResults, RoutingError errorCode) {

                    if (errorCode == RoutingError.NONE && routeResults.get(0).getRoute() != null) {
                        // create a map route object and place it on the map
                        mapRoute = new MapRoute(routeResults.get(0).getRoute());
                        map.addMapObject(mapRoute);

                        // Get the bounding box containing the route and zoom in
                        GeoBoundingBox gbb = routeResults.get(0).getRoute().getBoundingBox();
                        map.zoomTo(gbb, Map.Animation.NONE, Map.MOVE_PRESERVE_ORIENTATION);
                        textViewResult.setText(
                                String.format("Route calculated with %d maneuvers.",
                                        routeResults.get(0).getRoute().getManeuvers().size()));

                        navigationManager.setMap(map);

                        navigationManager.addPositionListener(new WeakReference<NavigationManager.PositionListener>(positionListener));
                        navigationManager.addNewInstructionEventListener(new WeakReference<NavigationManager.NewInstructionEventListener>(newInstructionEventListener));
                        NavigationManager.Error error = navigationManager.simulate(routeResults.get(0).getRoute(),16);

                    } else {
                        textViewResult.setText(
                                String.format("Route calculation failed: %s", errorCode.toString()));
                    }
                }

                public void onProgress(int percentage) {
                    textViewResult.setText(
                            String.format("... %d percent done ...", percentage));
                }

            };

    // Functionality for taps of the "Get Directions" button
    public void getDirections(View view) {
        // 1. clear previous results
        textViewResult.setText("");
        if (map != null && mapRoute != null) {
            map.removeMapObject(mapRoute);
            mapRoute = null;
        }

        // 2. Initialize CoreRouter
        CoreRouter coreRouter = new CoreRouter();

        // 3. Select routing options via RoutingMode
        RoutePlan routePlan = new RoutePlan();

        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);
        routePlan.setRouteOptions(routeOptions);

        // 4. Select Waypoints for your routes
        // START: BC Place Stadium
        routePlan.addWaypoint(new GeoCoordinate(13.0311221,77.562976));

        // END: Airport, YVR
        routePlan.addWaypoint(new GeoCoordinate(13.1187344,77.5765951));

        // 5. Retrieve Routing information via CoreRouter.Listener
        coreRouter.calculateRoute(routePlan, routerListener);
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
