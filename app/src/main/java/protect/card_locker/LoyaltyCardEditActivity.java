package protect.card_locker;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;

public class LoyaltyCardEditActivity extends AppCompatActivity
{
    private static final String TAG = "Catima";

    protected static final int SELECT_BARCODE_REQUEST = 1;

    ImageView thumbnail;
    EditText storeFieldEdit;
    EditText noteFieldEdit;
    ChipGroup groupsChips;
    View cardAndBarcodeLayout;
    TextView cardIdFieldView;
    View barcodeTypeView;
    AutoCompleteTextView barcodeTypeField;
    ImageView barcodeImage;
    View barcodeImageLayout;
    View barcodeCaptureLayout;

    Button captureButton;
    Button enterButton;

    int loyaltyCardId;
    boolean updateLoyaltyCard;
    Uri importLoyaltyCardUri = null;
    Integer headingColorValue = null;

    DBHelper db;
    ImportURIHelper importUriHelper;

    boolean hasChanged = false;

    private void extractIntentFields(Intent intent)
    {
        final Bundle b = intent.getExtras();
        loyaltyCardId = b != null ? b.getInt("id") : 0;
        updateLoyaltyCard = b != null && b.getBoolean("update", false);
        importLoyaltyCardUri = intent.getData();

        Log.d(TAG, "View activity: id=" + loyaltyCardId
                + ", updateLoyaltyCard=" + Boolean.toString(updateLoyaltyCard));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.loyalty_card_edit_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        extractIntentFields(getIntent());

        db = new DBHelper(this);
        importUriHelper = new ImportURIHelper(this);

        thumbnail = findViewById(R.id.thumbnail);
        storeFieldEdit = findViewById(R.id.storeNameEdit);
        noteFieldEdit = findViewById(R.id.noteEdit);
        groupsChips = findViewById(R.id.groupChips);
        cardAndBarcodeLayout = findViewById(R.id.cardAndBarcodeLayout);
        cardIdFieldView = findViewById(R.id.cardIdView);
        barcodeTypeView = findViewById(R.id.barcodeTypeView);
        barcodeTypeField = findViewById(R.id.barcodeTypeField);
        barcodeImage = findViewById(R.id.barcode);
        barcodeImageLayout = findViewById(R.id.barcodeLayout);
        barcodeCaptureLayout = findViewById(R.id.barcodeCaptureLayout);

        captureButton = findViewById(R.id.captureButton);
        enterButton = findViewById(R.id.enterButton);

        storeFieldEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasChanged = true;

                generateIcon(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        cardIdFieldView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasChanged = true;

                String formatString = barcodeTypeField.getText().toString();

                if (!formatString.isEmpty()) {
                    if (formatString.equals(getString(R.string.noBarcode))) {
                        hideBarcode();
                    } else {
                        generateBarcode(s.toString(), BarcodeFormat.valueOf(formatString));
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        barcodeTypeField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                hasChanged = true;

                if (!s.toString().isEmpty()) {
                    if (s.toString().equals(getString(R.string.noBarcode))) {
                        hideBarcode();
                    } else {
                        generateBarcode(cardIdFieldView.getText().toString(), BarcodeFormat.valueOf(s.toString()));
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    @Override
    public void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        Log.i(TAG, "Received new intent");
        extractIntentFields(intent);

        // Reset these fields, so they are re-populated in onResume().
        storeFieldEdit.setText("");
        noteFieldEdit.setText("");
        cardIdFieldView.setText("");
        barcodeTypeField.setText("");
    }

    @Override
    public void onResume()
    {
        super.onResume();

        Log.i(TAG, "To view card: " + loyaltyCardId);

        if(updateLoyaltyCard)
        {
            final LoyaltyCard loyaltyCard = db.getLoyaltyCard(loyaltyCardId);
            if(loyaltyCard == null)
            {
                Log.w(TAG, "Could not lookup loyalty card " + loyaltyCardId);
                Toast.makeText(this, R.string.noCardExistsError, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            if(storeFieldEdit.getText().length() == 0)
            {
                storeFieldEdit.setText(loyaltyCard.store);
            }

            if(noteFieldEdit.getText().length() == 0)
            {
                noteFieldEdit.setText(loyaltyCard.note);
            }

            if(cardIdFieldView.getText().length() == 0)
            {
                cardIdFieldView.setText(loyaltyCard.cardId);
            }

            if(barcodeTypeField.getText().length() == 0)
            {
                barcodeTypeField.setText(loyaltyCard.barcodeType.isEmpty() ? getString(R.string.noBarcode) : loyaltyCard.barcodeType);
            }

            if(headingColorValue == null)
            {
                headingColorValue = loyaltyCard.headerColor;
                if(headingColorValue == null)
                {
                    headingColorValue = LetterBitmap.getDefaultColor(this, loyaltyCard.store);
                }
            }

            setTitle(R.string.editCardTitle);
        }
        else if(importLoyaltyCardUri != null)
        {
            // Try to parse
            LoyaltyCard importCard;
            try {
                importCard = importUriHelper.parse(importLoyaltyCardUri);
            } catch (InvalidObjectException ex) {
                Toast.makeText(this, R.string.failedParsingImportUriError, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            storeFieldEdit.setText(importCard.store);
            noteFieldEdit.setText(importCard.note);
            cardIdFieldView.setText(importCard.cardId);
            barcodeTypeField.setText(importCard.barcodeType);
            headingColorValue = importCard.headerColor;
        }
        else
        {
            setTitle(R.string.addCardTitle);
            hideBarcode();
        }

        if(groupsChips.getChildCount() == 0)
        {
            List<Group> existingGroups = db.getGroups();

            List<Group> loyaltyCardGroups = db.getLoyaltyCardGroups(loyaltyCardId);

            if (existingGroups.isEmpty()) {
                groupsChips.setVisibility(View.GONE);
            } else {
                groupsChips.setVisibility(View.VISIBLE);
            }

            for (Group group : db.getGroups()) {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.layout_chip_choice, groupsChips, false);
                chip.setText(group._id);
                chip.setTag(group);

                chip.setChecked(false);
                for (Group loyaltyCardGroup : loyaltyCardGroups) {
                    if (loyaltyCardGroup._id.equals(group._id)) {
                        chip.setChecked(true);
                        break;
                    }
                }

                groupsChips.addView(chip);
            }
        }

        if(headingColorValue == null)
        {
            // Select a random color to start out with.
            TypedArray colors = getResources().obtainTypedArray(R.array.letter_tile_colors);
            final int color = (int)(Math.random() * colors.length());
            headingColorValue = colors.getColor(color, Color.BLACK);
            colors.recycle();
        }

        thumbnail.setOnClickListener(new ColorSelectListener(headingColorValue));

        if(cardIdFieldView.getText().length() > 0 && barcodeTypeField.getText().length() > 0)
        {
            String formatString = barcodeTypeField.getText().toString();

            if(formatString.isEmpty() || formatString.equals(getString(R.string.noBarcode)))
            {
                hideBarcode();
            }
            else
            {
                final BarcodeFormat format = BarcodeFormat.valueOf(formatString);
                final String cardIdString = cardIdFieldView.getText().toString();

                generateBarcode(cardIdString, format);
            }
        }

        View.OnClickListener captureCallback = new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                IntentIntegrator integrator = new IntentIntegrator(LoyaltyCardEditActivity.this);
                integrator.setDesiredBarcodeFormats(BarcodeSelectorActivity.SUPPORTED_BARCODE_TYPES);

                String prompt = getResources().getString(R.string.scanCardBarcode);
                integrator.setPrompt(prompt);
                integrator.setBeepEnabled(false);
                integrator.initiateScan();
            }
        };

        captureButton.setOnClickListener(captureCallback);

        enterButton.setOnClickListener(new EditCardIdAndBarcode());
        barcodeImage.setOnClickListener(new EditCardIdAndBarcode());

        if(cardIdFieldView.getText().length() > 0)
        {
            cardAndBarcodeLayout.setVisibility(View.VISIBLE);
            enterButton.setText(R.string.editCard);
        }
        else
        {
            cardAndBarcodeLayout.setVisibility(View.GONE);
            enterButton.setText(R.string.enterCard);
        }

        ArrayList<String> barcodeList = new ArrayList<>(BarcodeSelectorActivity.SUPPORTED_BARCODE_TYPES);
        barcodeList.add(0, getString(R.string.noBarcode));
        ArrayAdapter<String> barcodeAdapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_item, barcodeList);
        barcodeTypeField.setAdapter(barcodeAdapter);

        FloatingActionButton saveButton = findViewById(R.id.fabSave);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSave();
            }
        });

        generateIcon(storeFieldEdit.getText().toString());

        hasChanged = false;
    }

    @Override
    public void onBackPressed() {
        askBeforeQuitIfChanged();
    }

    private void askBeforeQuitIfChanged() {
        if (!hasChanged) {
            finish();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.leaveWithoutSaveTitle);
        builder.setMessage(R.string.leaveWithoutSaveConfirmation);
        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                finish();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    class EditCardIdAndBarcode implements View.OnClickListener
    {
        @Override
        public void onClick(View v)
        {
            Intent i = new Intent(getApplicationContext(), BarcodeSelectorActivity.class);

            String cardId = cardIdFieldView.getText().toString();
            if(cardId.length() > 0)
            {
                final Bundle b = new Bundle();
                b.putString("initialCardId", cardId);
                i.putExtras(b);
            }

            startActivityForResult(i, SELECT_BARCODE_REQUEST);
        }
    }

    class ColorSelectListener implements View.OnClickListener
    {
        final int defaultColor;

        ColorSelectListener(int defaultColor)
        {
            this.defaultColor = defaultColor;
        }

        @Override
        public void onClick(View v)
        {
            ColorPickerDialog dialog = ColorPickerDialog.newBuilder().setColor(defaultColor).create();
            dialog.setColorPickerDialogListener(new ColorPickerDialogListener()
            {
                @Override
                public void onColorSelected(int dialogId, int color)
                {
                    headingColorValue = color;

                    generateIcon(storeFieldEdit.getText().toString());
                }

                @Override
                public void onDialogDismissed(int dialogId)
                {
                    // Nothing to do, no change made
                }
            });
            dialog.show(getFragmentManager(), "color-picker-dialog");
        }
    }

    private void doSave()
    {
        String store = storeFieldEdit.getText().toString();
        String note = noteFieldEdit.getText().toString();
        String cardId = cardIdFieldView.getText().toString();
        String barcodeType = barcodeTypeField.getText().toString();

        // We do not want to save the no barcode string to the database
        // it is simply an empty there for no barcode
        if(barcodeType.equals(getString(R.string.noBarcode)))
        {
            barcodeType = "";
        }

        if(store.isEmpty())
        {
            Snackbar.make(storeFieldEdit, R.string.noStoreError, Snackbar.LENGTH_LONG).show();
            return;
        }

        if(cardId.isEmpty())
        {
            Snackbar.make(cardIdFieldView, R.string.noCardIdError, Snackbar.LENGTH_LONG).show();
            return;
        }

        List<Group> selectedGroups = new ArrayList<>();

        for (Integer chipId : groupsChips.getCheckedChipIds()) {
            Chip chip = groupsChips.findViewById(chipId);
            selectedGroups.add((Group) chip.getTag());
        }

        if(updateLoyaltyCard)
        {   //update of "starStatus" not necessary, since it cannot be changed in this activity (only in ViewActivity)
            db.updateLoyaltyCard(loyaltyCardId, store, note, cardId, barcodeType, headingColorValue);
            Log.i(TAG, "Updated " + loyaltyCardId + " to " + cardId);
        }
        else
        {
            loyaltyCardId = (int)db.insertLoyaltyCard(store, note, cardId, barcodeType, headingColorValue, 0);
        }

        db.setLoyaltyCardGroups(loyaltyCardId, selectedGroups);

        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if(updateLoyaltyCard)
        {
            getMenuInflater().inflate(R.menu.card_update_menu, menu);
        }
        else
        {
            getMenuInflater().inflate(R.menu.card_add_menu, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        switch(id)
        {
            case android.R.id.home:
                askBeforeQuitIfChanged();
                break;

            case R.id.action_delete:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.deleteTitle);
                builder.setMessage(R.string.deleteConfirmation);
                builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        Log.e(TAG, "Deleting card: " + loyaltyCardId);

                        DBHelper db = new DBHelper(LoyaltyCardEditActivity.this);
                        db.deleteLoyaltyCard(loyaltyCardId);

                        ShortcutHelper.removeShortcut(LoyaltyCardEditActivity.this, loyaltyCardId);

                        finish();
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);

        String contents = null;
        String format = null;

        IntentResult result =
                IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (result != null)
        {
            Log.i(TAG, "Received barcode information from capture");
            contents = result.getContents();
            format = result.getFormatName();
        }

        if(requestCode == SELECT_BARCODE_REQUEST && resultCode == Activity.RESULT_OK)
        {
            Log.i(TAG, "Received barcode information from typing it");

            contents = intent.getStringExtra(BarcodeSelectorActivity.BARCODE_CONTENTS);
            format = intent.getStringExtra(BarcodeSelectorActivity.BARCODE_FORMAT);
        }

        if(contents != null && contents.isEmpty() == false &&
                format != null)
        {
            Log.i(TAG, "Read barcode id: " + contents);
            Log.i(TAG, "Read format: " + format);

            TextView cardIdView = findViewById(R.id.cardIdView);
            cardIdView.setText(contents);

            // Set special NO_BARCODE value to prevent onResume from overwriting it
            barcodeTypeField.setText(format.isEmpty() ? getString(R.string.noBarcode) : format);
            onResume();
        }
    }

    private void showBarcode() {
        barcodeImageLayout.setVisibility(View.VISIBLE);
    }

    private void hideBarcode() {
        barcodeImageLayout.setVisibility(View.GONE);
    }

    private void generateBarcode(final String cardId, final BarcodeFormat barcodeFormat) {
        if (barcodeImage.getHeight() == 0) {
            Log.d(TAG, "ImageView size is not known known at start, waiting for load");
            // The size of the ImageView is not yet available as it has not
            // yet been drawn. Wait for it to be drawn so the size is available.
            barcodeImage.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (Build.VERSION.SDK_INT < 16) {
                                barcodeImage.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                            } else {
                                barcodeImage.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }

                            Log.d(TAG, "ImageView size now known");
                            new BarcodeImageWriterTask(barcodeImage, cardId, barcodeFormat).execute();
                        }
                    });
        } else {
            Log.d(TAG, "ImageView size known known, creating barcode");
            new BarcodeImageWriterTask(barcodeImage, cardId, barcodeFormat).execute();
        }

        showBarcode();
    }

    private void generateIcon(String store) {
        if (headingColorValue == null) {
            return;
        }

        thumbnail.setBackgroundColor(headingColorValue);

        LetterBitmap letterBitmap = Utils.generateIcon(this, store, headingColorValue);

        if (letterBitmap != null) {
            thumbnail.setImageBitmap(letterBitmap.getLetterTile());
        } else {
            thumbnail.setImageBitmap(null);
        }

        thumbnail.setMinimumWidth(thumbnail.getHeight());
    }
}
