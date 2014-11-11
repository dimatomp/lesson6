package net.dimatomp.lesson5;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;

/**
 * Created by dimatomp on 10.11.14.
 */
public class FeedDeleter extends CursorLoader {
    final int feedId;

    public FeedDeleter(Context context, int feedId) {
        super(context, Uri.parse("content://net.dimatomp.feeds.provider/feeds"), null, null, null, null);
        this.feedId = feedId;
    }

    @Override
    public Cursor loadInBackground() {
        getContext().getContentResolver().delete(
                Uri.parse("content://net.dimatomp.feeds.provider/removeFeed?feedId=" + feedId), null, null);
        return super.loadInBackground();
    }
}
