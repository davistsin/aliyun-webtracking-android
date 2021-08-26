package wiki.qdc.aliyunwebtracking;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.davistsin.webtracking.Logger;
import com.davistsin.webtracking.LoggerConfig;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testLogger();
            }
        });

        Logger.init(this, "your project name", "cn-shenzhen", "Your logstore name",
                new LoggerConfig.Builder()
                        .enablePrint(true)
                        .setOfflineMode(true)
                        .setMaxOfflineNum(100)
                        .build()
        );

    }

    private void testLogger() {
        try {
            JSONObject object = new JSONObject();
            object.put("test", "123456789000");
            Logger.put(object.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}