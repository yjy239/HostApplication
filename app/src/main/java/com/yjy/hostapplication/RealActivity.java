package com.yjy.hostapplication;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

/**
 * <pre>
 *     author : yjy
 *     e-mail : yujunyu12@gmail.com
 *     time   : 2018/08/17
 *     desc   :
 *     version: 1.0
 * </pre>
 */
public class RealActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.real_activity);
        Button btn = findViewById(R.id.btn);
        btn.setText("Real");
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        Button btn2 = findViewById(R.id.btn2);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Class clazz = HookManager.findClass("com.yjy.pluginapplication.MainActivity");
                if(clazz != null){
                    Log.e("clazz","find");
                }
            }
        });
    }
}
