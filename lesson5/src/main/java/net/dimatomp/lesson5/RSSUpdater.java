package net.dimatomp.lesson5;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;

public class RSSUpdater extends IntentService {
    public static final Uri REFRESH_ALL = Uri.parse("content://net.dimatomp.feeds.provider/refresh");

    public RSSUpdater() {
        super("RSSUpdater");
    }

    public static Uri refreshSpecificFeed(String feedId) {
        return Uri.parse("content://net.dimatomp.feeds.provider/refresh?feedId=" + Uri.encode(feedId));
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String feedId = intent.getStringExtra(FeedEntries.EXTRA_FEED_ID);
        Uri uri = feedId == null ? REFRESH_ALL : refreshSpecificFeed(feedId);
        ContentResolver resolver = getContentResolver();
        resolver.update(uri, null, null, null);
        resolver.notifyChange(uri, null);
    }
}
