package net.dimatomp.lesson5;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import mf.javax.xml.parsers.SAXParser;
import mf.org.apache.xerces.jaxp.SAXParserFactoryImpl;

import static net.dimatomp.lesson5.FeedColumns.ENTRIES;
import static net.dimatomp.lesson5.FeedColumns.ENTRY_DATE;
import static net.dimatomp.lesson5.FeedColumns.ENTRY_DESCRIPTION;
import static net.dimatomp.lesson5.FeedColumns.ENTRY_FEED;
import static net.dimatomp.lesson5.FeedColumns.ENTRY_TITLE;
import static net.dimatomp.lesson5.FeedColumns.ENTRY_URL;
import static net.dimatomp.lesson5.FeedColumns.FEEDS;
import static net.dimatomp.lesson5.FeedColumns.FEED_DESCRIPTION;
import static net.dimatomp.lesson5.FeedColumns.FEED_TITLE;
import static net.dimatomp.lesson5.FeedColumns.FEED_WEBSITE;
import static net.dimatomp.lesson5.FeedColumns.FEED_XML;
import static net.dimatomp.lesson5.FeedColumns._ID;

public class FeedStorage extends ContentProvider {
    private FeedDatabase database;

    private boolean updateWithID(final long id) {
        // TODO Optimize this by passing the cursor
        final String whereID = _ID + " = '" + id + "'";
        Cursor query = database.getReadableDatabase()
                .query(FEEDS, new String[]{FEED_XML}, whereID, null, null, null, null);
        query.moveToFirst();
        String xml = query.getString(query.getColumnIndex(FEED_XML));
        final SQLiteDatabase db = database.getWritableDatabase();
        db.beginTransaction();
        db.delete(ENTRIES, ENTRY_FEED + " = '" + id + "'", null);
        boolean result = true;
        try {
            SAXParser parser = new SAXParserFactoryImpl().newSAXParser();
            parser.parse(xml, new FeedParser(new FeedParser.ParserCallbacks() {
                @Override
                public void updateFeedInfo(ContentValues values) {
                    db.update(FEEDS, values, whereID, null);
                }

                @Override
                public void insertEntry(ContentValues values) {
                    values.put(ENTRY_FEED, id);
                    db.insert(ENTRIES, null, values);
                }
            }));
            db.setTransactionSuccessful();
        } catch (Exception e) {
            result = false;
        } finally {
            db.endTransaction();
        }
        return result;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Support custom selection and selectionArgs
        if (uri.getPathSegments().size() == 1)
            if (uri.getLastPathSegment().equals("removeFeed") && uri.getQueryParameterNames().contains("feedId")) {
                SQLiteDatabase db = database.getWritableDatabase();
                db.beginTransaction();
                try {
                    String feedId = uri.getQueryParameter("feedId");
                    db.delete(ENTRIES, ENTRY_FEED + " = ?", new String[]{feedId});
                    db.delete(FEEDS, _ID + " = ?", new String[]{feedId});
                    db.setTransactionSuccessful();
                } catch (Exception e) {
                    // Unsuccessful transaction.
                } finally {
                    db.endTransaction();
                }
                return 1;
            }
        throw new IllegalArgumentException("Bad delete URI");
    }

    @Override
    public String getType(Uri uri) {
        // Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (uri.getPathSegments().size() == 1)
            switch (uri.getLastPathSegment()) {
                case "feed":
                    if (uri.getQueryParameterNames().contains("feedXML")) {
                        if (values == null)
                            values = new ContentValues(1);
                        values.put(FEED_XML, uri.getQueryParameter("feedXML"));
                        long rowID = database.getWritableDatabase().insert(FEEDS, null, values);
                        updateWithID(rowID);
                        return Uri.parse("content://net.dimatomp.feeds.provider/entries?feedId=" + rowID);
                    }
            }
        throw new IllegalArgumentException("Bad insert URI");
    }

    @Override
    public boolean onCreate() {
        database = new FeedDatabase(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = database.getReadableDatabase();
        if (uri.getPathSegments().size() == 1) {
            switch (uri.getLastPathSegment()) {
                case "feeds":
                    return db.query(FEEDS, projection, selection, selectionArgs, null, null, null);
                case "entries":
                    if (!uri.getQueryParameterNames().contains("feedId"))
                        break;
                    selection = addToSelection(selection, ENTRY_FEED, uri.getQueryParameter("feedId"));
                    return db.query(ENTRIES, projection, selection, selectionArgs, null, null, null);
            }
        }
        throw new IllegalArgumentException("Bad query URI");
    }

    private String addToSelection(String selection, String key, String value) {
        if (selection != null && !selection.isEmpty())
            selection = " AND (" + selection + ")";
        else
            selection = "";
        return key + " = '" + value + "'" + selection;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        if (uri.getPathSegments().size() == 1)
            switch (uri.getLastPathSegment()) {
                case "refresh":
                    if (uri.getQueryParameterNames().contains("feedId"))
                        selection = addToSelection(selection, _ID, uri.getQueryParameter("feedId"));
                    Cursor ids = database.getReadableDatabase().query(FEEDS, new String[]{_ID}, selection, selectionArgs, null, null, null);
                    for (ids.moveToFirst(); !ids.isAfterLast(); ids.moveToNext())
                        updateWithID(ids.getLong(ids.getColumnIndex(_ID)));
                    return ids.getCount();
            }
        throw new IllegalArgumentException("Bad update URI");
    }

    private class FeedDatabase extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "feeds.db";

        FeedDatabase(Context context) {
            super(context, DATABASE_NAME, null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + FEEDS + " (" +
                    _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    FEED_TITLE + " TEXT, " +
                    FEED_XML + " TEXT NOT NULL, " +
                    FEED_WEBSITE + " TEXT, " +
                    FEED_DESCRIPTION + " TEXT);");
            db.execSQL("CREATE TABLE " + ENTRIES + " (" +
                    _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    ENTRY_TITLE + " TEXT, " +
                    ENTRY_FEED + " INTEGER NOT NULL, " +
                    ENTRY_DESCRIPTION + " TEXT, " +
                    ENTRY_URL + " TEXT, " +
                    ENTRY_DATE + " INTEGER);");

            ContentValues values = new ContentValues();
            values.put(FEED_XML, "http://bash.im/rss/");
            db.insert("FEEDS", null, values);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + FEEDS + ";");
            db.execSQL("DROP TABLE IF EXISTS " + ENTRIES + ";");
            onCreate(db);
        }
    }
}
