package com.ridar.akhilraj.ridar;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.GeocodeRequest;
import com.here.android.mpa.search.Location;
import com.here.android.mpa.search.ResultListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {

    private  int LEDSTRAIGHT = 9;
    private  int LEDDOWN = 10;
    private  int LEDLEFT = 11;
    private  int LEDRIGHT = 12;
    private  int LEDSLIGHTLEFT = 13;
    private  int LEDSLIGHTRIGHT = 14;
    private  int LEDHARDLEFT = 15;
    private  int LEDHARDRIGHT = 16;

    // map embedded in the map fragment
    private Map map = null;
    // map fragment embedded in this activity
    private MapFragment mapFragment = null;
    // TextView for displaying the current map scheme
    private TextView textViewResult = null;
    // MapRoute for this activity
    private MapRoute mapRoute = null;

    private  NavigationManager navigationManager = null;
    private PositioningManager positionManager = null;
    private long EDA = -1;
    private Boolean StateBlink = false;
    private Boolean StateSolid = false;
    private Boolean KeepSideFlag = false;
    private int LEDTOBLINK = 0;
    private ResultListener<List<Location>> listener=null;
    private GeocodeRequest request = null;
    private void SendBluetoohSignal(int signal){
    }
    private RequestQueue mRequestQueue;
    private EditText placeEditText;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewResult = (TextView) findViewById(R.id.title);
        textViewResult.setText(R.string.textview_routecoordinates_2waypoints);

        placeEditText = (EditText) findViewById(R.id.placeEditText);

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

                    positionManager = PositioningManager.getInstance();
                    positionManager.start(PositioningManager.LocationMethod.GPS_NETWORK);
                    positionManager.addListener(new WeakReference<PositioningManager.OnPositionChangedListener>(positionList));


                } else {
                    System.out.println("ERROR: Cannot initialize Map Fragment");
                }
            }
        });






    }


    private PositioningManager.OnPositionChangedListener positionList = new
            PositioningManager.OnPositionChangedListener() {

                public void onPositionUpdated(PositioningManager.LocationMethod method,
                                              GeoPosition position, boolean isMapMatched) {
                    // set the center only when the app is in the foreground
                    // to reduce CPU consumption

                        map.setCenter(position.getCoordinate(),
                                Map.Animation.NONE);

                }

                public void onPositionFixChanged(PositioningManager.LocationMethod method,
                                                 PositioningManager.LocationStatus status) {
                }
    };

    private NavigationManager.NewInstructionEventListener newInstructionEventListener = new NavigationManager.NewInstructionEventListener() {

        @Override
        public void onNewInstructionEvent() {
            Maneuver maneuver = navigationManager.getNextManeuver();
            if (maneuver != null) {
                if (maneuver.getAction() == Maneuver.Action.END) {
                    //notify the user that the route is complete
                }else {

                    KeepSideFlag = false;
                    LEDTOBLINK = 0;
                    String maneuvarGetTurn = maneuver.getTurn().toString();
                    Log.d("DBGRIDAR:", "GetTurn-" + maneuvarGetTurn);
                    Log.d("DBGRIDAR:", "DistanceToNextManeuvar-" + String.valueOf(maneuver.getDistanceToNextManeuver()));
                    Log.d("DBGRIDAR:", "Coordinate-" + maneuver.getCoordinate().toString());
                    Log.d("DBGRIDAR", "NextRoadName-" + maneuver.getNextRoadName());

                    if (maneuvarGetTurn.equals("LIGHT_RIGHT")) {
                        LEDTOBLINK = LEDSLIGHTRIGHT;
                    } else if (maneuvarGetTurn.equals("QUITE_RIGHT")) {
                        LEDTOBLINK = LEDRIGHT;
                    } else if (maneuvarGetTurn.equals("HEAVY_RIGHT")) {
                        LEDTOBLINK = LEDHARDRIGHT;
                    } else if (maneuvarGetTurn.equals("LIGHT_LEFT")) {
                        LEDTOBLINK = LEDSLIGHTLEFT;
                    } else if (maneuvarGetTurn.equals("QUITE_LEFT")) {
                        LEDTOBLINK = LEDLEFT;
                    } else if (maneuvarGetTurn.equals("HEAVY_LEFT")) {
                        LEDTOBLINK = LEDHARDLEFT;
                    } else if (maneuvarGetTurn.toLowerCase().contains("roundabout")) {
                        Log.d("DBGRIDAR:", "ANGLE" + String.valueOf(maneuver.getAngle()));
                        long turnangle = maneuver.getAngle();

                        if (turnangle < 68) {
                            LEDTOBLINK = LEDHARDLEFT;
                        } else if (turnangle < 113) {
                            LEDTOBLINK = LEDLEFT;
                        } else if (turnangle < 158) {
                            LEDTOBLINK = LEDSLIGHTLEFT;
                        } else if (turnangle < 203) {
                            LEDTOBLINK = LEDSTRAIGHT;
                        } else if (turnangle < 248) {
                            LEDTOBLINK = LEDRIGHT;
                        } else if (turnangle < 293) {
                            LEDTOBLINK = LEDSLIGHTRIGHT;
                        } else if (turnangle < 293) {
                            LEDTOBLINK = LEDHARDRIGHT;
                        }
                    } else if (maneuvarGetTurn.equals("KEEP_RIGHT")) {
                        LEDTOBLINK = LEDRIGHT;
                        KeepSideFlag = true;
                    } else if (maneuvarGetTurn.equals("KEEP_LEFT")) {
                        LEDTOBLINK = LEDLEFT;
                        KeepSideFlag = true;
                    }


                    Log.d("DBGRIDAR", "SIGNAL-STOP");
                    SendBluetoohSignal(0);
                    StateBlink = false;
                    StateSolid = false;
                }
            }
        }
    };


    private NavigationManager.PositionListener positionListener = new NavigationManager.PositionListener() {
        @Override
        public void onPositionUpdated(GeoPosition geoPosition) {
            super.onPositionUpdated(geoPosition);

            map.setCenter(geoPosition.getCoordinate(), Map.Animation.NONE);

            EDA = navigationManager.getNextManeuverDistance();
            //Log.d("DBGRIDAR:",String.valueOf(EDA));


            if(StateSolid == false){
                if(EDA<50 && KeepSideFlag == false){
                   //SOLID
                    LEDTOBLINK = LEDTOBLINK-8;
                    Log.d("DBGRIDAR:","SIGNALSOLID - "+LEDTOBLINK);
                    SendBluetoohSignal(LEDTOBLINK);
                    StateBlink = false;
                    StateSolid = true;
               }else if(EDA<200 && StateBlink==false){
                   //BLINK
                    Log.d("DBGRIDAR:","SIGNALBLINK - "+LEDTOBLINK);
                    SendBluetoohSignal(LEDTOBLINK);
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

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }

        return mRequestQueue;
    }

    // Functionality for taps of the "Get Directions" button
    public void getDirections(View view) {
        // 1. clear previous results
        textViewResult.setText("");
        if (map != null && mapRoute != null) {
            map.removeMapObject(mapRoute);
            mapRoute = null;
        }

        String enteredPlace = placeEditText.getText().toString();
        JsonObjectRequest placeLocationRequest = getJsonObjectRequest(enteredPlace);
        RequestQueue rq = getRequestQueue();
        rq.add(placeLocationRequest);
        rq.start();



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


    private JsonObjectRequest getJsonObjectRequest(String place) {
        String placeUrl ="https://maps.googleapis.com/maps/api/geocode/json?address=" + place + "&key=AIzaSyD-9JcB-zqw7hlQ8NE4Th07y-c1Sm6BAIE";
        return new JsonObjectRequest(placeUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray resultsArray = response.getJSONArray("results");
                            JSONObject firstPlace = resultsArray.getJSONObject(0);
                            JSONObject geometry = firstPlace.getJSONObject("geometry");
                            JSONObject location = geometry.getJSONObject("location");
                            Double lat = location.getDouble("lat");
                            Double lng = location.getDouble("lng");
                            onPlaceLocationReceived(lat, lng);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
    }

    private void onPlaceLocationReceived(Double lat, Double lng) {
        Log.d("MainActivity", "Lat" + lat + "Lng" + lng);

        CoreRouter coreRouter = new CoreRouter();
        RoutePlan routePlan = new RoutePlan();
        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);
        routePlan.setRouteOptions(routeOptions);
        Log.d("DBGRIDART", positionManager.getPosition().getCoordinate().toString());
        routePlan.addWaypoint(new GeoCoordinate(positionManager.getPosition().getCoordinate()));
        routePlan.addWaypoint(new GeoCoordinate(lat,lng)); //VidhanSoudha
        coreRouter.calculateRoute(routePlan, routerListener);
    }
}

