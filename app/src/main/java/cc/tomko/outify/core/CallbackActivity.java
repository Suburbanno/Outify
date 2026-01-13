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

    public static final String TAG = "CallbackActivity";

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
            handleOAuthVerify(uri);
        }
    }
		
    void handleOAuthVerify(Uri uri){
        Log.d(TAG, "Handling OAuth Verify Callback: " + uri.toString());
        String code = uri.getQueryParameter("code");
        String state = uri.getQueryParameter("state");

        final String at = MainActivity.auth.getAccessToken(code, state);
        Log.d(TAG, "handleOAuthVerify: " + at);
        finish();
    }
}
