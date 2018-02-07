package com.vrtrappers.trapit.database;

import android.provider.BaseColumns;

public final class Bookmarks {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private Bookmarks() {}

    /* Inner class that defines the table contents */
    public static class BookmarksEntry implements BaseColumns {
        public static final String TABLE_NAME = "entry";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_SNIPPET = "snippet";
    }
}
