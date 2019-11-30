package com.mapbox.mapboxandroiddemo.examples.labs;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.mapbox.api.matching.v5.MapboxMapMatching;
import com.mapbox.api.matching.v5.models.MapMatchingMatching;
import com.mapbox.api.matching.v5.models.MapMatchingResponse;
import com.mapbox.core.exceptions.ServicesException;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxandroiddemo.R;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.api.directions.v5.DirectionsCriteria.GEOMETRY_POLYLINE6;
import static com.mapbox.api.directions.v5.DirectionsCriteria.PROFILE_CYCLING;
import static com.mapbox.api.directions.v5.DirectionsCriteria.PROFILE_DRIVING;
import static com.mapbox.api.directions.v5.DirectionsCriteria.PROFILE_WALKING;
import static com.mapbox.core.constants.Constants.PRECISION_6;
import static com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_ROUND;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

/**
 * Use the Android system {@link View.OnTouchListener} to draw
 * an polygon and/or a line. Also perform a search for data points within the drawn polygon area.
 */
public class DragToDrawMapMatchedRouteActivity extends AppCompatActivity {

  private static final String TAG = "DragToDrawMapMatchedRouteActivity";
  private static final String FREEHAND_DRAW_LINE_LAYER_SOURCE_ID = "FREEHAND_DRAW_LINE_LAYER_SOURCE_ID";
  private static final String FREEHAND_DRAW_LINE_LAYER_ID = "FREEHAND_DRAW_LINE_LAYER_ID";

  // Adjust the static final variables to change the example's UI
  private static final String LINE_COLOR = "#e60800";
  private static final float LINE_WIDTH = 5f;
  private static final float LINE_OPACITY = 1f;
  private static final double DESIRED_MATCH_RADIUS = 45f;
  private String desiredProfile = PROFILE_DRIVING;

  private MapView mapView;
  private MapboxMap mapboxMap;
  private FloatingActionButton clearMapFab;
  private List<Point> freehandDrawingPointList = new ArrayList<>();
  private List<Point> finalChainedMatchPointList = new ArrayList<>();
  private List<Point> unchainedMatchPointList = new ArrayList<>();
  private int index = 0;
  private int numberOf100MultiplesInTotalDrawnPoints;
  private int remainingPoints;

