package sanp.mp100.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import sanp.mp100.R;
import sanp.mp100.ui.test.BusinessPlatformPostmanTestActivity;
import sanp.mp100.ui.test.BusinessPlatformTestActivity;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_test_business_platform:
                startActivity(new Intent(getApplicationContext(), BusinessPlatformTestActivity.class));
                break;
            case R.id.button_test_business_platform_postman:
                startActivity(new Intent(getApplicationContext(), BusinessPlatformPostmanTestActivity.class));
                break;
        }
    }
}
