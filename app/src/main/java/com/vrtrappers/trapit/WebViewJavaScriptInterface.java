package com.vrtrappers.trapit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

class WebViewJavaScriptInterface{
    private String rawText;
    private String lang;
    private Context context;
    WebViewJavaScriptInterface(String value, Context c) {
        rawText=value;
        lang="";
        context=c;
    }

    @JavascriptInterface
    public void onTapStart(String title,String snippet){
        Intent intent = new Intent(context.getApplicationContext(), InfoActivity.class);
        Bundle bundle=new Bundle();
        bundle.putString(context.getString(R.string.wiki_title_intent), title);
        bundle.putString(context.getString(R.string.wiki_snippet_intent),snippet);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    @JavascriptInterface
    public boolean isBookmarked(String title){return CameraActivity.bookmarksHelper.isExistTitle(title);}
    @JavascriptInterface
    public boolean addBookmark(String title,String snippet){return CameraActivity.bookmarksHelper.addEntry(title,snippet);}
    @JavascriptInterface
    public boolean removeBookmark(String title){return CameraActivity.bookmarksHelper.removeEntry(title);}
    @JavascriptInterface
    public String getSystemLang(){
        return (context.getString(R.string.lang_code));
    }
    @JavascriptInterface
    public String getTransInfo(){return (context.getString(R.string.translate_info_lang));}
    @JavascriptInterface
    public String getNoResultStr(){return (context.getString(R.string.no_result));}
    @JavascriptInterface
    public String getLangFromCode(){return (context.getString(context.getResources().getIdentifier(lang,"string",context.getPackageName())));}
    @JavascriptInterface
    public String getSuggestStr(){return context.getString(R.string.suggestion);}
    @JavascriptInterface
    public void setrawText(String t){
        rawText=t;
    }
    @JavascriptInterface
    public String getLanguageStr(){
        lang="";
        if(CameraActivity.getLangDetectAvailable()) {
            try {
                com.cybozu.labs.langdetect.Detector detector = DetectorFactory.create();
                detector.append(rawText);
                lang = detector.detect();
            } catch (LangDetectException e) {
                Log.d("lang", "something wrong" + e.getMessage());
            }
            if(lang.equals(context.getString(R.string.lang_code))){
                lang="";
            }
        }
        return lang;
    }
}
