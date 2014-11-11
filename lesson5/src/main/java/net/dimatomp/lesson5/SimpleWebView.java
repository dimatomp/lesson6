package net.dimatomp.lesson5;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class SimpleWebView extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_web_view);

        ((WebView) findViewById(R.id.web_view)).loadUrl(getIntent().getData().toString());
    }
}
