package com.wish.wishlist.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.wish.wishlist.R;
import com.wish.wishlist.test.Tester;
import com.wish.wishlist.test.WishService;

public class DebugActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        Button generateButton = (Button) findViewById(R.id.generate_wish_button);
        if (generateButton != null) {
            generateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Tester.getInstance().addWishes();
                }
            });
        }

        Button randomButton = (Button) findViewById(R.id.random_button);
        if (randomButton != null) {
            randomButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(DebugActivity.this, WishService.class);
                    startService(i);
                }
            });
        }
    }
}
