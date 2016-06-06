package org.opencv.samples.tutorial3;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

public class StartActivity extends Activity {

    public final static String EXTRA_MESSAGE = "com.mycompany.StartActivity.MESSAGE";
    public final static String EXTRA_BOOL = "ChekBox";
    public final static String EXTRA_SOUP = "Soup";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
    }

    public void sendText(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        EditText editText = (EditText) findViewById(R.id.editText);
        CheckBox ocr = (CheckBox) findViewById(R.id.checkBox);
        boolean check = ocr.isChecked();
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra(EXTRA_BOOL, check);

        if (!check) {
            intent.putExtra(EXTRA_BOOL, check);
            EditText soup = (EditText) findViewById(R.id.Soup);
            intent.putExtra(EXTRA_SOUP, soup.getText().toString());
        }

        startActivity(intent);
    }
}
