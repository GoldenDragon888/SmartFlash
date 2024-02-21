package au.smartflash.smartflash;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Button;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.Timestamp;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;

import au.smartflash.smartflash.model.Note;
import au.smartflash.smartflash.utils.DateUtils;
import jp.wasabeef.richeditor.RichEditor;

public class UserNotes extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout notesContainer;
    private EditText editTextContent;
    private Button btnSave, btnAdd, btnDelete, btnPickColor;
    private String currentUserId, currentEmail;
    private RichEditor richEditor; // Instance variable
    private CardView cardView; // Instance variable
    private String selectedColorForNewNote = "#d1edf2"; // Default color
    /*
      Adjusts the brightness of a color.
      @param color The original color.
      @param factor Brightness factor (greater than 1 to lighten, less than 1 to darken).
      @return The adjusted color.
     */
    private int adjustColorBrightness(int color, float brightnessFactor, float saturationFactor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);

        // Adjust the brightness (value)
        hsv[2] = Math.min(hsv[2] * brightnessFactor, 1.0f);

        // Reduce the saturation
        hsv[1] = Math.max(hsv[1] * saturationFactor, 0.0f);

        return Color.HSVToColor(hsv);
    }
    private static final long DOUBLE_CLICK_TIME_DELTA = 300; //milliseconds

    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5; // It's a dark color if the darkness is greater than 0.5
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        db = FirebaseFirestore.getInstance();
        notesContainer = findViewById(R.id.notesContainer);

        // In your onCreate or setupListeners method
        Button btnCreateNote = findViewById(R.id.btnCreateNote);
        btnCreateNote.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_green)));

        EditText editTextNewNote = findViewById(R.id.editTextNewNote);

        btnCreateNote.setOnClickListener(v -> {
            String newNoteName = editTextNewNote.getText().toString();
            if (!newNoteName.isEmpty()) {
                addNewNote(newNoteName, selectedColorForNewNote);
            } else {
                Toast.makeText(this, "Please enter a note name", Toast.LENGTH_SHORT).show();
            }
        });


        btnCreateNote.setOnClickListener(v -> {
            String newNoteName = editTextNewNote.getText().toString();
            if (!newNoteName.isEmpty()) {
                addNewNote(newNoteName, selectedColorForNewNote);
            } else {
                Toast.makeText(UserNotes.this, "Please enter a note name", Toast.LENGTH_SHORT).show();
            }
        });

        Button homeButton = findViewById(R.id.btnHome);
        homeButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.orange)));
        homeButton.setOnClickListener(v -> {
            finish();
        });
        Button btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_blue)));

        btnRefresh.setOnClickListener(v -> {
            refreshNotes();
        });
        btnPickColor = findViewById(R.id.btnPickColor); // Replace with your actual button ID
        btnPickColor.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.light_blue)));

        btnPickColor.setOnClickListener(v -> {
            showColorPickerDialog(color -> {
                updateColorPickerButton(color); // Update global color picker button
            });
        });

        EditText editTextSearch = findViewById(R.id.editTextSearch);
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Nothing needed here
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Call search method whenever text changes
                searchNotes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Nothing needed here
            }
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            currentEmail = user.getEmail();

            loadNotes();
        } else {
            // Handle user not logged in
        }
    }
    private HashSet<String> displayedNoteIds = new HashSet<>();

    private void searchNotes(String query) {
        if (query.isEmpty()) {
            loadNotes(); // Reload all notes if query is empty
            return;
        }

        notesContainer.removeAllViews();  // Clear the container
        displayedNoteIds.clear(); // Clear the tracking set

        db.collection("notes")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Note note = document.toObject(Note.class);
                            note.setId(document.getId());

                            // Check if the note is already displayed
                            if (!displayedNoteIds.contains(note.getId()) &&
                                    (note.getName().toLowerCase().contains(query.toLowerCase()) ||
                                            note.getContent().toLowerCase().contains(query.toLowerCase()))) {
                                addNoteToView(note);
                                displayedNoteIds.add(note.getId()); // Track the note as displayed
                            }
                        }
                    } else {
                        Log.e("Search", "Error searching notes", task.getException());
                        Toast.makeText(UserNotes.this, "Error searching notes", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setColorButtonListener(View view, int buttonId, final int color, AlertDialog dialog, Consumer<Integer> colorConsumer) {
        view.findViewById(buttonId).setOnClickListener(v -> {
            colorConsumer.accept(color);
            dialog.dismiss();
        });
    }
    private void editNoteName(Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Note Name");

        // Set up the input field
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(note.getName()); // Set the current name in the input field
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(note.getName())) {
                updateNoteNameInFirestore(note.getId(), newName);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateNoteNameInFirestore(String noteId, String newName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("notes").document(noteId)
                .update("name", newName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(UserNotes.this, "Note name updated", Toast.LENGTH_SHORT).show();
                    refreshNotes(); // Refresh to show updated name
                })
                .addOnFailureListener(e -> Toast.makeText(UserNotes.this, "Error updating name", Toast.LENGTH_SHORT).show());
    }


    private void showColorPickerDialog(final Consumer<Integer> colorConsumer) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_color_picker, null);
        builder.setView(view);
        final AlertDialog dialog = builder.create();

        setColorButtonListener(view, R.id.colorBlue, Color.BLUE, dialog, color -> {
            selectedColorForNewNote = String.format("#%06X", (0xFFFFFF & color));
            colorConsumer.accept(color);
            dialog.dismiss();
        });
        // Setting click listeners
        setColorButtonListener(view, R.id.colorBlue, Color.BLUE, dialog, colorConsumer);
        setColorButtonListener(view, R.id.colorBlack, Color.BLACK, dialog, colorConsumer);
        setColorButtonListener(view, R.id.colorWhite, Color.WHITE, dialog, colorConsumer);
        setColorButtonListener(view, R.id.colorDarkLightblue, getResources().getColor(R.color.light_blue), dialog, colorConsumer);
        setColorButtonListener(view, R.id.colormyback, getResources().getColor(R.color.my_background_color), dialog, colorConsumer);
        setColorButtonListener(view, R.id.colorWhite, getResources().getColor(R.color.white), dialog, colorConsumer);
        setColorButtonListener(view, R.id.colorDarkblue, getResources().getColor(R.color.dark_blue), dialog, colorConsumer);
        setColorButtonListener(view, R.id.colorDarkgreen, getResources().getColor(R.color.dark_green), dialog, colorConsumer);
        setColorButtonListener(view, R.id.colorDarkRed, getResources().getColor(R.color.dark_red), dialog, colorConsumer);
        setColorButtonListener(view, R.id.colorDarkPurple, getResources().getColor(R.color.dark_purple), dialog, colorConsumer);
        setColorButtonListener(view, R.id.colorFlashblue, getResources().getColor(R.color.flash_blue), dialog, colorConsumer);
        setColorButtonListener(view, R.id.colorOrange, getResources().getColor(R.color.orange), dialog, colorConsumer);
        setColorButtonListener(view, R.id.colorGray, getResources().getColor(R.color.gray), dialog, colorConsumer);
        setColorButtonListener(view, R.id.colorDarkgray, getResources().getColor(R.color.dark_gray), dialog, colorConsumer);
        setColorButtonListener(view, R.id.colorYellow, getResources().getColor(R.color.yellow), dialog, colorConsumer);
        setColorButtonListener(view, R.id.colorBlack, getResources().getColor(R.color.black), dialog, colorConsumer);

        // ... other color buttons ...

        dialog.show();
    }
    private void updateColorPickerButton(int color) {
        selectedColorForNewNote = String.format("#%06X", (0xFFFFFF & color));
        // Update the background tint list of the color picker button to the selected color
        btnPickColor.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    // Modify the updateNoteColor method
    private void updateNoteColor(String noteId, String hexColor, CardView noteCard, Runnable onCompletion) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> noteUpdates = new HashMap<>();
        noteUpdates.put("color", hexColor);

        db.collection("notes").document(noteId)
                .update(noteUpdates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(UserNotes.this, "Note color updated", Toast.LENGTH_SHORT).show();
                    noteCard.setCardBackgroundColor(Color.parseColor(hexColor));
                    onCompletion.run(); // This will run the refreshNotes method
                })
                .addOnFailureListener(e -> Toast.makeText(UserNotes.this, "Error updating color", Toast.LENGTH_SHORT).show());
    }
    private void refreshNotes() {
        // Clear the search field
        EditText editTextSearch = findViewById(R.id.editTextSearch);
        editTextSearch.setText("");

        // Clear existing views in the container to avoid duplicates
        notesContainer.removeAllViews();

        // Reload the notes
        loadNotes();
    }
    private boolean isLoadingNotes = false;

    private void loadNotes() {
        if (isLoadingNotes) return; // Avoid concurrent loading
        isLoadingNotes = true;

        // Clear existing views in the container to avoid duplicates
        notesContainer.removeAllViews();

        db.collection("notes")
                .whereEqualTo("userId", currentUserId)
                .orderBy("lastModified", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    isLoadingNotes = false;
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Note note = document.toObject(Note.class);
                            note.setId(document.getId()); // Manually set the ID
                            addNoteToView(note); // Add each note to the view
                        }
                    } else {
                        Log.e("FLAG", "Firestore - Error loading notes", task.getException());
                        Toast.makeText(this, "Error loading notes", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void addNoteToView(Note note) {
        // Initialize cardView
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 8);
        cardView.setLayoutParams(cardParams);
        cardView.setCardElevation(5);
        cardView.setRadius(15);

        // Retrieve and adjust the color from the note object
        String noteColorHex = note.getColor();
        int noteColor;
        int lighterNoteColor; // Variable to store the lighter color

        try {
            noteColor = Color.parseColor(noteColorHex);
            // Adjust brightness for a lighter shade
            lighterNoteColor = adjustColorBrightness(noteColor, 1.5f, 0.5f);

            // Set the original vibrant color as the background color of the CardView
            cardView.setCardBackgroundColor(noteColor);
        } catch (IllegalArgumentException e) {
            noteColor = Color.WHITE;
            lighterNoteColor = Color.WHITE; // Default to white if parsing fails
        }

        // Check if the background color is dark
        boolean isBackgroundColorDark = isColorDark(noteColor);

        // Create RichEditor with the lighter note color
        RichEditor richEditor = createRichEditor(note.getContent(), lighterNoteColor, note.getId());
        // Create buttons layout
        LinearLayout buttonsLayout = createButtonsLayout(note.getId(), cardView, richEditor);

        // Create header layout (name and hide button) and pass isBackgroundColorDark
        LinearLayout nameAndHideLayout = createNameAndHideLayout(note.getName(), richEditor, buttonsLayout, note, isBackgroundColorDark);

        // Create main layout for the note
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Add views to the main layout
        linearLayout.addView(nameAndHideLayout);
        linearLayout.addView(richEditor);
        linearLayout.addView(buttonsLayout);

        // Add main layout to the CardView
        cardView.addView(linearLayout);

        // Add the CardView to your main container
        notesContainer.addView(cardView, 0);
    }


    private RichEditor createRichEditor(String content, int bgColor, String noteId) {
        RichEditor richEditor = new RichEditor(this);
        richEditor.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        richEditor.setEditorHeight(200);
        richEditor.setEditorFontSize(16);
        richEditor.setEditorFontColor(Color.BLACK);
        richEditor.setPadding(10, 10, 10, 10);
        richEditor.setPlaceholder("Insert text here...");
        richEditor.setHtml(content);
        richEditor.setBackgroundColor(bgColor);  // Set the original note color
        richEditor.setVisibility(View.GONE);

        richEditor.setOnTextChangeListener(new RichEditor.OnTextChangeListener() {
            int charCountSinceLastSave = 0;

            @Override
            public void onTextChange(String text) {
                charCountSinceLastSave++;
                if (charCountSinceLastSave >= 10) {
                    saveNoteContent(noteId, text, false); // Autosave without refresh
                    charCountSinceLastSave = 0;
                }
            }
        });

        richEditor.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                saveNoteContent(noteId, richEditor.getHtml(), false); // Autosave on focus loss
            }
        });

        return richEditor;
    }
    private void saveNoteContent(String noteId, String content, boolean refreshOnCompletion) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> noteUpdates = new HashMap<>();
        noteUpdates.put("content", content);
        noteUpdates.put("lastModified", FieldValue.serverTimestamp());

        db.collection("notes").document(noteId)
                .update(noteUpdates)
                .addOnSuccessListener(aVoid -> {
                    if (refreshOnCompletion) {
                        refreshNotes();
                    }
                })
                .addOnFailureListener(e -> Log.e("AutoSave", "Error auto-saving note", e));
    }
    private LinearLayout createButtonsLayout(String noteId, CardView cardView, RichEditor richEditor) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.setVisibility(View.GONE);

        // Existing buttons
        layout.addView(createButton("Save", R.color.dark_green, noteId, cardView, richEditor));
        layout.addView(createButton("Delete", R.color.dark_red, noteId, cardView, richEditor));
        layout.addView(createButton("Pick Color", R.color.dark_purple, noteId, cardView, richEditor));

        // Add formatting buttons
        layout.addView(createFormatButton("Bold", richEditor));
        layout.addView(createFormatButton("Italic", richEditor));
        layout.addView(createFormatButton("Underline", richEditor));
        layout.addView(createFormatButton("Bullets", richEditor));

        return layout;
    }

    private Button createFormatButton(String formatType, RichEditor richEditor) {
        Button button = new Button(this);
        button.setText(formatType);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 100));
        button.setOnClickListener(view -> applyFormatting(formatType, richEditor));
        return button;
    }

    private void applyFormatting(String formatType, RichEditor richEditor) {
        switch (formatType) {
            case "Bold":
                richEditor.setBold();
                break;
            case "Italic":
                richEditor.setItalic();
                break;
            case "Underline":
                richEditor.setUnderline();
                break;
            case "Bullets":
                richEditor.setBullets();
                break;
        }
    }

    private Button createButton(String text, int colorResId, String noteId, CardView cardView, RichEditor richEditor) {
        Button button = new Button(this);
        button.setText(text);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 100));
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, colorResId)));

        switch (text) {
            case "Save":
                button.setOnClickListener(view -> saveCurrentNote(noteId, richEditor.getHtml(), () -> refreshNotes()));
                break;
            case "Delete":
                button.setOnClickListener(view -> showDeleteConfirmationDialog(noteId));
                break;
            case "Pick Color":
                button.setOnClickListener(view -> showColorPickerDialog(color -> updateNoteColor(noteId, String.format("#%06X", (0xFFFFFF & color)), cardView, () -> refreshNotes())));
                break;
        }
        return button;
    }
    private Drawable createCircleDrawable(int color, int diameterInPixels) {
        ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
        drawable.setIntrinsicHeight(diameterInPixels);
        drawable.setIntrinsicWidth(diameterInPixels);
        drawable.getPaint().setColor(color);
        return drawable;
    }

    private LinearLayout createNameAndHideLayout(String noteName, RichEditor richEditor, LinearLayout buttonsLayout, Note note, boolean isBackgroundColorDark) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView textViewName = new TextView(this);
        LinearLayout.LayoutParams textViewParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        textViewParams.gravity = Gravity.CENTER_VERTICAL;
        textViewName.setLayoutParams(textViewParams);
        textViewName.setText(noteName);
        textViewName.setTextSize(20);
        textViewName.setGravity(Gravity.CENTER_VERTICAL);

        // Set text color based on the background color
        textViewName.setTextColor(isBackgroundColorDark ? Color.WHITE : Color.BLACK);


        Button btnToggleVisibility = new Button(this);
        int dotDiameter = getResources().getDimensionPixelSize(R.dimen.dot_diameter);
        btnToggleVisibility.setBackground(createCircleDrawable(ContextCompat.getColor(this, R.color.dark_green), dotDiameter));
        // Add right margin to the button
        int rightMargin = getResources().getDimensionPixelSize(R.dimen.right_margin); // Define this value in your dimens.xml
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(dotDiameter, dotDiameter);
        buttonParams.gravity = Gravity.CENTER_VERTICAL;
        buttonParams.setMargins(0, 0, rightMargin, 0); // Set right margin here
        btnToggleVisibility.setLayoutParams(buttonParams);

        btnToggleVisibility.setOnClickListener(v -> {
            if (richEditor.getVisibility() == View.VISIBLE) {
                richEditor.setVisibility(View.GONE);
                buttonsLayout.setVisibility(View.GONE);
                btnToggleVisibility.setBackground(createCircleDrawable(ContextCompat.getColor(this, R.color.dark_green), dotDiameter));
            } else {
                richEditor.setVisibility(View.VISIBLE);
                buttonsLayout.setVisibility(View.VISIBLE);
                btnToggleVisibility.setBackground(createCircleDrawable(ContextCompat.getColor(this, R.color.dark_red), dotDiameter));
            }
        });
        // Add a listener for single and double clicks
        // Within the createNameAndHideLayout method or where you set the click listener
        final Note currentNote = note; // This is to make 'note' effectively final for the listener

        // Change in the listener to use the passed 'note' parameter
        textViewName.setOnClickListener(new View.OnClickListener() {
            private long lastClickTime = 0;

            @Override
            public void onClick(View view) {
                long clickTime = System.currentTimeMillis();
                if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                    // Double click detected
                    editNoteName(note);
                } else {
                    // Single click detected
                    new Handler().postDelayed(() -> {
                        if (System.currentTimeMillis() - clickTime >= DOUBLE_CLICK_TIME_DELTA) {
                            // Use the same toggleVisibility method
                            toggleVisibility(btnToggleVisibility, richEditor, buttonsLayout);
                        }
                    }, DOUBLE_CLICK_TIME_DELTA);
                }
                lastClickTime = clickTime;
            }
        });
        layout.addView(textViewName);
        layout.addView(btnToggleVisibility);

        return layout;
    }

    private void toggleVisibility(Button btnToggleVisibility, RichEditor richEditor, LinearLayout buttonsLayout) {
        int dotDiameter = getResources().getDimensionPixelSize(R.dimen.dot_diameter);

        if (richEditor.getVisibility() == View.VISIBLE) {
            richEditor.setVisibility(View.GONE);
            buttonsLayout.setVisibility(View.GONE);
            btnToggleVisibility.setBackground(createCircleDrawable(ContextCompat.getColor(this, R.color.dark_green), dotDiameter));
        } else {
            richEditor.setVisibility(View.VISIBLE);
            buttonsLayout.setVisibility(View.VISIBLE);
            btnToggleVisibility.setBackground(createCircleDrawable(ContextCompat.getColor(this, R.color.dark_red), dotDiameter));
        }
    }

    // Modify the saveCurrentNote method
    private void saveCurrentNote(String noteId, String updatedContent, Runnable onCompletion) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> noteUpdates = new HashMap<>();
        noteUpdates.put("content", updatedContent);
        noteUpdates.put("lastModified", FieldValue.serverTimestamp());

        db.collection("notes").document(noteId)
                .update(noteUpdates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(UserNotes.this, "Note saved successfully", Toast.LENGTH_SHORT).show();
                    onCompletion.run(); // This will run the refreshNotes method
                })
                .addOnFailureListener(e -> Toast.makeText(UserNotes.this, "Error saving note", Toast.LENGTH_SHORT).show());
    }

    private void addNewNote(String noteName, String color) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create a new note map
        Map<String, Object> newNote = new HashMap<>();
        newNote.put("name", noteName);
        newNote.put("content", ""); // Start with empty content
        newNote.put("createdAt", FieldValue.serverTimestamp());
        newNote.put("lastModified", FieldValue.serverTimestamp());
        newNote.put("color", color); // Use the passed color
        newNote.put("isEncrypted", "false");
        newNote.put("userId", currentUserId); // Assuming you have the userId stored
        newNote.put("color", selectedColorForNewNote); // Use the selected color

        // Add a new document with a generated ID
        db.collection("notes")
                .add(newNote)
                .addOnSuccessListener(documentReference -> Toast.makeText(UserNotes.this, "Note added successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(UserNotes.this, "Error adding note", Toast.LENGTH_SHORT).show());
    }

    private void showDeleteConfirmationDialog(String noteId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note, as it can't be recovered?")
                .setPositiveButton("Yes", (dialog, which) -> deleteCurrentNote(noteId))
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void deleteCurrentNote(String noteId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Delete the note from Firestore
        db.collection("notes").document(noteId)
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(UserNotes.this, "Note deleted successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(UserNotes.this, "Error deleting note", Toast.LENGTH_SHORT).show());
    }

}
