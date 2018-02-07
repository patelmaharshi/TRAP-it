package com.vrtrappers.trapit;


import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;

import com.vrtrappers.trapit.database.BookmarksHelper;

import java.util.ArrayList;
import java.util.List;

public class InfoActivity extends AppCompatActivity {
private final int TAB_SIZE=5;
    private WikiFragment wikiFragment[];
    private TabLayout tabLayout;
    private BookmarksHelper bookmarksHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        getSupportActionBar().setTitle(getIntent().getStringExtra(getString(R.string.wiki_title_intent)));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ViewPager viewPager=(ViewPager)findViewById(R.id.viewpager);
        if(viewPager!=null){
            String[] wikiHosts=getResources().getStringArray(R.array.wiki_hosts);
            String[] pageTitles=getResources().getStringArray(R.array.page_titles);
            ViewPagerAdapter adapter=new ViewPagerAdapter(getSupportFragmentManager());
            wikiFragment=new WikiFragment[TAB_SIZE];
            for(int i=0;i<TAB_SIZE;i++) {
                wikiFragment[i] = new WikiFragment();
                getIntent().putExtra(getString(R.string.host_name_intent),wikiHosts[i]);
                wikiFragment[i].setArguments(getIntent().getExtras());
                adapter.addFragment(wikiFragment[i], pageTitles[i]);
            }
            viewPager.setAdapter(adapter);
        }
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        allotEachTabWithEqualWidth();
        bookmarksHelper=new BookmarksHelper(getApplicationContext());
    }
    /**
     * To allow equal width for each tab, while (TabLayout.MODE_SCROLLABLE)
     */
    private void allotEachTabWithEqualWidth() {

        ViewGroup slidingTabStrip = (ViewGroup) tabLayout.getChildAt(0);
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            View tab = slidingTabStrip.getChildAt(i);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) tab.getLayoutParams();
            layoutParams.weight = 1;
            tab.setLayoutParams(layoutParams);
        }

    }
    @Override
    public void onBackPressed() {
        WebView webView = (WebView) wikiFragment[tabLayout.getSelectedTabPosition()].getView().findViewById(R.id.wiki_page);
        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        allotEachTabWithEqualWidth();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_items, menu);
        MenuItem item=menu.findItem(R.id.action_bookmark);
        if(bookmarksHelper.isExistTitle(getIntent().getStringExtra(getString(R.string.wiki_title_intent)))){
            item.setIcon(R.drawable.ic_bookmark_black_24dp);
        }
        else item.setIcon(R.drawable.ic_bookmark_border_black_24dp);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_search:
            try {
                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH );
                intent.putExtra(SearchManager.QUERY, getIntent().getStringExtra(getString(R.string.wiki_title_intent)));
                startActivity(intent);
            } catch (ActivityNotFoundException e){
                Snackbar.make(findViewById(android.R.id.content),getString(R.string.search_app_error), Snackbar.LENGTH_LONG).setAction(getString(R.string.gsearch), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.google_url)+getIntent().getStringExtra(getString(R.string.wiki_title_intent))));
                        try{
                            startActivity(intent);
                        }catch (ActivityNotFoundException e){
                            Snackbar.make(findViewById(android.R.id.content),getString(R.string.browser_error), Snackbar.LENGTH_LONG);
                        }
                    }
                }).show();
            }
            return true;
            case R.id.action_bookmark:
                boolean isBookmarked= bookmarksHelper.isExistTitle(getIntent().getStringExtra(getString(R.string.wiki_title_intent)));
                if(isBookmarked) {
                    isBookmarked = bookmarksHelper.removeEntry(getIntent().getStringExtra(getString(R.string.wiki_title_intent)));
                }
                else{
                    isBookmarked=bookmarksHelper.addEntry(getIntent().getStringExtra(getString(R.string.wiki_title_intent)),getIntent().getStringExtra(getString(R.string.wiki_snippet_intent)));
                }
                if(isBookmarked){
                    item.setIcon(R.drawable.ic_bookmark_black_24dp);
                }
                else item.setIcon(R.drawable.ic_bookmark_border_black_24dp);
                return true;
            case R.id.action_show_bookmark:
                Intent intent = new Intent(getApplicationContext(), BookMarkActivity.class);
                startActivity(intent);
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
            return super.onOptionsItemSelected(item);
        }
    }
    private class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();
        int[] tabIcons = {
                R.drawable.wikipedia,
                R.drawable.piece,
                R.drawable.commons,
                R.drawable.wikinews,
                R.drawable.wikivoyage
        };
        public ViewPagerAdapter(FragmentManager fragmentManager){
            super(fragmentManager);
        }

        public void addFragment(Fragment fragment, String title) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {

            Drawable image = ContextCompat.getDrawable(getApplicationContext(),tabIcons[position]);
            image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
            // Replace blank spaces with image icon
            SpannableString sb = new SpannableString("   " + mFragmentTitles.get(position));
            ImageSpan imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BOTTOM);
            sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return sb;
        }
    }
}
