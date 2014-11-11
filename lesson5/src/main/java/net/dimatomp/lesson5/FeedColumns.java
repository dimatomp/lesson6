package net.dimatomp.lesson5;

import android.provider.BaseColumns;

/**
 * Created by dimatomp on 20.10.14.
 */
public interface FeedColumns extends BaseColumns {
    String FEEDS = "feeds";
    String FEED_TITLE = "FeedTitle";
    String FEED_XML = "XMLAddress";
    String FEED_WEBSITE = "Website";
    String FEED_DESCRIPTION = "Description";

    String ENTRIES = "entries";
    String ENTRY_FEED = "FeedId";
    String ENTRY_TITLE = "EntryTitle";
    String ENTRY_URL = "EntryURL";
    String ENTRY_DESCRIPTION = "EntryDescription";
    String ENTRY_DATE = "PublishDate";
}
