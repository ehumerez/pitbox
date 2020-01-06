package com.bzgroup.pitboxauxiliovehicular.services.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bzgroup.pitboxauxiliovehicular.R;
import com.bzgroup.pitboxauxiliovehicular.ServiceRequestScreen.ServiceRequestActivity;
import com.bzgroup.pitboxauxiliovehicular.dialogs.DeniedLocationPersmissionDialogFragment;
import com.bzgroup.pitboxauxiliovehicular.entities.Service;
import com.bzgroup.pitboxauxiliovehicular.entities.Vehicle;
import com.bzgroup.pitboxauxiliovehicular.services.IServicesPresenter;
import com.bzgroup.pitboxauxiliovehicular.services.ServicesPresenter;
import com.bzgroup.pitboxauxiliovehicular.services.adapter.AdapterServices;
import com.bzgroup.pitboxauxiliovehicular.services.modals.ScheduleBottomSheetDialog;
import com.bzgroup.pitboxauxiliovehicular.utils.IOnItemClickListener;
import com.bzgroup.pitboxauxiliovehicular.utils.Permission;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ServicesActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, DeniedLocationPersmissionDialogFragment.DeniedLocationPersmissionDialogFragmentListener, IOnItemClickListener, AdapterView.OnItemSelectedListener, IServicesView, ScheduleBottomSheetDialog.ScheduleBottomSheetDialogListener {

    public static final String TAG = ServicesActivity.class.getSimpleName();

    private static final int REQUEST_CHECK_SETTINGS = 911;

    private GoogleMap mMap;

    private LocationRequest mLocationRequest;
    private LocationManager mlocationManager;

    private FusedLocationProviderClient mFusedLocationClient;

    @BindView(R.id.activity_services_spinner_myvehicles)
    Spinner activity_services_spinner_myvehicles;
    @BindView(R.id.activity_services_spinner_services)
    Spinner activity_services_spinner_services;

    @BindView(R.id.activity_services_request_btn)
    Button activity_services_request_btn;
    @BindView(R.id.activity_services_progress)
    ProgressBar activity_services_progress;
    @BindView(R.id.activity_services_base_price)
    TextView activity_services_base_price;
    @BindView(R.id.activity_services_rv)
    RecyclerView recyclerView;
    AdapterServices mAdapter;

    @BindView(R.id.activity_service_schedule)
    ImageView activity_service_schedule;

    private String categorieId;
    private boolean isSchedule;
    private IServicesPresenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_services);
        ButterKnife.bind(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        categorieId = getIntent().getStringExtra("CATEGORIE_ID");
        isSchedule = getIntent().getBooleanExtra("IS_SCHEDULE", false);

        if (!Permission.validateLocationPermissions(getApplicationContext()))
            Permission.requestLocationPermissions(this);
        else
            locationStart();

        mPresenter = new ServicesPresenter(this);
        mPresenter.onCreate();
        mPresenter.handleMyVehicles();

        setupAdapter();
        setupRecycler();
        setupSpinnerListeners();
        setupLayout();
    }

    private void setupLayout() {
        if (isSchedule)
            activity_service_schedule.setVisibility(View.VISIBLE);
        else
            activity_service_schedule.setVisibility(View.VISIBLE);
    }

    private void setupSpinnerListeners() {
        activity_services_spinner_myvehicles.setOnItemSelectedListener(this);
        activity_services_spinner_services.setOnItemSelectedListener(this);
    }

    private void setupRecycler() {
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mAdapter);
    }

    private void setupAdapter() {
        mAdapter = new AdapterServices(this, new ArrayList<>(), this);
//        requestServices();
    }


    public void locationStart() {
        if (!verificarGPS_Activo()) {
//            fragment_addresses_cv.setVisibility(View.VISIBLE);
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
            createLocationRequest();
            getCurrentLocationSetting();
        }
//        else
//            fragment_addresses_cv.setVisibility(View.GONE);
        mlocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        mlocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void getCurrentLocationSetting() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(getApplicationContext());
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                Log.d(TAG, "getCurrentLocationSetting onSuccess: ");
//                fragment_addresses_cv.setVisibility(View.GONE);
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        Log.d(TAG, "getCurrentLocationSetting onFailure: ");
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(ServicesActivity.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    public boolean verificarGPS_Activo() {
        mlocationManager = (LocationManager) Objects.requireNonNull(getApplication()).getSystemService(Context.LOCATION_SERVICE);
        return mlocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        if (currentLocation != null) {
            LatLng scz = new LatLng(currentLocation.latitude, currentLocation.longitude);
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(scz)      // Sets the center of the map to Mountain View
                    .zoom(15)                   // Sets the orientation of the camera to east
                    // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-17.781943, -63.180489), 12));

        // Add a marker in Sydney and move the camera
//        LatLng sydney = ;
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 12));

    }

    @Override
    protected void onDestroy() {
        mPresenter.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
//                        if (addressesFragment != null)
//                            addressesFragment.hideMessageEnabledGPS();
                        break;
                    case Activity.RESULT_CANCELED:
                        ServicesActivity.A s = new ServicesActivity.A("", getString(R.string.activity_service_stations_denied_location_settings), DeniedLocationPersmissionDialogFragment.DENIED_SETTINGS_FIRST_TIME);
                        s.execute();
                        break;
                    default:
                        break;
                }
                break;
            case REQUEST_PERMISSION_SETTING:
                Log.d(TAG, "onActivityResult: REQUEST_PERMISSION_SETTING");
                if (!Permission.validateLocationPermissions(this)) {
//                    Permission.requestLocationPermissions(this);
                    A s = new A("fsdfsdfsdfs", getString(R.string.activity_service_stations_denied_location_permission_never_ask_again), DeniedLocationPersmissionDialogFragment.DENIED_MORE_TIME);
                    s.execute();
                } else {
                    locationStart();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: requestCode " + requestCode + " permissions " + permissions.toString() + " grantResults " + grantResults.toString());
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Permission.MULTIPLE_PERMISSIONS_REQUEST_CODE:
                // Verifica si todos los permisos se aceptaron o no
                if (Permission.validatePermissions(getApplicationContext(), grantResults)) {
                    locationStart();
                    //Si todos los permisos fueron aceptados continua con el flujo normal
                } else if (Permission.validateLocationPermissions(getApplicationContext()))
                    locationStart();

                else {
                    //Si algun permiso fue rechazado no se puede continuar
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {
                        // Mostrar una extensión al usuario * de forma asíncrona * - no bloquear
                        // este hilo esperando la respuesta del usuario! Despues del usuario
                        // ve la explicación, intente nuevamente para solicitar el permiso.
                        ServicesActivity.A s = new ServicesActivity.A("", getString(R.string.activity_service_stations_denied_location_permission), DeniedLocationPersmissionDialogFragment.DENIED_FIRST_TIME);
                        s.execute();
                    } else {
                        // Don't ask again check
                        ServicesActivity.A s = new ServicesActivity.A("", getString(R.string.activity_service_stations_denied_location_permission_never_ask_again), DeniedLocationPersmissionDialogFragment.DENIED_MORE_TIME);
                        s.execute();
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    boolean locationB = true;
    private LatLng currentLocation;

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: ");
        if (locationB) {
            if (mMap != null) {
                Log.d(TAG, "onLocationChanged: mMap != null");
                mMap.getUiSettings().setZoomControlsEnabled(true);
                mMap.getUiSettings().setCompassEnabled(true);
                mMap.setMyLocationEnabled(true);
                LatLng scz = new LatLng(location.getLatitude(), location.getLongitude());
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(scz)      // Sets the center of the map to Mountain View
                        .zoom(15)                   // Sets the tilt of the camera to 30 degrees
                        .build();                   // Creates a CameraPosition from the builder
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                currentLocation = cameraPosition.target;
//            mPresenter.handleServiceStations();
            }
            locationB = false;
        }
        if (mlocationManager != null) {
            mlocationManager.removeUpdates(this);
            mlocationManager = null;
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, int id) {
        switch (id) {
            case DeniedLocationPersmissionDialogFragment.DENIED_FIRST_TIME:
                Permission.requestLocationPermissions(this);
                break;
            case DeniedLocationPersmissionDialogFragment.DENIED_MORE_TIME:
                openSettings();
                break;
            case DeniedLocationPersmissionDialogFragment.DENIED_SETTINGS_FIRST_TIME:
                locationStart();
                break;
        }
    }

    private static final int REQUEST_PERMISSION_SETTING = 159;

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {

    }

    private Service service;

    @Override
    public void onItemClick(Object object) {
//        if (object instanceof Service) {
//            service = (Service) object;
//            activity_services_base_price.setText(service.getPrecioBase());
//        }
    }

    @Override
    public void onItemSelected(Object object) {

    }

    @Override
    public void showProgress() {
        activity_services_progress.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        activity_services_progress.setVisibility(View.GONE);
    }

    @Override
    public void providerMyVehicles(List<Vehicle> myVehicles) {
        ArrayAdapter<Vehicle> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.simple_spinner_dropdown_item_eliot, myVehicles);
//        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_eliot);
        activity_services_spinner_myvehicles.setAdapter(adapter);
    }

    @Override
    public void providerServices(List<Service> services) {
        ArrayAdapter<Service> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.simple_spinner_dropdown_item_eliot, services);
//        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_eliot);
        activity_services_spinner_services.setAdapter(adapter);
    }

    @Override
    public void providerServicesEmpty() {

    }

    @Override
    public void providerMyVehiclesEmpty() {

    }

    private void enabledInputs() {
        activity_services_request_btn.setEnabled(true);
    }

    private void disabledInputs() {
        activity_services_request_btn.setEnabled(false);
    }

    @OnClick(R.id.activity_services_request_btn)
    public void handleServiceRequest() {
        showProgress();
        disabledInputs();
        if (service != null && currentLocation != null) {
            Log.d(TAG, "handleServiceRequest currentLocation: " + currentLocation.toString());
            Log.d(TAG, "handleServiceRequest service: " + service.toString());
            startActivity(new Intent(getApplicationContext(), ServiceRequestActivity.class));
            finish();
        }
    }

    @OnClick(R.id.activity_service_schedule)
    public void handleScheduleService() {
        ScheduleBottomSheetDialog scheduleBottomSheetDialog = new ScheduleBottomSheetDialog();
        scheduleBottomSheetDialog.show(getSupportFragmentManager(), "scheduleBottomSheetDialog");
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int spinnerId = parent.getId();
        Object object = parent.getItemAtPosition(position);
        switch (spinnerId) {
            case R.id.activity_services_spinner_myvehicles:
                if (object instanceof Vehicle) {
                    mPresenter.handleServices(categorieId);
                }
                break;
            case R.id.activity_services_spinner_services:
                if (object instanceof Service) {
                    service = (Service) object;
                    activity_services_base_price.setText(service.getPrecioBase());
                }
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onScheduleButtonClick(String time, String data) {
        Toast.makeText(this, time + " / " + data, Toast.LENGTH_SHORT).show();
    }

    public class A extends AsyncTask<Void, Void, Void> {

        private String mTitle, mBody;
        private int mId;

        public A(String title, String body, int id) {
            mTitle = title;
            mBody = body;
            mId = id;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            showDeniedLocationDialog(mTitle, mBody, mId);
            return null;
        }
    }

    public void showDeniedLocationDialog(String title, String body, int id) {
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = new DeniedLocationPersmissionDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString("DENIED_LOCATION_PERMISSION", body);
        bundle.putInt("DENIED_LOCATION_PERMISSION_ID", id);
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), "DeniedLocationPersmissionDialogFragment");
        dialog.setCancelable(false);
    }
}
