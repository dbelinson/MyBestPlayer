package com.simpity.android.media;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.simpity.android.media.Res;

public class XmlFileSelectActivity extends Activity {

	final static public int BROWSE_ACTIVITY = 200;
	final static public String PATH = "path";

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(Res.layout.xml_export);

        findViewById(Res.id.xml_browse_button).setOnClickListener(
        		new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						startActivityForResult(new Intent(
								XmlFileSelectActivity.this,
								XmlBrowseActivity.class), BROWSE_ACTIVITY);
					}
        		});

        findViewById(Res.id.xml_export_ok_button).setOnClickListener(
        		new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						EditText editor = (EditText)findViewById(Res.id.xml_export_path);
						String path = editor.getText().toString();
						try {
							FileInputStream in = new FileInputStream(path);
							in.close();
						} catch (FileNotFoundException e) {
							Toast.makeText(XmlFileSelectActivity.this,
									"File not found", Toast.LENGTH_LONG).show();
							return;
						} catch (IOException e) {
							e.printStackTrace();
						}

						Intent intent = new Intent();
						intent.putExtra(XmlFileSelectActivity.PATH, path);

						setResult(StreamMediaActivity.SUCCESS, intent);
						finish();
					}
        		});

        findViewById(Res.id.xml_export_cancel_button).setOnClickListener(
        		new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						finishActivity(StreamMediaActivity.CANCEL);
					}
        		});

        setResult(StreamMediaActivity.CANCEL);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == StreamMediaActivity.SUCCESS &&
				requestCode == BROWSE_ACTIVITY) {
			String path = data.getStringExtra(PATH);

			EditText editor = (EditText)findViewById(Res.id.xml_export_path);
			editor.setText(path);
			editor.invalidate();
		}
	}
}
