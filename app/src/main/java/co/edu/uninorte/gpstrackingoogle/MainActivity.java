package co.edu.uninorte.gpstrackingoogle;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import co.edu.uninorte.gpstrackingoogle.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, ResultCallback<LocationSettingsResult> {
    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 30000;
    protected static final String TAG = "TrackingActivity";
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    protected static final int REQUEST_PERMISSIONS = 89;
    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;
    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;
    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    protected LocationSettingsRequest mLocationSettingsRequest;

    ActivityMainBinding mainBinding;
    Status status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        buildGoogleApiClient();
        createLocationRequest();
        buildLocationSettingsRequest();

    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * <p>
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {

        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        //    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


    }

    protected void buildLocationSettingsRequest() {

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        mLocationSettingsRequest = builder.build();


    }


    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(
                mGoogleApiClient,
                mLocationSettingsRequest
        );//Checkea que la configuracion del celular sea la adecuadaa (Sin embargo todavia no se ha dado permisos a la aplicacion)


        result.setResultCallback(this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "API CONECTADA");

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(
                mGoogleApiClient,
                mLocationSettingsRequest
        );//Checkea que la configuracion del celular sea la adecuadaa (Sin embargo todavia no se ha dado permisos a la aplicacion)


        result.setResultCallback(this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Conexion API Suspendida");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public void EndTrack(View view) {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);

    }

    public void BeginTrack(View view) {

        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));


    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        status = locationSettingsResult.getStatus();
        Log.d(TAG, "OnResult " + status.getStatusCode());
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                if (mGoogleApiClient.isConnected()) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "No tiene permisos", Toast.LENGTH_LONG).show();
                        String[] permisos = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
                        ActivityCompat.requestPermissions(this, permisos, REQUEST_PERMISSIONS);
                        return;
                    }
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                }
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                Toast.makeText(this, "RESOLUTION_REQUIRED", Toast.LENGTH_LONG).show();
                try {
                    status.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);//Activar los servicios en el CELULAR
                    //(pero todavia no ha pedido permisos para usarlos la app
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Toast.makeText(this, "SETTINGS_CHANGE_UNAVAILABLE", Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permisos Obtenidos", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "No TIENES PERMISOS :C", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {

            // Check for the integer request code originally supplied to startResolutionForResult().

            case REQUEST_CHECK_SETTINGS:

                switch (resultCode) {

                    case Activity.RESULT_OK:
                        if (mGoogleApiClient.isConnected()) {
                            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                Toast.makeText(this, "No tiene permisos", Toast.LENGTH_LONG).show();
                                String[] permisos = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
                                ActivityCompat.requestPermissions(this, permisos, REQUEST_PERMISSIONS);

                                return;
                            }
                            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                        }
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        break;

                    case Activity.RESULT_CANCELED:
                        try {
                            status.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);//Activar los servicios en el CELULAR
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                        }

                        Log.i(TAG, "User chose not to make required location settings changes.");
                        break;

                }

                break;

        }

    }

    @Override
    public void onLocationChanged(Location location) {
        mainBinding.LatitudTbx.setText(location.getLatitude()+"");
        mainBinding.LongitudTbx.setText(location.getLongitude()+"");

    }

    @Override
    protected void onStop() {
        super.onStop();

    }


}
