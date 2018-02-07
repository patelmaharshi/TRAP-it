package com.vrtrappers.trapit;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class ResultsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        setSupportActionBar((Toolbar)findViewById(R.id.results_toolbar));
        getSupportActionBar().setTitle(getString(R.string.results_activity_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final ScrollView scrollView=(ScrollView)findViewById(R.id.scroll_view);
        ArrayList<CharSequence> rawTextData=getIntent().getCharSequenceArrayListExtra(getString(R.string.raw_text_intent));
        LinearLayout cardsLayout=(LinearLayout)findViewById(R.id.cards_layout);
        LinearLayout.LayoutParams mLayoutParams=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mLayoutParams.setMargins(16,16,16,16);
        for(int i=0;i<rawTextData.size();i++){
            CardView cardView=new CardView(getApplicationContext());
            LinearLayout linearLayout=new LinearLayout(getApplicationContext());
            LinearLayout.LayoutParams layoutParams=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            linearLayout.setLayoutParams(layoutParams);
            linearLayout.setPadding(16,24,16,24);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            TextView textView=new TextView(getApplicationContext());
            textView.setLayoutParams(layoutParams);
            TextViewCompat.setTextAppearance(textView,R.style.TextAppearance_AppCompat_Title);
            textView.setText(rawTextData.get(i));
            textView.setTextIsSelectable(true);
            linearLayout.addView(textView);
            WebView webView=new WebView(getApplicationContext());
            webView.getSettings().setJavaScriptEnabled(true);
            webView.addJavascriptInterface(new WebViewJavaScriptInterface(rawTextData.get(i).toString(),getApplicationContext()),"android");
            try {
                webView.loadUrl(getString(R.string.web_page_url)+ URLEncoder.encode(rawTextData.get(i).toString().replaceAll("\n"," "),getString(R.string.encoding)));
            } catch (UnsupportedEncodingException e) {
                Log.d("unsupp",e.getMessage());
            }
            webView.setLayoutParams(layoutParams);
            webView.setWebViewClient(new WebViewClient());
            webView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent motionEvent) {
                    scrollView.requestDisallowInterceptTouchEvent(true);

                    switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_UP:
                            scrollView.requestDisallowInterceptTouchEvent(false);
                            break;
                    }
                    return false;
                }
            });
            linearLayout.addView(webView);
            cardView.addView(linearLayout);
            cardView.setLayoutParams(mLayoutParams);
            cardView.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(),R.color.colorPrimaryLight));
            cardsLayout.addView(cardView);
            mLayoutParams=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            mLayoutParams.setMargins(16,0,16,16);
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
