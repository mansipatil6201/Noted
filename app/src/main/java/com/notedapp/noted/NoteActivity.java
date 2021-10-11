package com.notedapp.noted;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;

public class NoteActivity extends AppCompatActivity {

    private static final String EXTRA_NOTE_TITLE = "EXTRA_NOTE_TITLE";

    private boolean colourNavbar;
    private String title, note;
    private EditText noteText, titleText;
    private AlertDialog dialog;
    private int count=0;
    private SpeechRecognizer speechRecognizer;
    Intent speechRecognizerIntent;


    private @ColorInt
    int colourPrimary, colourFont, colourBackground;


    public static Intent getStartIntent(Context context, String title) {
        Intent intent = new Intent(context, NoteActivity.class);
        intent.putExtra(EXTRA_NOTE_TITLE, title);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        titleText = findViewById(R.id.et_title);
        noteText = findViewById(R.id.et_note);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        // If activity started from a share intent
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                noteText.setText(sharedText);
                note = sharedText;
                title = "";
            }
        } else { // If activity started from the notes list
            title = intent.getStringExtra(EXTRA_NOTE_TITLE);
            if (title == null || TextUtils.isEmpty(title)) {
                title = "";
                note = "";
                noteText.requestFocus();
                if (getSupportActionBar() != null)
                    getSupportActionBar().setTitle(getString(R.string.new_note));
            } else {
                titleText.setText(title);
                note = HelperUtils.readFile(NoteActivity.this, title);
                noteText.setText(note);
                if (getSupportActionBar() != null)
                    getSupportActionBar().setTitle(title);
            }
        }


        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},1);

        }


        speechRecognizer=SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizerIntent= new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        speechRecognizer.setRecognitionListener(new RecognitionListener()

        {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {

            }

            @Override
            public void onResults(Bundle results) {

                ArrayList<String> data = results.getStringArrayList(speechRecognizer.RESULTS_RECOGNITION);

                noteText.setText(data.get(0));



            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        getSettings(PreferenceManager.getDefaultSharedPreferences(NoteActivity.this));
        applySettings();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        note = noteText.getText().toString().trim();
        if (getCurrentFocus() != null)
            getCurrentFocus().clearFocus();
    }

    @Override
    public void onPause() {
        if (!isChangingConfigurations()) {
            saveFile();
        }
        if (dialog != null && dialog.isShowing())
            dialog.dismiss();
        dialog = null;
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_undo:
                noteText.setText(note);
                noteText.setSelection(noteText.getText().length());
                return (true);

            case R.id.btn_share:
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, noteText.getText().toString());
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_to)));
                return (true);

            case R.id.btn_speechToText:
                if(count==0)
                {
                    //imageButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_mic_24));

                    //start speech listening
                    speechRecognizer.startListening(speechRecognizerIntent);
                    count=1;
                }
                else
                {
                    //imageButton.setImageDrawable(getDrawable(R.drawable.ic_baseline_mic_off_24));

                    //stop listening
                    speechRecognizer.stopListening();
                    count=0;
                }
                return (true);


            case R.id.btn_delete:
                dialog = new AlertDialog.Builder(NoteActivity.this, R.style.AlertDialogTheme)
                        .setTitle(getString(R.string.confirm_delete))
                        .setMessage(getString(R.string.confirm_delete_text))
                        .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (HelperUtils.fileExists(NoteActivity.this, title)) {
                                    deleteFile(title + HelperUtils.TEXT_FILE_EXTENSION);
                                }
                                title = "";
                                note = "";
                                titleText.setText(title);
                                noteText.setText(note);
                                finish();
                            }
                        })
                        .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setIcon(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_delete_white_24dp))
                        .show();
                if (dialog.getWindow() != null) {
                    dialog.getWindow().getDecorView().setBackgroundColor(colourPrimary);
                }
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
                return (true);
        }
        return (super.onOptionsItemSelected(item));
    }

    private void getSettings(SharedPreferences preferences) {
        colourPrimary = preferences.getInt(HelperUtils.PREFERENCE_COLOUR_PRIMARY, ContextCompat.getColor(NoteActivity.this, R.color.colorPrimary));
        colourFont = preferences.getInt(HelperUtils.PREFERENCE_COLOUR_FONT, Color.BLACK);
        colourBackground = preferences.getInt(HelperUtils.PREFERENCE_COLOUR_BACKGROUND, Color.WHITE);
        colourNavbar = preferences.getBoolean(HelperUtils.PREFERENCE_COLOUR_NAVBAR, false);
    }

    private void applySettings() {
        HelperUtils.applyColours(NoteActivity.this, colourPrimary, colourNavbar);

        // Set text field underline colour
        noteText.setBackgroundTintList(ColorStateList.valueOf(colourPrimary));
        titleText.setBackgroundTintList(ColorStateList.valueOf(colourPrimary));

        // Set actionbar and background colour
        findViewById(R.id.scroll_view).setBackgroundColor(colourBackground);
        if (getSupportActionBar() != null)
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(colourPrimary));

        // Set font colours
        titleText.setTextColor(colourFont);
        noteText.setTextColor(colourFont);

        // Set hint colours
        titleText.setHintTextColor(ColorUtils.setAlphaComponent(colourFont, 120));
        noteText.setHintTextColor(ColorUtils.setAlphaComponent(colourFont, 120));
    }

    private void saveFile() {
        // Get current title and note
        String newTitle = titleText.getText().toString().trim().replace("/", " ");
        String newNote = noteText.getText().toString().trim();

        // Check if title and note are empty
        if (TextUtils.isEmpty(newTitle) && TextUtils.isEmpty(newNote)) {
            return;
        }

        // Check if title and note are unchanged
        if (newTitle.equals(title) && newNote.equals(note)) {
            return;
        }

        // Get file name to be saved if the title has changed or if it is empty
        if (!title.equals(newTitle) || TextUtils.isEmpty(newTitle)) {
            newTitle = newFileName(newTitle);
            titleText.setText(newTitle);
        }

        // Save the file with the new file name and content
        HelperUtils.writeFile(NoteActivity.this, newTitle, newNote);

        // If the title is not empty and the file name has changed then delete the old file
        if (!TextUtils.isEmpty(title) && !newTitle.equals(title)) {
            deleteFile(title + HelperUtils.TEXT_FILE_EXTENSION);
        }

        // Set the title to be the new saved title for when the home button is pressed
        title = newTitle;

    }

    private String newFileName(String name) {
        // If it is empty, give it a default title of "Note"
        if (TextUtils.isEmpty(name)) {
            name = getString(R.string.note);
        }
        // If the name already exists, append a number to it
        if (HelperUtils.fileExists(NoteActivity.this, name)) {
            int i = 1;
            while (true) {
                if (!HelperUtils.fileExists(NoteActivity.this, name + " (" + i + ")") || title.equals(name + " (" + i + ")")) {
                    name = (name + " (" + i + ")");
                    break;
                }
                i++;
            }
        }
        return name;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode==1)
        {
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "Permission Granted",Toast.LENGTH_SHORT);
            }
            else
            {
                Toast.makeText(this, "Permission Denied",Toast.LENGTH_SHORT);
            }
        }

    }

}
