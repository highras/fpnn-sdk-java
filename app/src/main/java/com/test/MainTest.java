package com.test;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;

public class MainTest extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_test);

        System.out.println(new String("Test with activity!"));
        byte[] bytes = this.loadKey("key/test-secp256k1-public.der-false");
        new TestCase(bytes);
    }

    private byte[] loadKey(String derPath) {

        byte[] bytes = new byte[0];
        InputStream inputStream = null;

        try {

            inputStream = getAssets().open(derPath);

            int len = -1;
            int size = inputStream.available();
            bytes = new byte[size];

            inputStream.read(bytes);
            inputStream.close();
        } catch (IOException ex) {

            System.err.println(ex);
        }

        return bytes;
    }
}
