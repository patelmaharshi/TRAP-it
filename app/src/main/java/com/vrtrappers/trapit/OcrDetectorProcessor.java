/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vrtrappers.trapit;

import android.graphics.Rect;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.vrtrappers.trapit.camera.GraphicOverlay;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * A very simple Processor which gets detected TextBlocks and adds them to the overlay
 * as OcrGraphics.
 */
class OcrDetectorProcessor implements Detector.Processor<TextBlock> {
    private GraphicOverlay<OcrGraphic> mGraphicOverlay;

    private CameraActivity mCameraActivity;
    private boolean wvcount;
    OcrDetectorProcessor(GraphicOverlay<OcrGraphic> ocrGraphicOverlay,CameraActivity cameraActivity) {
        mGraphicOverlay = ocrGraphicOverlay;
        mCameraActivity=cameraActivity;
    }

    /**
     * Called by the detector to deliver detection results.
     * If your application called for it, this could be a place to check for
     * equivalent detections by tracking TextBlocks that are similar in location and content from
     * previous frames, or reduce noise by eliminating TextBlocks that have not persisted through
     * multiple detections.
     */
    private ArrayList<AugmentedObject> preitems = new ArrayList<>();
    private SparseArray<TextBlock> items;
    @Override
    public void receiveDetections(Detector.Detections<TextBlock> detections) {
        if(mCameraActivity.isFreezeOn) {
           // items=detections.getDetectedItems();
            return;
        }
            mGraphicOverlay.clear();
            items = detections.getDetectedItems();
            ArrayList<AugmentedObject> tempitems = new ArrayList<>();
            for (int i = 0; i < items.size(); ++i) {
                TextBlock item = items.valueAt(i);
                Rect rectItem = item.getBoundingBox();
                if (item.getValue() != null) {
                    boolean flag = false;
                    if (mCameraActivity.focusPoint != null) {
                        if (!rectItem.contains(mCameraActivity.focusPoint.x, mCameraActivity.focusPoint.y))
                            flag = true;
                    }
                    if (flag) continue;
                    boolean similarity = false;
                    for (int j = 0; j < preitems.size(); ++j) {
                        if (rectItem.intersect(preitems.get(j).textBlock.getBoundingBox())||rectItem.contains(preitems.get(j).textBlock.getBoundingBox())) {
                            similarity = true;
                            tempitems.add(preitems.get(j));
                        }
                    }
                    if (!similarity) {
                        AugmentedObject added = new AugmentedObject(item, null);
                        addView(added);
                        tempitems.add(added);
                    }
                }
                OcrGraphic graphic = new OcrGraphic(mGraphicOverlay, item);
                mGraphicOverlay.add(graphic);
                wvcount=true;
            }
            if(items.size()==0 && wvcount){
                mCameraActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mCameraActivity.removeAllWebViews();
                    }
                });
                wvcount=false;
            }
            for (int i = 0; i < preitems.size(); i++) {
                if (!tempitems.contains(preitems.get(i))) {
                    removeView(preitems.get(i).view);
                }
            }
            preitems = new ArrayList<>(tempitems);
    }

    private void addView(final AugmentedObject added){
        final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        Rect rect=new Rect(added.textBlock.getBoundingBox());
        if(mCameraActivity.getResources().getBoolean(R.bool.is_landscape) || mCameraActivity.getResources().getBoolean(R.bool.isTablet)){
            if(added.textBlock.getBoundingBox().left<(mCameraActivity.deviceSize.x*0.15)){
                layoutParams.leftMargin=rect.left;
            }
            else{
                layoutParams.leftMargin=(int)(mCameraActivity.deviceSize.x*0.15);
            }
            layoutParams.rightMargin=(int)(mCameraActivity.deviceSize.x*0.30-layoutParams.leftMargin);
        }
        layoutParams.topMargin=rect.bottom;
        mCameraActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final WebView webView=new WebView(mCameraActivity.getApplicationContext());
                webView.setBackgroundColor(0x40FFFFFF);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.addJavascriptInterface(new WebViewJavaScriptInterface(added.textBlock.getValue(),mCameraActivity.getApplicationContext()),"android");
                try {
                    webView.loadUrl(mCameraActivity.getString(R.string.web_page_url)+ URLEncoder.encode(added.textBlock.getValue().replaceAll("\n"," "),mCameraActivity.getString(R.string.encoding)));
                } catch (UnsupportedEncodingException e) {
                    Log.d("unsupp",e.getMessage());
                }
                webView.setLayoutParams(layoutParams);
                webView.setWebViewClient(new WebViewClient());
                mCameraActivity.mainLayout.addView(webView);
                added.view = webView;
                }
            });
    }
    private void removeView(final View view){
        mCameraActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCameraActivity.mainLayout.removeView(view);
            }
        });
    }
    /**
     * Frees the resources associated with this detection processor.
     */
    @Override
    public void release() {
        mGraphicOverlay.clear();
    }
}