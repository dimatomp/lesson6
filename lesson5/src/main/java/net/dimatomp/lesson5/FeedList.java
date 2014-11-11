package net.dimatomp.lesson5;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import static net.dimatomp.lesson5.FeedColumns.FEED_DESCRIPTION;
import static net.dimatomp.lesson5.FeedColumns.FEED_TITLE;
import static net.dimatomp.lesson5.FeedColumns._ID;

public class FeedList extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    Menu menu;
    private final SimpleCursorAdapter.ViewBinder binder = new SimpleCursorAdapter.ViewBinder() {
        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            if (view.getId() == R.id.remove_button) {
                boolean removeButtonVisible = menu == null || !menu.findItem(R.id.action_remove_buttons).isChecked();
                view.setVisibility(removeButtonVisible ? View.VISIBLE : View.INVISIBLE);
            } else {
                String text = cursor.getString(columnIndex);
                if (view.getId() == android.R.id.text2)
                    view.setVisibility(text == null || text.isEmpty() ? View.GONE : View.VISIBLE);
                ((TextView) view).setText(text);
            }
            return true;
        }
    };
    boolean resumedAfterPause = false;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case 0:
                return new CursorLoader(
                        this, Uri.parse("content://net.dimatomp.feeds.provider/feeds"),
                        null, null, null, null);
            case 1:
                return new FeedDeleter(this, args.getInt("feedId"));
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        ((CursorAdapter) getListAdapter()).swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        ((CursorAdapter) getListAdapter()).swapCursor(null);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Cursor feed = (Cursor) getListAdapter().getItem(position);
        int feedId = feed.getInt(feed.getColumnIndex(_ID));
        Intent intent = new Intent(this, FeedEntries.class);
        intent.putExtra(FeedEntries.EXTRA_FEED_ID, feedId);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_list);

        setListAdapter(new SimpleCursorAdapter(this, R.layout.feed_item, null,
                new String[]{FEED_TITLE, FEED_DESCRIPTION, FEED_DESCRIPTION},
                new int[]{android.R.id.text1, android.R.id.text2, R.id.remove_button}, 0) {
            {
                setViewBinder(binder);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                super.bindView(view, context, cursor);
                final int feedId = cursor.getInt(cursor.getColumnIndex(_ID));
                view.findViewById(R.id.remove_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle args = new Bundle(1);
                        args.putInt("feedId", feedId);
                        getLoaderManager().restartLoader(1, args, FeedList.this).forceLoad();
                    }
                });
            }
        });
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onPause() {
        resumedAfterPause = true;
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (resumedAfterPause)
            getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_feed_list, menu);
        this.menu = menu;
        ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            try {
                Uri refUri = data.getData();
                Intent showEntries = new Intent(this, FeedEntries.class);
                showEntries.putExtra(FeedEntries.EXTRA_FEED_ID, Integer.parseInt(refUri.getQueryParameter("feedId")));
                startActivity(showEntries);
            } catch (NumberFormatException | NullPointerException e) {
                Toast.makeText(this, R.string.invalid_feed_uri, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_new_feed:
                startActivityForResult(new Intent(this, NewFeedForm.class), 1);
                return true;
            case R.id.action_remove_buttons:
                item.setChecked(!item.isChecked());
                ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