  private View.OnTouchListener customOnTouchListener = new View.OnTouchListener() {
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

      LatLng latLngTouchCoordinate = mapboxMap.getProjection().fromScreenLocation(
          new PointF(motionEvent.getX(), motionEvent.getY()));

      freehandDrawingPointList.add(
          Point.fromLngLat(latLngTouchCoordinate.getLongitude(),
              latLngTouchCoordinate.getLatitude())
      );

      mapboxMap.getStyle(style -> {
        // Draw the line on the map as the finger is dragged along the map
        GeoJsonSource drawLineSource = style.getSourceAs(FREEHAND_DRAW_LINE_LAYER_SOURCE_ID);
        if (drawLineSource != null) {
          drawLineSource.setGeoJson(LineString.fromLngLats(freehandDrawingPointList));
        }

        // Take next actions when the on-screen drawing is finished
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
          findViewById(R.id.loading_route_progress_spinner).setVisibility(View.VISIBLE);

          Log.d(TAG, "onStyleLoaded: freehandDrawingPointList.size() = " + freehandDrawingPointList.size());

          if (freehandDrawingPointList.size() < 100) {
            requestMapMatched(freehandDrawingPointList, false, false);
            setUiAfterMapMatching();
          } else {
            numberOf100MultiplesInTotalDrawnPoints = freehandDrawingPointList.size() / 100;
            remainingPoints = freehandDrawingPointList.size() - numberOf100MultiplesInTotalDrawnPoints * 100;
            Log.d(TAG, "onStyleLoaded: numberOf100MultiplesInTotalDrawnPoints = " + numberOf100MultiplesInTotalDrawnPoints);

            Log.d(TAG, "onStyleLoaded: freehandDrawingPointList.size()%100 = " + freehandDrawingPointList.size() % 100);

            makeLinkedCall(index);


          }
        }
      });
      return true;
    }
  };

  private void makeLinkedCall(int indexMultiple) {
    List<Point> pointListToMatch = freehandDrawingPointList.subList(indexMultiple == 0
        ? indexMultiple * 100 : indexMultiple * 100 + 1, indexMultiple * 100 + 100);
    Log.d(TAG, "makeLinkedCall: pointListToMatch = " + pointListToMatch.size());
    requestMapMatched(pointListToMatch, true, false);
  }

  private void makeMatchCallForRemainingPoints() {
    if (freehandDrawingPointList.size() % 100 != 0) {
      Log.d(TAG, "makeMatchCallForRemainingPoints: numberOf100MultiplesInTotalDrawnPoints * 100 + 1 = " +
          numberOf100MultiplesInTotalDrawnPoints * 100 + 1);

      Log.d(TAG, "makeMatchCallForRemainingPoints: freehandDrawingPointList.size() - numberOf100MultiplesInTotalDrawnPoints * 100 = " +
          String.valueOf(freehandDrawingPointList.size() - numberOf100MultiplesInTotalDrawnPoints * 100));

      List<Point> remainingModuloPointsToMatch = freehandDrawingPointList.subList(
          numberOf100MultiplesInTotalDrawnPoints * 100 + 1,
          freehandDrawingPointList.size() - numberOf100MultiplesInTotalDrawnPoints * 100);
      requestMapMatched(remainingModuloPointsToMatch, true, true);
    }
  }

  private void setUiAfterMapMatching() {
    Toast.makeText(DragToDrawMapMatchedRouteActivity.this,
        getString(R.string.move_map_explore_map_matched_route), Toast.LENGTH_SHORT).show();
    enableMapMovement();
    clearMapFab.setVisibility(View.VISIBLE);
    findViewById(R.id.loading_route_progress_spinner).setVisibility(View.INVISIBLE);
    freehandDrawingPointList = new ArrayList<>();
    finalChainedMatchPointList = new ArrayList<>();
  }

  @SuppressLint("LongLogTag")
  private void requestMapMatched(List<Point> points, boolean chainingMatchedPolylineTogether,
                                 boolean finalChainedCall) {
    Log.d(TAG, "requestMapMatched: call points size = " + points.size());
    try {
      Double[] radiusArray = new Double[points.size()];
      for (int x = 0; x < points.size(); x++) {
        radiusArray[x] = DESIRED_MATCH_RADIUS;
      }
      MapboxMapMatching client = MapboxMapMatching.builder()
          .accessToken(Mapbox.getAccessToken())
          .profile(desiredProfile)
          .coordinates(points)
          .tidy(true)
          .radiuses(radiusArray)
          .geometries(GEOMETRY_POLYLINE6)
          .build();

      // Execute the API call and handle the response.
      client.enqueueCall(new Callback<MapMatchingResponse>() {
        @SuppressLint({"LogNotTimber", "LongLogTag"})
        @Override
        public void onResponse(@NonNull Call<MapMatchingResponse> call,
                               @NonNull Response<MapMatchingResponse> response) {

          if (response.isSuccessful() && response.body() != null) {

            try {

              List<MapMatchingMatching> matchings = response.body().matchings();

              Log.d(TAG, "onResponse: number of matchings = " + matchings.size());

              LineString lineStringFromApiResponse = LineString.fromPolyline(Objects.requireNonNull(
                  matchings.get(0).geometry()), PRECISION_6);

              if (!chainingMatchedPolylineTogether) {
                unchainedMatchPointList.addAll(lineStringFromApiResponse.coordinates());
              } else {
                enableMapMovement();
                DragToDrawMapMatchedRouteActivity.this.finalChainedMatchPointList.addAll(lineStringFromApiResponse.coordinates());
                if (index == numberOf100MultiplesInTotalDrawnPoints) {
                  if (finalChainedCall) {
                    displayLineStringOnMap(LineString.fromLngLats(finalChainedMatchPointList));
                    setUiAfterMapMatching();
                  } else {
                    makeMatchCallForRemainingPoints();
                  }
                } else {
                  index++;
                  Log.d(TAG, "onResponse: index = " + index);
                  makeLinkedCall(index);
                }
              }

              if (!chainingMatchedPolylineTogether) {
                displayLineStringOnMap(LineString.fromLngLats(unchainedMatchPointList));
                DragToDrawMapMatchedRouteActivity.this.unchainedMatchPointList = new ArrayList<>();
                DragToDrawMapMatchedRouteActivity.this.finalChainedMatchPointList = new ArrayList<>();
                Toast.makeText(DragToDrawMapMatchedRouteActivity.this,
                    R.string.drawn_route_now_matched, Toast.LENGTH_SHORT).show();
              }

            } catch (NullPointerException nullPointerException) {
              Log.d(TAG, "onResponse: nullPointerException = " + nullPointerException);
            }
          } else {
            // If the response code does not response "OK" an error has occurred.
            Log.d(TAG, "MapboxMapMatching failed with " + response.code());
          }
        }

        @Override
        public void onFailure(Call<MapMatchingResponse> call, Throwable throwable) {
          Log.d(TAG, "throwable = " + throwable.toString());
          Log.d(TAG, "throwable call = " + call.request().url().toString());
        }
      });
    } catch (ServicesException servicesException) {
      Log.d(TAG, "servicesException = " + servicesException.toString());
    }
  }


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Mapbox access token is configured here. This needs to be called either in your application
    // object or in the same activity which contains the mapview.
    Mapbox.getInstance(this, getString(R.string.access_token));

    // This contains the MapView in XML and needs to be called after the access token is configured.
    setContentView(R.layout.activity_lab_drag_to_draw_map_matched_route);

    mapView = findViewById(R.id.mapView);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(new OnMapReadyCallback() {
      @Override
      public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        mapboxMap.setStyle(new Style.Builder().fromUri(Style.MAPBOX_STREETS)
                .withSource(new GeoJsonSource(FREEHAND_DRAW_LINE_LAYER_SOURCE_ID))
            , new Style.OnStyleLoaded() {
              @Override
              public void onStyleLoaded(@NonNull Style style) {

                DragToDrawMapMatchedRouteActivity.this.mapboxMap = mapboxMap;

                LineLayer drawnAndMatchedLineLayer = new LineLayer(FREEHAND_DRAW_LINE_LAYER_ID,
                    FREEHAND_DRAW_LINE_LAYER_SOURCE_ID).withProperties(
                    lineWidth(LINE_WIDTH),
                    lineJoin(LINE_JOIN_ROUND),
                    lineOpacity(LINE_OPACITY),
                    lineColor(Color.parseColor(LINE_COLOR)));

                if (style.getLayer("road-label") != null) {
                  style.addLayerBelow(drawnAndMatchedLineLayer, "road-label");
                } else {
                  style.addLayer(drawnAndMatchedLineLayer);
                }

                enableRouteDrawing();

                Toast.makeText(DragToDrawMapMatchedRouteActivity.this,
                    getString(R.string.draw_route_instruction), Toast.LENGTH_SHORT).show();

                initProfileSelectionFabs();

                clearMapFab = findViewById(R.id.clear_map_for_new_draw_fab);
                clearMapFab.setOnClickListener(new View.OnClickListener() {
                  @Override
                  public void onClick(View view) {
                    clearMapFab.setVisibility(View.INVISIBLE);

                    resetAllLists();

                    // Add empty Feature array to the sources
                    GeoJsonSource drawLineSource = style.getSourceAs(FREEHAND_DRAW_LINE_LAYER_SOURCE_ID);
                    if (drawLineSource != null) {
                      drawLineSource.setGeoJson(FeatureCollection.fromFeatures(new Feature[]{}));
                    }

                    enableRouteDrawing();
                  }
                });
              }
            });
      }
    });
  }

  private void resetAllLists() {
    freehandDrawingPointList = new ArrayList<>();
    finalChainedMatchPointList = new ArrayList<>();
    unchainedMatchPointList = new ArrayList<>();
  }

  /**
   * Enable moving the map
   */
  private void enableMapMovement() {
    mapView.setOnTouchListener(null);
  }

  /**
   * Enable drawing on the map by setting the custom touch listener on the {@link MapView}
   */
  private void enableRouteDrawing() {
    mapView.setOnTouchListener(customOnTouchListener);
  }


  private void displayLineStringOnMap(@NonNull LineString lineString) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        GeoJsonSource drawLineSource = style.getSourceAs(FREEHAND_DRAW_LINE_LAYER_SOURCE_ID);
        if (drawLineSource != null) {
          drawLineSource.setGeoJson(Feature.fromGeometry(lineString));
        }
      }
    });
  }

  private void initProfileSelectionFabs() {
    findViewById(R.id.cycling_profile_fab).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        desiredProfile = PROFILE_CYCLING;
        resetAllLists();
      }
    });
    findViewById(R.id.walking_profile_fab).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        desiredProfile = PROFILE_WALKING;
        resetAllLists();
      }
    });
    findViewById(R.id.driving_profile_fab).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        desiredProfile = PROFILE_DRIVING;
        resetAllLists();
      }
    });
  }

  // Add the mapView lifecycle to the activity's lifecycle methods
  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }
}
