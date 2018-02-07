package com.vrtrappers.trapit;

import android.view.View;

import com.google.android.gms.vision.text.TextBlock;

class AugmentedObject {
        TextBlock textBlock;
        View view;
        AugmentedObject(TextBlock t, View v){
            textBlock=t;
            view=v;
        }
}
