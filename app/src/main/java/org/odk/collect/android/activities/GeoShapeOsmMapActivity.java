/*
 * Copyright (C) 2016 GeoODK
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import org.odk.collect.android.R;
import org.odk.collect.android.spatial.MapHelper;
import org.odk.collect.android.widgets.GeoShapeWidget;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

/**
 * Version of the GeoPointMapActivity that uses the new Maps v2 API and Fragments to enable
 * specifying a location via placing a tracker on a map.
 *
 * @author jonnordling@gmail.com
 */


public class GeoShapeOsmMapActivity extends Activity implements IRegisterReceiver {
    private MapView mMap;
    private ArrayList<Marker> map_markers = new ArrayList<Marker>();
    private Polyline polyline;
    public int zoom_level = 3;
    public static final int stroke_width = 5;
    public String final_return_string;
    private MapEventsOverlay OverlayEventos;
    private boolean clear_button_test = false;
    private Button mClearButton;
    private Button mSaveButton;
    private Button mLayersButton;
    public Boolean gpsStatus = true;
    private Button mLocationButton;
    public MyLocationNewOverlay mMyLocationOverlay;
    public Boolean data_loaded = false;

    private MapHelper mHelper;

    private AlertDialog zoomDialog;
    private View zoomDialogView;

    private Button zoomPointButton;
    private Button zoomLocationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.geoshape_osm_layout);
        setTitle(getString(R.string.geoshape_title)); // Setting title of the action
        mSaveButton = (Button) findViewById(R.id.save);
        mClearButton = (Button) findViewById(R.id.clear);

        mMap = (MapView) findViewById(R.id.geoshape_mapview);
        mHelper = new MapHelper(this, mMap, GeoShapeOsmMapActivity.this);
        mMap.setMultiTouchControls(true);
        mMap.setBuiltInZoomControls(true);
        mMap.setMapListener(mapViewListner);
        overlayPointPathListner();
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnLocation();
            }
        });
        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (map_markers.size() != 0) {
                    showClearDialog();
                }
            }
        });
        mLayersButton = (Button) findViewById(R.id.layers);
        mLayersButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mHelper.showLayersDialog(GeoShapeOsmMapActivity.this);

            }
        });
        mLocationButton = (Button) findViewById(R.id.gps);
        mLocationButton.setEnabled(false);
        mLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                showZoomDialog();
            }
        });


        GpsMyLocationProvider imlp = new GpsMyLocationProvider(this.getBaseContext());
        imlp.setLocationUpdateMinDistance(1000);
        imlp.setLocationUpdateMinTime(60000);
        mMyLocationOverlay = new MyLocationNewOverlay(mMap);


        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            if (intent.hasExtra(GeoShapeWidget.SHAPE_LOCATION)) {
                mClearButton.setEnabled(true);
                data_loaded = true;
                String s = intent.getStringExtra(GeoShapeWidget.SHAPE_LOCATION);
                overlayIntentPolygon(s);
                //zoomToCentroid();
                mLocationButton.setEnabled(true);
                zoomtoBounds();
            }
        } else {
            mMyLocationOverlay.runOnFirstFix(centerAroundFix);
            mClearButton.setEnabled(false);
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    GeoPoint point = new GeoPoint(34.08145, -39.85007);
                    mMap.getController().setZoom(3);
                    mMap.getController().setCenter(point);
                }
            }, 100);

        }

        mMap.invalidate();

        zoomDialogView = getLayoutInflater().inflate(R.layout.geoshape_zoom_dialog, null);

        zoomLocationButton = (Button) zoomDialogView.findViewById(R.id.zoom_location);
        zoomLocationButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                zoomToMyLocation();
                mMap.invalidate();
                zoomDialog.dismiss();
            }
        });

        zoomPointButton = (Button) zoomDialogView.findViewById(R.id.zoom_shape);
        zoomPointButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //zoomToCentroid();
                zoomtoBounds();
                mMap.invalidate();
                zoomDialog.dismiss();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null) {
            mHelper.setBasemap();
        }

        upMyLocationOverlayLayers();
    }

    @Override
    public void onBackPressed() {
        if (map_markers != null && map_markers.size() > 0) {
            showBackDialog();
        } else {
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableMyLocation();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disableMyLocation();
    }


    private void overlayIntentPolygon(String str) {
        mClearButton.setEnabled(true);
        clear_button_test = true;
        String s = str.replace("; ", ";");
        String[] sa = s.split(";");
        for (int i = 0; i < (sa.length - 1); i++) {
            String[] sp = sa[i].split(" ");
            double[] gp = new double[4];
            String lat = sp[0].replace(" ", "");
            String lng = sp[1].replace(" ", "");
            gp[0] = Double.parseDouble(lat);
            gp[1] = Double.parseDouble(lng);
            Marker marker = new Marker(mMap);
            marker.setPosition(new GeoPoint(gp[0], gp[1]));
            marker.setDraggable(true);
            marker.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_place_black_36dp));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setOnMarkerClickListener(nullmarkerlistner);
            map_markers.add(marker);
            // pathOverlay.addPoint(marker.getPosition());
            marker.setDraggable(true);
            marker.setOnMarkerDragListener(draglistner);
            mMap.getOverlays().add(marker);
        }
        update_polygon();
        mMap.getOverlays().remove(OverlayEventos);
    }


    private Handler mHandler = new Handler(Looper.getMainLooper());

    private Runnable centerAroundFix = new Runnable() {
        public void run() {
            mHandler.post(new Runnable() {
                public void run() {
                    mLocationButton.setEnabled(true);
                    showZoomDialog();
                }
            });
        }
    };

    private void showGPSDisabledAlertToUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(getString(R.string.gps_enable_message))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.enable_gps),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                startActivityForResult(
                                        new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0);
                            }
                        });
        alertDialogBuilder.setNegativeButton(getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private void upMyLocationOverlayLayers() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            overlayMyLocationLayers();
        } else {
            showGPSDisabledAlertToUser();
        }

    }

    private void overlayMyLocationLayers() {
        mMap.getOverlays().add(mMyLocationOverlay);
        mMyLocationOverlay.setEnabled(true);
        mMyLocationOverlay.enableMyLocation();
    }

    private void zoomToMyLocation() {
        if (mMyLocationOverlay.getMyLocation() != null) {
            mMap.getController().setZoom(15);
            mMap.getController().setCenter(mMyLocationOverlay.getMyLocation());
        }
    }

    private void disableMyLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mMyLocationOverlay.setEnabled(false);
            mMyLocationOverlay.disableFollowLocation();
            mMyLocationOverlay.disableMyLocation();
            gpsStatus = false;
        }
    }


    private void overlayPointPathListner() {
        OverlayEventos = new MapEventsOverlay(mReceive);
        polyline = new Polyline();
        polyline.setColor(Color.RED);
        Paint paint = polyline.getPaint();
        paint.setStrokeWidth(stroke_width);
        mMap.getOverlays().add(polyline);
        mMap.getOverlays().add(OverlayEventos);
        mMap.invalidate();
    }

    private void clearFeatures() {
        clear_button_test = false;
        map_markers.clear();
        polyline.setPoints(new ArrayList<GeoPoint>());
        mMap.getOverlays().clear();
        mClearButton.setEnabled(false);
        //mSaveButton.setEnabled(false);
        overlayPointPathListner();
        overlayMyLocationLayers();
        mMap.invalidate();

    }

    private void showClearDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.geo_clear_warning))
                .setPositiveButton(getString(R.string.clear),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                clearFeatures();
                            }
                        })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                }).show();

    }

    private void showBackDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.geo_exit_warning))
                .setPositiveButton(getString(R.string.discard),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                }).show();

    }

    private String generateReturnString() {
        String tempString = "";
        if (map_markers.size() > 1) {
            map_markers.add(map_markers.get(0));
            for (int i = 0; i < map_markers.size(); i++) {
                String lat = Double.toString(map_markers.get(i).getPosition().getLatitude());
                String lng = Double.toString(map_markers.get(i).getPosition().getLongitude());
                String alt = "0.0";
                String acu = "0.0";
                tempString = tempString + lat + " " + lng + " " + alt + " " + acu + ";";
            }
        }
        return tempString;
    }

    private void returnLocation() {
        final_return_string = generateReturnString();
        Intent i = new Intent();
        i.putExtra(
                FormEntryActivity.GEOSHAPE_RESULTS,
                final_return_string);
        setResult(RESULT_OK, i);
        finish();
    }

    private void update_polygon() {
        List<GeoPoint> points = new ArrayList<>();
        for (int i = 0; i < map_markers.size(); i++) {
            points.add(map_markers.get(i).getPosition());
        }
        points.add(map_markers.get(0).getPosition());

        polyline.setPoints(points);
        mMap.invalidate();
    }

    private MapEventsReceiver mReceive = new MapEventsReceiver() {
        @Override
        public boolean longPressHelper(GeoPoint point) {
            if (!clear_button_test) {
                mClearButton.setEnabled(true);
                clear_button_test = true;
            }
            Marker marker = new Marker(mMap);
            marker.setPosition(point);
            marker.setDraggable(true);
            marker.setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_place_black_36dp));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setOnMarkerClickListener(nullmarkerlistner);
            map_markers.add(marker);
            marker.setDraggable(true);
            marker.setOnMarkerDragListener(draglistner);
            mMap.getOverlays().add(marker);
            List<GeoPoint> points = polyline.getPoints();
            points.add(marker.getPosition());
            polyline.setPoints(points);
            update_polygon();
            mMap.invalidate();
            return false;
        }

        @Override
        public boolean singleTapConfirmedHelper(GeoPoint arg0) {
            return false;
        }
    };

    private MapListener mapViewListner = new MapListener() {
        @Override
        public boolean onZoom(ZoomEvent zoomLev) {
            zoom_level = zoomLev.getZoomLevel();
            return false;
        }

        @Override
        public boolean onScroll(ScrollEvent arg0) {
            return false;
        }

    };

    private Marker.OnMarkerDragListener draglistner = new Marker.OnMarkerDragListener() {
        @Override
        public void onMarkerDragStart(Marker marker) {

        }

        @Override
        public void onMarkerDragEnd(Marker marker) {
            update_polygon();

        }

        @Override
        public void onMarkerDrag(Marker marker) {
            update_polygon();

        }
    };


    private Marker.OnMarkerClickListener nullmarkerlistner = new Marker.OnMarkerClickListener() {

        @Override
        public boolean onMarkerClick(Marker arg0, MapView arg1) {
            return false;
        }
    };

    /*
        This functions should be added to the mapHelper Class

     */
    private void zoomtoBounds() {
        mMap.getController().setZoom(4);
        mMap.invalidate();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                double minLat = Double.MAX_VALUE;
                double maxLat = Double.MIN_VALUE;
                double minLong = Double.MAX_VALUE;
                double maxLong = Double.MIN_VALUE;
                Integer size = map_markers.size();
                for (int i = 0; i < size; i++) {
                    GeoPoint tempMarker = map_markers.get(i).getPosition();
                    if (tempMarker.getLatitude() < minLat) {
                        minLat = tempMarker.getLatitude();
                    }
                    if (tempMarker.getLatitude() > maxLat) {
                        maxLat = tempMarker.getLatitude();
                    }
                    if (tempMarker.getLongitude() < minLong) {
                        minLong = tempMarker.getLongitude();
                    }
                    if (tempMarker.getLongitude() > maxLong) {
                        maxLong = tempMarker.getLongitude();
                    }
                }
                BoundingBox boundingBox = new BoundingBox(maxLat, maxLong, minLat, minLong);
                mMap.zoomToBoundingBox(boundingBox, false);
                mMap.invalidate();
            }
        }, 100);
        mMap.invalidate();

    }

    public void showZoomDialog() {

        if (zoomDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.zoom_to_where));
            builder.setView(zoomDialogView)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            dialog.cancel();
                            zoomDialog.dismiss();
                        }
                    });
            zoomDialog = builder.create();
        }
        //If feature enable zoom to button else disable
        if (mMyLocationOverlay.getMyLocation() != null) {
            zoomLocationButton.setEnabled(true);
            zoomLocationButton.setBackgroundColor(Color.parseColor("#50cccccc"));
            zoomLocationButton.setTextColor(Color.parseColor("#ff333333"));
        } else {
            zoomLocationButton.setEnabled(false);
            zoomLocationButton.setBackgroundColor(Color.parseColor("#50e2e2e2"));
            zoomLocationButton.setTextColor(Color.parseColor("#FF979797"));
        }

        if (map_markers.size() != 0) {
            zoomPointButton.setEnabled(true);
            zoomPointButton.setBackgroundColor(Color.parseColor("#50cccccc"));
            zoomPointButton.setTextColor(Color.parseColor("#ff333333"));
        } else {
            zoomPointButton.setEnabled(false);
            zoomPointButton.setBackgroundColor(Color.parseColor("#50e2e2e2"));
            zoomPointButton.setTextColor(Color.parseColor("#FF979797"));
        }
        zoomDialog.show();
    }

    @Override
    public void destroy() {

    }
}
