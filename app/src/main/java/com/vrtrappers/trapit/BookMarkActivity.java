package com.vrtrappers.trapit;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.vrtrappers.trapit.database.BookmarksHelper;

import java.util.ArrayList;

public class BookMarkActivity extends AppCompatActivity{
    private BookMarkListAdapter mAdapter;
    private ActionMode mActionMode;
    private ListView listView;
    private TextView mTextView;
    ArrayList<String[]> listRecords;
    BookmarksHelper bookmarksHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmark);
        setSupportActionBar((Toolbar)findViewById(R.id.bookmark_toolbar));
        getSupportActionBar().setTitle(getString(R.string.bookmark_activity_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        bookmarksHelper=new BookmarksHelper(getApplicationContext());
        listView = (ListView) findViewById(R.id.bookmark_list);
        mTextView = (TextView) findViewById(R.id.empty_view);
        listRecords=bookmarksHelper.getAllRecords();
        if(listRecords==null){
            listView.setVisibility(View.GONE);
            mTextView.setVisibility(View.VISIBLE);
            listRecords=new ArrayList<>();
        }
        mAdapter = new BookMarkListAdapter(this, listRecords);
        listView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //If ActionMode not null select item
                if (mActionMode != null)
                    onListItemSelect(position);
                else{
                    Intent intent = new Intent(getApplicationContext(), InfoActivity.class);
                    Bundle bundle=new Bundle();
                    bundle.putString(getString(R.string.wiki_title_intent), listRecords.get(position)[0]);
                    bundle.putString(getString(R.string.wiki_snippet_intent),listRecords.get(position)[1]);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                //Select item on long click
                onListItemSelect(position);
                return true;
            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.bookmark_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_clear_all:
                if(mAdapter.getCount()==0) {
                    Toast.makeText(this, getString(R.string.no_data_available), Toast.LENGTH_SHORT).show();
                    return true;
                }
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.bookmark_activity_title))
                        .setMessage(getString(R.string.bookmark_clear_confirm))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                boolean isCleared=bookmarksHelper.removeAll();
                                if(isCleared){
                                    mAdapter.clearAllData();
                                    listView.setVisibility(View.GONE);
                                    mTextView.setVisibility(View.VISIBLE);
                                }
                            }})
                        .setNegativeButton(android.R.string.no, null).show();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    //List item select method
    private void onListItemSelect(int position) {
        mAdapter.toggleSelection(position);//Toggle the selection

        boolean hasCheckedItems = mAdapter.getSelectedCount() > 0;//Check if any items are already selected or not


        if (hasCheckedItems && mActionMode == null)
            // there are some selected items, start the actionMode
        {
            mActionMode = startSupportActionMode(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    mode.getMenuInflater().inflate(R.menu.bookmark_action_menu, menu);//Inflate the menu over action mode
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    //menu.findItem(R.id.delete_btn).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
                    return true;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    if (item.getItemId() == R.id.delete_btn) {
                        deleteRows();
                    }
                    return false;
                }


                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    mAdapter.removeSelection();
                    setNullToActionMode();//Set action mode null
                }
            });
        }
        else if (!hasCheckedItems && mActionMode != null)
            // there no selected items, finish the actionMode
            mActionMode.finish();

        if (mActionMode != null)
            //set action mode title on item selection
            mActionMode.setTitle(String.valueOf(mAdapter.getSelectedCount()) + " "+getString(R.string.selected));
    }
    //Set action mode null after use
    public void setNullToActionMode() {
        if (mActionMode != null)
            mActionMode = null;
    }

    //Delete selected rows
    public void deleteRows() {
        SparseBooleanArray selected = mAdapter.getSelectedIds();//Get selected ids

        //Loop all selected ids
        for (int i = (selected.size() - 1); i >= 0; i--) {
            if (selected.valueAt(i)) {
                //If current id is selected remove the item via key
                bookmarksHelper.removeEntry(listRecords.get(selected.keyAt(i))[0]);
                listRecords.remove(selected.keyAt(i));
                mAdapter.notifyDataSetChanged();//notify adapter
            }
        }
        if(mAdapter.getCount()==0){
            listView.setVisibility(View.GONE);
            mTextView.setVisibility(View.VISIBLE);
        }
        Toast.makeText(this, selected.size()+" "+ getString(R.string.item_deleted), Toast.LENGTH_SHORT).show();//Show Toast
        mActionMode.finish();//Finish action mode after use
    }
}