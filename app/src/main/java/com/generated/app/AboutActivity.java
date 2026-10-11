package com.generated.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

public class AboutActivity extends Activity {

    public static final String SITE_CREATOR_URL = "https://mitmachim.top/user/%D7%97%D7%99%D7%99%D7%90-%D7%A9%D7%99%D7%90%D7%95%D7%9E%D7%99";
    public static final String APP_CREATOR_URL = "https://mitmachim.top/user/%D7%A0%D7%95%D7%97%D7%A2%D7%9D-fm";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ImageButton btnBack = (ImageButton) findViewById(R.id.btnAboutBack);
        Button btnSiteCreator = (Button) findViewById(R.id.btnSiteCreatorProfile);
        Button btnAppCreator = (Button) findViewById(R.id.btnAppCreatorProfile);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSiteCreator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openExternalLink(SITE_CREATOR_URL);
            }
        });

        btnAppCreator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openExternalLink(APP_CREATOR_URL);
            }
        });
    }

    private void openExternalLink(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.no_app_to_open_link), Toast.LENGTH_SHORT).show();
        }
    }
}
