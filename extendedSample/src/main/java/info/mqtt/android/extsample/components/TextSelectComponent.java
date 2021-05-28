package info.mqtt.android.extsample.components;


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.appcompat.app.AlertDialog;
import info.mqtt.android.extsample.R;
import timber.log.Timber;


public class TextSelectComponent extends RelativeLayout {

    private final TextView subLabel;

    private final String inputTitle;
    private final boolean numberInput;
    private final ArrayList<ITextSelectCallback> registeredCallbacks = new ArrayList<>();
    private final Context context;
    private String setText;

    public TextSelectComponent(Context context, AttributeSet attr) {
        super(context, attr);
        this.context = context;
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.text_select, this);
        TextView mainLabel = findViewById(R.id.mainLabel);
        this.subLabel = findViewById(R.id.subLabel);
        RelativeLayout textSelectLayout = findViewById(R.id.container);
        final TypedArray attributeArray = context.obtainStyledAttributes(attr, R.styleable.TextSelectComponent);
        mainLabel.setText(attributeArray.getString(R.styleable.TextSelectComponent_main_label));
        this.subLabel.setText(attributeArray.getString(R.styleable.TextSelectComponent_default_value));
        this.inputTitle = attributeArray.getString(R.styleable.TextSelectComponent_input_title);
        setText = attributeArray.getString(R.styleable.TextSelectComponent_default_value);
        this.numberInput = attributeArray.getBoolean(R.styleable.TextSelectComponent_numberTxt, false);
        textSelectLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog();
            }
        });
        attributeArray.recycle();
    }

    private void showInputDialog() {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams")
        View promptView = layoutInflater.inflate(R.layout.text_input_dialog, null);
        TextView promptText = promptView.findViewById(R.id.textView);
        promptText.setText(inputTitle);
        final EditText promptEditText = promptView.findViewById(R.id.edittext);
        if (this.numberInput) {
            Timber.i("NUMBER INPUT");
            promptEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        } else {
            Timber.i("NOT A NUMBER INPUT");
            promptEditText.setInputType(InputType.TYPE_CLASS_TEXT);
        }
        Timber.i("Setting text to: " + setText);
        Timber.i("input Type: " + promptEditText.getInputType());
        promptEditText.setText(setText);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(promptView);

        // Set up a dialog window
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String text = promptEditText.getText().toString();
                        subLabel.setText(text);
                        for (ITextSelectCallback callback : registeredCallbacks) {
                            callback.onTextUpdate(text);
                        }

                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        alertDialogBuilder.setOnKeyListener(new Dialog.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.cancel();
                }
                return true;
            }
        });

        // Create the alert Dialog
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    public void register(ITextSelectCallback callback) {
        registeredCallbacks.add(callback);
    }

    public String getSetText() {
        return setText;
    }

    public void setSetText(String setText) {
        this.setText = setText;

        this.subLabel.setText(setText);
    }

    public int getSetInt() {
        return Integer.parseInt(setText);
    }

    public void setSetInt(int value) {
        this.setText = String.valueOf(value);
    }

}
