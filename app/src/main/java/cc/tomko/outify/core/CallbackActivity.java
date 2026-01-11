package cc.tomko.outify.core;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.util.Log;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import cc.tomko.outify.MainActivity;
import cc.tomko.outify.R;

/**
 *  Handles "outify://xyz" callbacks
 */
public class CallbackActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_callback);
        handleIntent(getIntent());
    }

    void handleIntent(@Nullable Intent intent){
        if(intent == null) return;

        Uri uri = intent.getData();
        if(uri == null || !"outify".equals(uri.getScheme())) return;

        if("oauth".equals(uri.getHost()) && "/verify".equals(uri.getPath())){
            String code = uri.getQueryParameter("code");
            String state = uri.getQueryParameter("state");

            Log.d("CallbackActivity", "handleIntent: " + code);

            final String at = MainActivity.auth.getAccessToken(code);
            Log.d("CallbackActivity", "handleIntent: " + at);
            Toast.makeText(this, "Code: " + code, Toast.LENGTH_SHORT).show();
        }
    }
}