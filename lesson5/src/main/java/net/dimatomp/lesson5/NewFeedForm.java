package net.dimatomp.lesson5;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class NewFeedForm extends Activity {
    public static final String EXTRA_XML_ADDRESS = "net.dimatomp.rss.EXTRA_XML_ADDRESS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_feed_form);

        if (getIntent().hasExtra(EXTRA_XML_ADDRESS))
            ((TextView) findViewById(R.id.xml_address)).setText(getIntent().getStringExtra(EXTRA_XML_ADDRESS));
    }

    public void finishDialog(View view) {
        switch (view.getId()) {
            case R.id.button_ok:
                CharSequence xmlAddress = ((TextView) findViewById(R.id.xml_address)).getText();
                if (xmlAddress != null) {
                    Uri insertionResult = getContentResolver().insert(Uri.parse("content://net.dimatomp.feeds.provider/feed?feedXML=" + Uri.encode(xmlAddress.toString())), null);
                    if (insertionResult != null) {
                        Intent result = new Intent();
                        result.setData(insertionResult);
                        setResult(RESULT_OK, result);
                        break;
                    }
                }
            case R.id.button_cancel:
                setResult(RESULT_CANCELED);
                break;
        }
        finish();
    }
}
