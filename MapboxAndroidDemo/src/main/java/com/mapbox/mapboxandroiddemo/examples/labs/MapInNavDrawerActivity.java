package com.mapbox.mapboxandroiddemo.examples.labs;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.mapbox.mapboxandroiddemo.MainActivity;
import com.mapbox.mapboxandroiddemo.R;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapFragment;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

/**
 * Show a Mapbox map in the Android-system Navigtion Drawer
 */
public class MapInNavDrawerActivity extends AppCompatActivity {

  private String TAG = "MapInNavDrawerActivity";
  private DrawerLayout drawerLayout;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Mapbox access token is configured here. This needs to be called either in your application
    // object or in the same activity which contains the mapview.
    Mapbox.getInstance(this, getString(R.string.access_token));

    // This contains the MapView in XML and needs to be called after the access token is configured.
    setContentView(R.layout.activity_map_in_nav_drawer);

    drawerLayout = findViewById(R.id.drawer_layout);


    Switch navDrawerToggle = findViewById(R.id.show_map_in_nav_drawer_switch_toggle);
    navDrawerToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {

        Log.d(TAG, "onCheckedChanged: checked = " + checked);

        if (checked) {
          Intent intent = new Intent(MapInNavDrawerActivity.this, MainActivity.class);
          intent.putExtra("FROM_NAV_ACTIVITY", true);
          intent.putExtra("CHECKED_SWITCH_STATUS_FROM_NAV_ACTIVITY", checked);
          setResult(RESULT_OK, intent);
          finish();
        }
      }
    });
  }
}