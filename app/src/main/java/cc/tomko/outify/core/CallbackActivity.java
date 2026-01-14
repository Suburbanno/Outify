package cc.tomko.outify.core;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import cc.tomko.outify.OutifyApplication;
import cc.tomko.outify.R;
import cc.tomko.outify.TokenStore;

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
            handleOAuthVerify(uri);
            finish();
        }
    }
		
    void handleOAuthVerify(Uri uri){
        String code = uri.getQueryParameter("code");
        String state = uri.getQueryParameter("state");

        final String[] tokens = OutifyApplication.spAuthManager.getTokenPair(code, state);
        if(tokens.length != 2){
            Toast.makeText(this, "An error occurred", Toast.LENGTH_SHORT).show();
            return;
        }

        final TokenStore store = OutifyApplication.tokenStore;
        try {
            store.saveTokens(tokens[0], tokens[1]);
            Toast.makeText(this, "Credentials saved!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "An error occurred", Toast.LENGTH_SHORT).show();
            throw new RuntimeException(e);
        }
    }
}
