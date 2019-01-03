package com.example.module_a;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.dovar.router_annotation.Path;
import com.dovar.router_api.eventbus.EventCallback;
import com.dovar.router_api.router.Router;
import com.example.common_service.ServiceKey;

@Path(path = "/a/main")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.module_a_activity_main);

        findViewById(R.id.tv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Router.navigator("/c/main").navigateTo(MainActivity.this);
            }
        });

        findViewById(R.id.bt_jumpToSecond).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.bt_event_a).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString("content", "事件A");
                Router.publish(ServiceKey.EVENT_A, bundle);
            }
        });

        Router.subscribe(this, ServiceKey.EVENT_B, new EventCallback() {
            @Override
            public void onEvent(Bundle e) {
                Toast.makeText(MainActivity.this, "/a/main/收到事件B", Toast.LENGTH_SHORT).show();
            }
        });

        Router.subscribe(this, ServiceKey.EVENT_C, new EventCallback() {
            @Override
            public void onEvent(Bundle e) {
                Toast.makeText(MainActivity.this, "/a/main/收到事件C", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
