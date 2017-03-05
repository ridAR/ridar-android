package com.techgeekme.ridar;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.MapEngine;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Maneuver;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RoutingError;
import com.here.android.mpa.search.GeocodeRequest;
import com.here.android.mpa.search.Location;
import com.here.android.mpa.search.ResultListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.List;
import java.util.UUID;

public class NavActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks {

    // map embedded in the map fragment
    private Map map = null;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // map fragment embedded in this activity
    private MapFragment mapFragment = null;

    private static final String TAG = "NavActivity";

    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    private static final int LEDSTRAIGHT = 9;
    private static final int LEDDOWN = 10;
    private static final int LEDLEFT = 11;
    private static final int LEDRIGHT = 12;
    private static final int LEDSLIGHTLEFT = 13;
    private static final int LEDSLIGHTRIGHT = 14;
    private static final int LEDHARDLEFT = 15;
    private static final int LEDHARDRIGHT = 16;

    private BluetoothAdapter btAdapter = null;

    private boolean helmetConnected;

    private static String address = "98:D3:31:40:A6:D7";


    // TextView for displaying the current map scheme
    private TextView textViewResult = null;
    // MapRoute for this activity
    private MapRoute mapRoute = null;

    private NavigationManager navigationManager = null;
    private PositioningManager positionManager = null;
    private long EDA = -1;
    private Boolean StateBlink = false;
    private Boolean StateSolid = false;
    private Boolean KeepSideFlag = false;
    private int LEDTOBLINK = 0;
    private ResultListener<List<Location>> listener = null;
    private GeocodeRequest request = null;

    private RequestQueue mRequestQueue;
    private EditText placeEditText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Calling oncreate");
        setContentView(R.layout.activity_nav);
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.MAC_MESSAGE);
        Toast.makeText(this, message, Toast.LENGTH_SHORT);
        address = message;

        placeEditText = (EditText) findViewById(com.techgeekme.ridar.R.id.placeEditText);

        textViewResult = (TextView) findViewById(com.techgeekme.ridar.R.id.title);
        textViewResult.setText(com.techgeekme.ridar.R.string.textview_routecoordinates_2waypoints);
        noLocation();
        setupBluetoothConnection();
        initialize();
    }

    private GoogleApiClient googleApiClient;

    final static int REQUEST_LOCATION = 199;

    private boolean initialized;

