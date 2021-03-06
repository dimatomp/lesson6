package net.dimatomp.lesson5;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static net.dimatomp.lesson5.FeedColumns.ENTRY_DATE;
import static net.dimatomp.lesson5.FeedColumns.ENTRY_DESCRIPTION;
import static net.dimatomp.lesson5.FeedColumns.ENTRY_TITLE;
import static net.dimatomp.lesson5.FeedColumns.ENTRY_URL;

public class FeedEntries extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String EXTRA_FEED_ID = "net.dimatomp.lesson5.FeedEntries.EXTRA_FEED_ID";
    private static final String COLLAPSED_ENTRIES = "collapsedEntries";
    static SimpleCursorAdapter.ViewBinder binder = new SimpleCursorAdapter.ViewBinder() {
        final DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.DEFAULT, Locale.getDefault());

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            if (cursor.getColumnName(columnIndex).equals(ENTRY_DATE))
                ((TextView) view).setText(dateTimeFormat.format(new Date(cursor.getLong(columnIndex))));
            else
                ((TextView) view).setText(cursor.getString(columnIndex));
            return true;
        }
    };
    private final ContentObserver observer = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            getLoaderManager().restartLoader(0, getIntent().getExtras(), FeedEntries.this);
            collapsedEntries.clear();
        }
    };
    Menu menu;
    boolean refreshButtonShown = true;
    Set<Long> collapsedEntries = new HashSet<>();

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case 0:
                Uri uri = Uri.parse("content://net.dimatomp.feeds.provider/entries?feedId=" + args.getInt(EXTRA_FEED_ID));
                setRefreshButtonShown(false);
                return new CursorLoader(this, uri, null, null, null, ENTRY_DATE + " DESC");
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        ((CursorAdapter) getListAdapter()).swapCursor(data);
        setRefreshButtonShown(true);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        ((CursorAdapter) getListAdapter()).swapCursor(null);
        setRefreshButtonShown(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int cnt = 0;
        long[] entries = new long[collapsedEntries.size()];
        for (long a: collapsedEntries)
            entries[cnt++] = a;
        outState.putLongArray(COLLAPSED_ENTRIES, entries);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Cursor entry = (Cursor) getListAdapter().getItem(position);
        String address = entry.getString(entry.getColumnIndex(ENTRY_URL));
        if (address != null && !address.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(address));
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_feed_entries);

        SimpleCursorAdapter adapter = new ExpandableItemCursorAdapter(R.layout.feed_entry, null,
                new String[]{ENTRY_TITLE, ENTRY_DATE, ENTRY_DESCRIPTION},
                new int[]{android.R.id.text1, android.R.id.text2, R.id.short_description}, 0);
        adapter.setViewBinder(binder);
        setListAdapter(adapter);
        getLoaderManager().initLoader(0, getIntent().getExtras(), this);

        if (savedInstanceState != null && savedInstanceState.containsKey(COLLAPSED_ENTRIES))
            for (long a: savedInstanceState.getLongArray(COLLAPSED_ENTRIES))
                collapsedEntries.add(a);

        getContentResolver().registerContentObserver(RSSUpdater.REFRESH_ALL, false, observer);
        getContentResolver().registerContentObserver(RSSUpdater.refreshSpecificFeed(Integer.toString(getIntent().getIntExtra(EXTRA_FEED_ID, -1))), false, observer);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_feed_entries, menu);
        this.menu = menu;
        menu.findItem(R.id.action_refresh).setVisible(refreshButtonShown);
        return super.onCreateOptionsMenu(menu);
    }

    public void setRefreshButtonShown(boolean shown) {
        refreshButtonShown = shown;
        setProgressBarIndeterminateVisibility(!shown);
        if (menu != null)
            menu.findItem(R.id.action_refresh).setVisible(shown);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                Intent intent = new Intent(this, RSSUpdater.class);
                intent.putExtra(EXTRA_FEED_ID, Integer.toString(getIntent().getIntExtra(EXTRA_FEED_ID, -1)));
                setRefreshButtonShown(false);
                startService(intent);
                return true;
            case R.id.action_expand_all:
                collapsedEntries.clear();
                ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
                return true;
            case R.id.action_collapse_all:
                BaseAdapter adapter = (BaseAdapter) getListAdapter();
                for (int i = 0; i < adapter.getCount(); i++)
                    collapsedEntries.add(adapter.getItemId(i));
                adapter.notifyDataSetChanged();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void expandCollapse(View v) {
        View parent = (View) v.getParent();
        Long itemId = null;
        for (Pair pair: viewToId)
            if (pair != null && pair.view == parent) {
                itemId = pair.id;
                break;
            }
        View description = parent.findViewById(R.id.short_description);
        if (description.getVisibility() == View.VISIBLE) {
            description.setVisibility(View.GONE);
            if (itemId != null)
                collapsedEntries.add(itemId);
        } else {
            description.setVisibility(View.VISIBLE);
            if (itemId != null)
                collapsedEntries.remove(itemId);
        }
    }

    class Pair {
        final View view;
        final long id;

        Pair(View view, long id) {
            this.view = view;
            this.id = id;
        }
    }

    int cPos = 0;
    final Pair viewToId[] = new Pair[20];

    private class ExpandableItemCursorAdapter extends SimpleCursorAdapter {
        private ExpandableItemCursorAdapter(int layout, Cursor c, String[] from, int[] to, int flags) {
            super(FeedEntries.this, layout, c, from, to, flags);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View result = super.getView(position, convertView, parent);
            viewToId[cPos] = new Pair(result, getItemId(position));
            cPos = (cPos + viewToId.length) % viewToId.length;
            boolean collapsed = collapsedEntries.contains(getItemId(position));
            result.findViewById(R.id.short_description).setVisibility(collapsed ? View.GONE : View.VISIBLE);
            return result;
        }
    }
}
