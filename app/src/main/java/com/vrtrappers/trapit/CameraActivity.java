package com.vrtrappers.trapit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.text.TextRecognizer;
import com.vrtrappers.trapit.camera.CameraSource;
import com.vrtrappers.trapit.camera.CameraSourcePreview;
import com.vrtrappers.trapit.camera.GraphicOverlay;
import com.vrtrappers.trapit.database.BookmarksHelper;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    // Intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;
    private static boolean isAvailLangDetect;
    protected Point focusPoint;
    private FloatingActionButton resetFocusBtn;
    private FloatingActionButton freezeBtn;
    private FloatingActionButton bookmarksBtn;
    protected boolean isFreezeOn;
    // Permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    IntentFilter lowstorageFilter;
    private CameraSource mCameraSource;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    protected CameraSourcePreview mPreview;
    private GraphicOverlay<OcrGraphic> mGraphicOverlay;
    protected CoordinatorLayout mainLayout;
    protected static BookmarksHelper bookmarksHelper;
    protected Point deviceSize;
    private ImageView imageView;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            if (Build.VERSION.SDK_INT < 16) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_camera);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
        isAvailLangDetect=unZipProfiles();
        if(isAvailLangDetect){
            try {
                DetectorFactory.loadProfile(getFilesDir() + "/profiles/");
            } catch (LangDetectException e) {
                Log.d("cybozu", "something wrong" + e.getMessage());
            }
        }
        deviceSize = new Point();
        bookmarksHelper=new BookmarksHelper(this);
        mainLayout = (CoordinatorLayout) findViewById(R.id.topLayout);
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay<OcrGraphic>) findViewById(R.id.graphicOverlay);
        resetFocusBtn=(FloatingActionButton)findViewById(R.id.focus_reset_btn);
        resetFocusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                focusPoint=null;
                if(imageView!=null) {
                    mainLayout.removeView(imageView);
                }
                resetFocusBtn.hide();
            }
        });
        freezeBtn=(FloatingActionButton)findViewById(R.id.freeze_btn);
        freezeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isFreezeOn) {
                    freezeBtn.setImageResource(R.drawable.ic_pause_circle_outline_black_24dp);
                    isFreezeOn = false;
                }
                else {
                    freezeBtn.setImageResource(R.drawable.ic_pause_circle_filled_black_24dp);
                    isFreezeOn = true;
                }
            }
        });
        freezeBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ArrayList<CharSequence> augObjectsList=getAllWebViewsParam();
                if(augObjectsList.size()==0)
                    return false;
                Intent intent=new Intent(getApplicationContext(),ResultsActivity.class);
                intent.putCharSequenceArrayListExtra(getString(R.string.raw_text_intent),augObjectsList);
                startActivity(intent);
                return true;
            }
        });
        bookmarksBtn = (FloatingActionButton) findViewById(R.id.bookmarks_btn);
        bookmarksBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), BookMarkActivity.class);
                startActivity(intent);
            }
        });
        gestureDetector = new GestureDetector(this, new CaptureGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }
        getWindowManager().getDefaultDisplay().getSize(deviceSize);
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
    protected boolean unZipProfiles() {
            File file = new File(getFilesDir() + "/profiles/");
            if (!file.exists()) {
                boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;
                if (hasLowStorage) {
                    Toast.makeText(this, R.string.low_storage_error_lang, Toast.LENGTH_LONG).show();
                    Log.w(TAG, getString(R.string.low_storage_error_lang));
                    return false;
                }
                try {
                    Decompress.unzipFromAssets(getApplicationContext(), "profiles.zip", getFilesDir() + "/profiles/" );
                } catch (IOException e) {
                    Log.d("unzip","something wrong"+e.getMessage());
                    return false;
                }
            }
            return true;
    }
    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }
    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the ocr detector to detect small text samples
     * at long distances.
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource() {
        Context context = getApplicationContext();

        // A text recognizer is created to find text.  An associated multi-processor instance
        // is set to receive the text recognition results, track the text, and maintain
        // graphics for each text block on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each text block.
        TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();
        textRecognizer.setProcessor(new OcrDetectorProcessor(mGraphicOverlay,this));

        if (!textRecognizer.isOperational()) {
            // Note: The first time that an app using a Vision API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any text,
            // barcodes, or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error_ocr, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error_ocr));
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the text recognizer to detect small pieces of text.
        mCameraSource =
                new CameraSource.Builder(getApplicationContext(), textRecognizer)
                        .setFacing(CameraSource.CAMERA_FACING_BACK)
                        .setRequestedPreviewSize(1280, 1024)
                        .setRequestedFps(2.0f)
                        .setFlashMode(Camera.Parameters.FLASH_MODE_AUTO)
                        .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)
                        .build();

    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
        final ImageView flashBtn = (ImageView) findViewById(R.id.flashButton);
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)&& mCameraSource.getFlashMode()!=null) {
            switch (mCameraSource.getFlashMode()) {
                case Camera.Parameters.FLASH_MODE_AUTO:
                    flashBtn.setImageResource(R.drawable.ic_flash_auto_black_24px);
                    break;
                case Camera.Parameters.FLASH_MODE_OFF:
                    flashBtn.setImageResource(R.drawable.ic_flash_off_black_24px);
                    break;
                case Camera.Parameters.FLASH_MODE_TORCH:
                    flashBtn.setImageResource(R.drawable.ic_flash_on_black_24px);
                    break;
            }
            flashBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mCameraSource.getFlashMode()!=null){
                        switch (mCameraSource.getFlashMode()) {
                            case Camera.Parameters.FLASH_MODE_AUTO:
                                mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                                flashBtn.setImageResource(R.drawable.ic_flash_on_black_24px);
                                break;
                            case Camera.Parameters.FLASH_MODE_OFF:
                                mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                                flashBtn.setImageResource(R.drawable.ic_flash_auto_black_24px);
                                break;
                            case Camera.Parameters.FLASH_MODE_TORCH:
                                mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                                flashBtn.setImageResource(R.drawable.ic_flash_off_black_24px);
                                break;
                        }
                    }
                }
            });
            flashBtn.setVisibility(View.VISIBLE);
        }
        refreshAllWVBookMarks();
    }

    private void refreshAllWVBookMarks() {
        for( int i = 0; i < mainLayout.getChildCount(); i++ ) {
            if (mainLayout.getChildAt(i) instanceof WebView) {
                ((WebView) mainLayout.getChildAt(i)).loadUrl("javascript:refreshBM()");
            }
        }
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    protected void removeAllWebViews() {
        for( int i = 0; i < mainLayout.getChildCount(); i++ ) {
            if (mainLayout.getChildAt(i) instanceof WebView) {
                mainLayout.removeView(mainLayout.getChildAt(i));
            }
        }
    }


    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }
        if(bookmarksHelper!=null){
            bookmarksHelper.close();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if(requestCode==RC_HANDLE_CAMERA_PERM) {
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createCameraSource();
                return;
            }
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    finish();
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getResources().getString(R.string.app_name))
                    .setMessage(R.string.no_camera_permission)
                    .setPositiveButton(R.string.ok, listener)
                    .show();
        }else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        gestureDetector.onTouchEvent(e);
        return scaleGestureDetector.onTouchEvent(e) || super.onTouchEvent(e);
    }
    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        /**
         * Responds to scaling events for a gesture in progress.
         * Reported by pointer motion.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should consider this event
         * as handled. If an event was not handled, the detector
         * will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example,
         * only wants to update scaling factors if the change is
         * greater than 0.01.
         */
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        /**
         * Responds to the beginning of a scaling gesture. Reported by
         * new pointers going down.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         * @return Whether or not the detector should continue recognizing
         * this gesture. For example, if a gesture is beginning
         * with a focal point outside of a region where it makes
         * sense, onScaleBegin() may return false to ignore the
         * rest of the gesture.
         */
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        /**
         * Responds to the end of a scale gesture. Reported by existing
         * pointers going up.
         * <p/>
         * Once a scale has ended, {@link ScaleGestureDetector#getFocusX()}
         * and {@link ScaleGestureDetector#getFocusY()} will return focal point
         * of the pointers remaining on the screen.
         *
         * @param detector The detector reporting the event - use this to
         *                 retrieve extended info about event state.
         */
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (mCameraSource != null) {
                mCameraSource.doZoom(detector.getScaleFactor());
            }
        }
    }
    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if(isFreezeOn){
                return super.onSingleTapConfirmed(e);
            }
            if(imageView!=null){
                mainLayout.removeView(imageView);
            }
            focusPoint=new Point((int)e.getRawX(),(int)e.getRawY());
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            imageView=new ImageView(getApplicationContext());
            imageView.setImageResource(R.drawable.ic_filter_center_focus_black_24dp);
            layoutParams.leftMargin=focusPoint.x-imageView.getDrawable().getIntrinsicWidth()/2;
            layoutParams.topMargin=focusPoint.y-imageView.getDrawable().getIntrinsicHeight()/2;
            imageView.setLayoutParams(layoutParams);
            mainLayout.addView(imageView);
            if(!resetFocusBtn.isShown()) {
                resetFocusBtn.show();
            }
            return super.onSingleTapConfirmed(e);
        }
    }
    public static Map<String, List<String>> getQueryParams(String url) {
        try {
            Map<String, List<String>> params = new HashMap<>();
            String[] urlParts = url.split("\\?");
            if (urlParts.length > 1) {
                String query = urlParts[1];
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    String key = URLDecoder.decode(pair[0], "UTF-8");
                    String value = "";
                    if (pair.length > 1) {
                        value = URLDecoder.decode(pair[1], "UTF-8");
                    }

                    List<String> values = params.get(key);
                    if (values == null) {
                        values = new ArrayList<>();
                        params.put(key, values);
                    }
                    values.add(value);
                }
            }

            return params;
        } catch (UnsupportedEncodingException | ArrayIndexOutOfBoundsException ex) {
            Log.d("unsupaiob", ex.getMessage());
            return null;
        }
    }
    public static boolean getLangDetectAvailable(){
        return isAvailLangDetect;
    }
   private ArrayList<CharSequence> getAllWebViewsParam(){
        ArrayList<CharSequence> webViews=new ArrayList<>();
        for( int i = 0; i < mainLayout.getChildCount(); i++ ) {
            if (mainLayout.getChildAt(i) instanceof WebView) {
                webViews.add(getQueryParams(((WebView) mainLayout.getChildAt(i)).getUrl()).get("q").get(0));
            }
        }
        return webViews;
    }
}