//     check whether gps is enabled
    public boolean noLocation() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            //  buildAlertMessageNoGps();

            enableLoc();
            return true;
        }
        return false;

    }


    private void enableLoc() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(com.google.android.gms.location.LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult connectionResult) {

                            Log.v(TAG, "Location error " + connectionResult.getErrorCode());
                        }
                    }).build();
            googleApiClient.connect();

            com.google.android.gms.location.LocationRequest locationRequest = com.google.android.gms.location.LocationRequest.create().create();
            locationRequest.setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(30 * 1000);
            locationRequest.setFastestInterval(5 * 1000);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);

            builder.setAlwaysShow(true);

            PendingResult<LocationSettingsResult> result =
                    com.google.android.gms.location.LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                status.startResolutionForResult(
                                        NavActivity.this, REQUEST_LOCATION);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                    }
                }
            });
        }
    }

    private void initialize() {

        Log.i(TAG, "CALLING INITIALIZE");

        // Search for the map fragment to finish setup by calling init().
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(
                R.id.mapfragment);

        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(
                    OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    initialized = true;
                    // retrieve a reference of the map from the map fragment
                    map = mapFragment.getMap();
                    // Set the map center to the Vancouver region (no animation)
                    map.setCenter(new GeoCoordinate(49.196261, -123.004773, 0.0),
                            Map.Animation.NONE);
                    // Set the zoom level to the average between min and max
//                    map.setZoomLevel(
//                            (map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);

                    map.setZoomLevel(17);

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

    private NavigationManager.NewInstructionEventListener newInstructionEventListener = new NavigationManager.NewInstructionEventListener() {

        @Override
        public void onNewInstructionEvent() {
            Maneuver maneuver = navigationManager.getNextManeuver();
            if (maneuver != null) {
                if (maneuver.getAction() == Maneuver.Action.END) {
                    //notify the user that the route is complete
                } else {

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
                    } else if (maneuvarGetTurn.equals("UNDEFINED")) {
                        LEDTOBLINK = LEDDOWN;
                    }


                    Log.d("DBGRIDAR", "SIGNAL-STOP");
                    sendBluetoothSignal(0);
                    StateBlink = false;
                    StateSolid = false;
                }
            }
        }
    };

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

    private void sendBluetoothSignal(int message) {

        Log.d(TAG, "...Send data: " + message + "...");

        try {
            outStream.write((byte) message);
        } catch (IOException e) {
            String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
            if (address.equals("00:00:00:00:00:00"))
                msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 35 in the java code";
            msg = msg + ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";

            errorExit("Fatal Error", msg);
        }
    }


    private NavigationManager.PositionListener positionListener = new NavigationManager.PositionListener() {
        @Override
        public void onPositionUpdated(GeoPosition geoPosition) {
            super.onPositionUpdated(geoPosition);

            map.setCenter(geoPosition.getCoordinate(), Map.Animation.NONE);

            EDA = navigationManager.getNextManeuverDistance();
            //Log.d("DBGRIDAR:",String.valueOf(EDA));


            if (StateSolid == false) {
                if (EDA < 50 && KeepSideFlag == false) {
                    //SOLID
                    LEDTOBLINK = LEDTOBLINK - 8;
                    Log.d("DBGRIDAR:", "SIGNALSOLID - " + LEDTOBLINK);
                    sendBluetoothSignal(LEDTOBLINK);
                    StateBlink = false;
                    StateSolid = true;
                } else if (EDA < 100 && StateBlink == false) {
                    //BLINK
                    Log.d("DBGRIDAR:", "SIGNALBLINK - " + LEDTOBLINK);
                    sendBluetoothSignal(LEDTOBLINK);
                    StateBlink = true;
                }
            }
        }
    };

    private CoreRouter.Listener routerListener = new CoreRouter.Listener() {
        public void onCalculateRouteFinished(List<RouteResult> routeResults, RoutingError errorCode) {

            if (errorCode == RoutingError.NONE && routeResults.get(0).getRoute() != null) {
                // create a map route object and place it on the map
                mapRoute = new MapRoute(routeResults.get(0).getRoute());
                map.addMapObject(mapRoute);

                // Get the bounding box containing the route and zoom in
                GeoBoundingBox gbb = routeResults.get(0).getRoute().getBoundingBox();
                map.zoomTo(gbb, Map.Animation.NONE, Map.MOVE_PRESERVE_ORIENTATION);
                map.setZoomLevel(17);
                textViewResult.setText(
                        String.format("Route calculated with %d maneuvers.",
                                routeResults.get(0).getRoute().getManeuvers().size()));

                navigationManager.setMap(map);

                navigationManager.addPositionListener(new WeakReference<NavigationManager.PositionListener>(positionListener));
                navigationManager.addNewInstructionEventListener(new WeakReference<NavigationManager.NewInstructionEventListener>(newInstructionEventListener));
                NavigationManager.Error error = navigationManager.simulate(routeResults.get(0).getRoute(), 16);
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


    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }

        return mRequestQueue;
    }


    private JsonObjectRequest getJsonObjectRequest(String place) {
        String placeUrl = "https://maps.googleapis.com/maps/api/geocode/json?address=" + URLEncoder.encode(place) + "&key=AIzaSyAkhlJ3RA5B9nQYlKPIeXoiOVP456-QKIQ";
        return new JsonObjectRequest(placeUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.i(TAG, "JSON Response: " + response);
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
        if (positionManager.getPosition().isValid()) {
            Log.d("DBGRIDART", positionManager.getPosition().getCoordinate().toString());
            routePlan.addWaypoint(new GeoCoordinate(positionManager.getPosition().getCoordinate()));
            routePlan.addWaypoint(new GeoCoordinate(lat, lng)); //VidhanSoudha
            coreRouter.calculateRoute(routePlan, routerListener);
        } else {
            Toast.makeText(this, "Current location not acquired", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        sendBluetoothSignal(0);
        navigationManager.stop();
        if (outStream != null) {
            try {
                outStream.flush();
            } catch (IOException e) {
                errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
            }
        }

        try {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }

        super.onDestroy();

    }

    private void errorExit(String title, String message) {
        Log.d(TAG, title + " - " + message);
        finish();
    }

    public void setupBluetoothConnection() {

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        // TODO: Scan for the remote device
        // Set up a pointer to the remote node using it's address.
        BluetoothDevice remoteDevice = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.
        try {
            btSocket = createBluetoothSocket(remoteDevice);
        } catch (IOException e1) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e1.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
//        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting...");
        try {
            btSocket.connect();
            Log.d(TAG, "...Connection ok...");
            Toast.makeText(this, "Connected to helmet!", Toast.LENGTH_SHORT).show();
            helmetConnected = true;
        } catch (IOException e) {
            try {
                // TODO: Reattempt Do not go through
                btSocket.close();
                Toast.makeText(this, "Turn on helmet!", Toast.LENGTH_SHORT).show();
                helmetConnected = false;
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Create Socket...");

        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice remoteDevice) throws IOException {
        if (Build.VERSION.SDK_INT >= 10) {
            try {
                final Method m = remoteDevice.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[]{UUID.class});
                return (BluetoothSocket) m.invoke(remoteDevice, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection", e);
                Toast.makeText(this, "Could not connect to helmet", Toast.LENGTH_SHORT).show();
            }
        }
        return remoteDevice.createRfcommSocketToServiceRecord(MY_UUID);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_LOCATION:
                switch (resultCode) {
                    case Activity.RESULT_CANCELED: {
                        // The user was asked to change settings, but chose not to
                        finish();
                        break;
                    }
                    default: {
                        break;
                    }
                }
                break;
        }

    }
}
