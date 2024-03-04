package au.smartflash.smartflash;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import au.smartflash.smartflash.model.Note;
import jp.wasabeef.richeditor.RichEditor;

import com.google.firebase.firestore.SetOptions;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

public class CalendarNotesActivity extends AppCompatActivity {
    // Change to MaterialCalendarView
    private MaterialCalendarView calendarView;
    // UI components
    //private CalendarView calendarView;
    private EditText dayNoteEditText, todoNoteEditText;

    // Data
    private String currentSelectedDate;
    private String dayNoteContent, todoNoteContent;

    // Database reference (Firebase Firestore, for example)
    private FirebaseFirestore db;
    private Button btnSave, btnAdd, btnDelete, btnPickColor;
    private String selectedColorForNewNote = "#d1edf2"; // Default color
    private LinearLayout notesContainer;
    private RichEditor dayNoteRichEditor, todoNoteRichEditor, dailyInsightsRichEditor;
    private String userId;
    private String formattedDate;
    private Button saveNoteButton;
    private Button saveTodoNoteButton;
    private String selectedDate;
    private boolean isTextChangedAfterSave = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_notes);

        // Initialize database
        db = FirebaseFirestore.getInstance();

        calendarView = findViewById(R.id.calendarView);
        dayNoteRichEditor = findViewById(R.id.dayNoteRichEditor);
        todoNoteRichEditor = findViewById(R.id.todoNoteRichEditor);
        // Initialize the RichEditor for daily insights
        dailyInsightsRichEditor = findViewById(R.id.dailyInsightsRichEditor);

        saveNoteButton = findViewById(R.id.saveNoteButton);
        setButtonColor(saveNoteButton, R.color.dark_green);


        // Get current user's email
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = (user != null) ? user.getEmail() : null;

        // Set today's date initially
        //formattedDate = getCurrentFormattedDate();
        // Initialize selected date with the current date
        this.selectedDate = getCurrentFormattedDate();

        //set some styles
        calendarView.setHeaderTextAppearance(R.style.CustomHeaderTextStyle);
        calendarView.setWeekDayTextAppearance(R.style.CustomWeekDayTextStyle);
        // Create a decorator for the selected day
        SelectedDayDecorator selectedDayDecorator = new SelectedDayDecorator();
        calendarView.addDecorator(selectedDayDecorator);

        // Date change listener for the calendar
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            this.selectedDate = String.format("%02d-%02d-%d", date.getDay(), date.getMonth() + 1, date.getYear());
            fetchNotesForDate(this.selectedDate);
            fetchDailyInsightsForDate(this.selectedDate);
            selectedDayDecorator.setDate(date);
            calendarView.invalidateDecorators(); // Refresh the decorator to update the selected day's look
        });

        // Highlight today's date
        CalendarDay today = CalendarDay.today();
        calendarView.setSelectedDate(today);
        selectedDayDecorator.setDate(today);

        Button btnToday = findViewById(R.id.btnToday);
        btnToday.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_blue)));

        btnToday.setOnClickListener(v -> {
            CalendarDay cal_today = CalendarDay.today();
            calendarView.setCurrentDate(cal_today);
            calendarView.setSelectedDate(cal_today);
            this.selectedDate = getCurrentFormattedDate();
            fetchNotesForDate(this.selectedDate);
            fetchDailyInsightsForDate(this.selectedDate);
            selectedDayDecorator.setDate(cal_today);
            calendarView.invalidateDecorators();
        });


        // Load notes for today
        this.selectedDate = getCurrentFormattedDate();
        fetchNotesForDate(this.selectedDate);
        fetchDailyInsightsForDate(this.selectedDate);

        Button btnSwitchviews = findViewById(R.id.btnSwitchviews);
        btnSwitchviews.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_purple)));

        btnSwitchviews.setOnClickListener(v -> {
            // Refresh day notes
            switchNoteViews();
        });

        saveNoteButton.setOnClickListener(v -> {
            // Change button appearance to indicate save is in progress
            saveNoteButton.setText("Saving...");
            saveNoteButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gray)));

            String dayNoteId = userId + "_" + this.selectedDate;
            String dayNoteContent = dayNoteRichEditor.getHtml();
            saveCurrentNote(dayNoteId, dayNoteContent, saveNoteButton, "dayNotes", false); // false for manual save

            String todoNoteId = userId + "_" + this.selectedDate;
            String todoNoteContent = todoNoteRichEditor.getHtml();
            saveCurrentNote(todoNoteId, todoNoteContent, saveNoteButton, "todoNotes", false); // false for manual save

            String insightsNoteId = userId + "_" + this.selectedDate;
            String insightsNoteContent = dailyInsightsRichEditor.getHtml();
            saveCurrentNote(insightsNoteId, insightsNoteContent, saveNoteButton, "daily_insights", false); // false for manual save

            // Optional: Add a delay to simulate save time
            new Handler().postDelayed(() -> {
                // Revert button appearance back to normal after save is complete
                saveNoteButton.setText("Save both Notes");
                saveNoteButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_green)));
            }, 2000); // 2000 milliseconds delay for demonstration
        });

        Button btnToggleDayNotes = findViewById(R.id.btnToggleDayNotes);
        btnToggleDayNotes.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.yellow)));
        btnToggleDayNotes.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.black)));

        Button btnToggleTodoNotes = findViewById(R.id.btnToggleTodoNotes);
        btnToggleTodoNotes.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.yellow)));
        btnToggleTodoNotes.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.black)));

        Button btnToggleInsights = findViewById(R.id.btnToggleInsights);
        btnToggleInsights.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.yellow)));
        btnToggleTodoNotes.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.black)));

        btnToggleDayNotes.setOnClickListener(v -> toggleVisibility(dayNoteRichEditor, btnToggleDayNotes));
        btnToggleTodoNotes.setOnClickListener(v -> toggleVisibility(todoNoteRichEditor, btnToggleTodoNotes));
        btnToggleInsights.setOnClickListener(v -> toggleVisibility(dailyInsightsRichEditor, btnToggleInsights));

        //add the formatting buttons to the editors.
        LinearLayout dayNoteFormattingButtonsContainer = findViewById(R.id.dayNoteFormattingButtonsContainer);
        LinearLayout todoNoteFormattingButtonsContainer = findViewById(R.id.todoNoteFormattingButtonsContainer);
        LinearLayout dailyInsightsFormattingButtonsContainer = findViewById(R.id.dailyInsightsFormattingButtonsContainer);

        LinearLayout dayNoteFormattingLayout = createFormattingButtonsLayout(dayNoteRichEditor);
        LinearLayout todoNoteFormattingLayout = createFormattingButtonsLayout(todoNoteRichEditor);
        LinearLayout dailyInsightsFormattingLayout = createFormattingButtonsLayout(dailyInsightsRichEditor);

        dayNoteFormattingButtonsContainer.addView(dayNoteFormattingLayout);
        todoNoteFormattingButtonsContainer.addView(todoNoteFormattingLayout);
        dailyInsightsFormattingButtonsContainer.addView(dailyInsightsFormattingLayout);

        dayNoteFormattingButtonsContainer.setVisibility(View.VISIBLE);
        todoNoteFormattingButtonsContainer.setVisibility(View.VISIBLE);
        dailyInsightsFormattingButtonsContainer.setVisibility(View.VISIBLE);

        Button homeButton = findViewById(R.id.btnHome);
        homeButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.orange)));
        homeButton.setOnClickListener(v -> {
            finish();
        });
        Button btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_purple)));

        btnRefresh.setOnClickListener(v -> {
            // Change button appearance to indicate refresh is in progress
            btnRefresh.setText("Refreshing...");
            btnRefresh.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gray)));

            // Refresh day notes
            refreshNotes("dayNotes");

            // Refresh todo notes
            refreshNotes("todoNotes");

            // Optional: Add a delay to simulate refresh time
            new Handler().postDelayed(() -> {
                // Revert button appearance back to normal after refresh is complete
                btnRefresh.setText("Refresh");
                btnRefresh.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_purple)));
            }, 1000); // 1000 milliseconds delay for demonstration
        });
        // Initialize notesContainer
        //notesContainer = findViewById(R.id.notesContainer);

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
        int charCountSinceLastSave = 0;
        // Add text change listeners for auto-save
        //setupAutoSaveListeners();
    }
    private void toggleVisibility(View view, Button toggleButton) {
        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.GONE);
            toggleButton.setText("Show");
        } else {
            view.setVisibility(View.VISIBLE);
            toggleButton.setText("Hide");
        }
    }
    private boolean isDayNoteOnTop = true;

    private void switchNoteViews() {
        // Get the parent LinearLayouts for the RichEditors and their headers
        LinearLayout dayNotesContainer = findViewById(R.id.daynotesContainer);
        LinearLayout todoNotesContainer = findViewById(R.id.todonotesContainer);

        // Get the headers for the day note and todo note
        TextView dayNoteHeader = findViewById(R.id.dayNoteHeader); // Adjust ID as per your layout
        TextView todoNoteHeader = findViewById(R.id.todoNoteHeader); // Adjust ID as per your layout

        // Remove RichEditors and headers from their current containers
        dayNotesContainer.removeAllViews();
        todoNotesContainer.removeAllViews();

        // Switch the RichEditors and headers
        if (isDayNoteOnTop) {
            todoNotesContainer.addView(dayNoteHeader);
            todoNotesContainer.addView(dayNoteRichEditor);
            dayNotesContainer.addView(todoNoteHeader);
            dayNotesContainer.addView(todoNoteRichEditor);
            isDayNoteOnTop = false;
        } else {
            dayNotesContainer.addView(dayNoteHeader);
            dayNotesContainer.addView(dayNoteRichEditor);
            todoNotesContainer.addView(todoNoteHeader);
            todoNotesContainer.addView(todoNoteRichEditor);
            isDayNoteOnTop = true;
        }
    }


    private HashSet<String> displayedNoteIds = new HashSet<>();
    private String getCurrentFormattedDate() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1; // Month is 0-based, so add 1
        int year = calendar.get(Calendar.YEAR);

        // Format the date as "DD-MM-YYYY"
        return String.format("%02d-%02d-%d", day, month, year);
    }
    private void searchNotes(String query) {
        if (query.isEmpty()) {
            fetchNotesForDate(null); // Reload all notes if query is empty
            return;
        }

        notesContainer.removeAllViews();  // Clear the container
        displayedNoteIds.clear(); // Clear the tracking set

        db.collection("notes")
                //.whereEqualTo("userId", currentUserId)
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
                                //addNoteToView(note);
                                displayedNoteIds.add(note.getId()); // Track the note as displayed
                            }
                        }
                    } else {
                        Log.e("Search", "Error searching notes", task.getException());
                        Toast.makeText(CalendarNotesActivity.this, "Error searching notes", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private final int defaultDayNoteBgColor = Color.parseColor("#D1EDF2"); // Light blue
    private final int defaultTodoNoteBgColor = Color.parseColor("#90EE90"); // Light green

    private void fetchNotesForDate(String date) {
        if (userId != null && date != null) {
            fetchNote(userId, date, "dayNotes", Color.parseColor("#D1EDF2")); // Light blue for day notes
            fetchNote(userId, date, "todoNotes", Color.parseColor("#90EE90")); // Light green for todo notes
        }
    }
    private void fetchNote(String userId, String date, String collectionName, int bgColor) {
        String noteId = userId + "_" + date;
        db.collection(collectionName).document(noteId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String content = documentSnapshot.exists() ? documentSnapshot.getString("content") : "";
                    RichEditor noteRichEditor = createRichEditor(content, bgColor, noteId, collectionName);


                    // Set content and make it visible
                    noteRichEditor.setHtml(content);
                    noteRichEditor.setVisibility(View.VISIBLE);

                    // Log the content for debugging
                    Log.d("FLAG", "CalendarNotesActivity - Fetched Note Content: " + content);

                    // Add RichEditor to layout
                    if (collectionName.equals("dayNotes")) {
                        dayNoteRichEditor.setHtml(content);
                        dayNoteRichEditor.setVisibility(View.VISIBLE);
                    } else if (collectionName.equals("todoNotes")) {
                        todoNoteRichEditor.setHtml(content);
                        todoNoteRichEditor.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(CalendarNotesActivity.this, "Error fetching note", Toast.LENGTH_SHORT).show());
    }
    private void refreshNotes(String noteType) {
        // Clear the search field and existing views in the container
        EditText editTextSearch = findViewById(R.id.editTextSearch);
        editTextSearch.setText("");

        // Use the selected date from the calendar, not the current date
        String dateToFetch = (this.selectedDate != null && !this.selectedDate.isEmpty()) ? this.selectedDate : getCurrentFormattedDate();

        // Fetch and update both dayNote and todoNote based on the selected date
        fetchAndDisplayNote("dayNotes", dateToFetch);
        fetchAndDisplayNote("todoNotes", dateToFetch);
        fetchDailyInsightsForDate(dateToFetch);
    }

    private void fetchAndDisplayNote(String collectionName, String date) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String noteId = userId + "_" + date;

        db.collection(collectionName).document(noteId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String content = documentSnapshot.getString("content");
                        if (collectionName.equals("dayNotes")) {
                            dayNoteRichEditor.setHtml(content);
                            dayNoteRichEditor.setVisibility(View.VISIBLE);
                        } else if (collectionName.equals("todoNotes")) {
                            todoNoteRichEditor.setHtml(content);
                            todoNoteRichEditor.setVisibility(View.VISIBLE);
                        }
                    } else {
                        // Handle case when there is no content for the selected date
                        if (collectionName.equals("dayNotes")) {
                            dayNoteRichEditor.setHtml("Enter new dayNote");
                            dayNoteRichEditor.setVisibility(View.VISIBLE);
                        } else if (collectionName.equals("todoNotes")) {
                            todoNoteRichEditor.setHtml("Enter new todoNote");
                            todoNoteRichEditor.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching note", Toast.LENGTH_SHORT).show());
    }

    private void setColorButtonListener(View view, int buttonId, final int color, AlertDialog dialog, Consumer<Integer> colorConsumer) {
        view.findViewById(buttonId).setOnClickListener(v -> {
            colorConsumer.accept(color);
            dialog.dismiss();
        });
    }
    private void fetchDailyInsightsForDate(String date) {
        if (userId != null && date != null) {
            String insightsId = userId + "_" + date;
            db.collection("daily_insights").document(insightsId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String content = documentSnapshot.exists() ? documentSnapshot.getString("content") : "";
                        dailyInsightsRichEditor.setHtml(content);
                        dailyInsightsRichEditor.setVisibility(View.VISIBLE);
                    })
                    .addOnFailureListener(e -> Toast.makeText(CalendarNotesActivity.this, "Error fetching daily insights", Toast.LENGTH_SHORT).show());
        }
    }
    private RichEditor createRichEditor(String content, int bgColor, String noteId, String collectionName) {
        RichEditor richEditor = new RichEditor(this);

        richEditor.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        richEditor.setEditorHeight(200);
        richEditor.setEditorFontSize(16);
        richEditor.setEditorFontColor(Color.BLACK);
        richEditor.setPadding(10, 10, 10, 10);
        richEditor.setPlaceholder("Insert text here...");
        richEditor.setHtml(content);

        // Convert bgColor to hex string
        Log.d ("FLAG", "createRicheditor bgcolor: " + bgColor);
        String hexColor = String.format("#%06X", (0xFFFFFF & bgColor));
        String htmlContent = "<html><body style='background-color: " + hexColor + ";'>" + content + "</body></html>";
        richEditor.setHtml(htmlContent);
        richEditor.setVisibility(View.VISIBLE);
        dayNoteRichEditor.setOnTextChangeListener(new RichEditor.OnTextChangeListener() {
            @Override
            public void onTextChange(String text) {
                // Change button color to red when text changes
                setButtonColor(saveNoteButton, R.color.dark_red);
            }
        });
        todoNoteRichEditor.setOnTextChangeListener(new RichEditor.OnTextChangeListener() {
            @Override
            public void onTextChange(String text) {
                // Change button color to red when text changes
                setButtonColor(saveNoteButton, R.color.dark_red);
            }
        });
        dayNoteRichEditor.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String dayNoteId = userId + "_" + this.selectedDate;
                saveCurrentNote(dayNoteId, dayNoteRichEditor.getHtml(), saveNoteButton, "dayNotes", true); // true for autoSave
            }
        });

        todoNoteRichEditor.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String todoNoteId = userId + "_" + this.selectedDate;
                saveCurrentNote(todoNoteId, todoNoteRichEditor.getHtml(), saveNoteButton, "todoNotes", true); // true for autoSave
            }
        });
        dailyInsightsRichEditor .setOnTextChangeListener(new RichEditor.OnTextChangeListener() {
            @Override
            public void onTextChange(String text) {
                // Change button color to red when text changes
                setButtonColor(saveNoteButton, R.color.dark_red);
            }
        });
        dailyInsightsRichEditor .setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String insightsNoteId = userId + "_" + this.selectedDate;
                saveCurrentNote(insightsNoteId, dailyInsightsRichEditor.getHtml(), saveNoteButton, "daily_insights", true); // true for autoSave
            }
        });
        // Inject JavaScript for checkbox functionality
        injectCheckboxScript(richEditor);

        return richEditor;
    }

    private void saveCurrentNote(String noteId, String content, Button saveButton, String collectionName, boolean isAutoSave) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentDeviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Proceed with saving the note with the provided content
        db.collection(collectionName).document(noteId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String lastEditedBy = documentSnapshot.getString("lastEditedBy");
                    if (!currentDeviceId.equals(lastEditedBy) && lastEditedBy != null) {
                        Toast.makeText(CalendarNotesActivity.this, "This note might have been edited on another device.", Toast.LENGTH_LONG).show();
                    }

                    // Prepare and save the note
                    Map<String, Object> noteUpdates = new HashMap<>();
                    noteUpdates.put("content", content);
                    noteUpdates.put("lastModified", FieldValue.serverTimestamp());
                    noteUpdates.put("lastEditedBy", currentDeviceId);

                    db.collection(collectionName).document(noteId)
                            .set(noteUpdates, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                if (!isAutoSave) {
                                    setButtonColor(saveButton, R.color.dark_green);
                                    Toast.makeText(CalendarNotesActivity.this, "Note saved successfully", Toast.LENGTH_SHORT).show();
                                    setButtonColor(saveButton, R.color.dark_green);

                                    // Refresh notes only if it's a manual save
                                    refreshNotes(collectionName);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(CalendarNotesActivity.this, "Error saving note", Toast.LENGTH_SHORT).show();
                                Log.e("FLAG", "saveCurrentNote - Error for Note ID: " + noteId + "; Error: " + e.getMessage(), e);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CalendarNotesActivity.this, "Error checking note status", Toast.LENGTH_SHORT).show();
                });
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
            case "Strikethrough":
                richEditor.setStrikeThrough();
                break;
            case "Indent":
                richEditor.setIndent();
                break;
            case "Outdent":
                richEditor.setOutdent();
                break;
        }
    }


    private Button createFormatButton(String formatType, RichEditor richEditor) {
        Button button = new Button(this);
        String buttonText = formatType.substring(0, 1).toUpperCase() + formatType.substring(1).toLowerCase();
        button.setText(buttonText);

        // Smaller text size
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);

        // Apply bold text style
        button.setTypeface(button.getTypeface(), Typeface.BOLD);

        // Adjusting the button height
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                0, 60, 1.0f);
        buttonParams.setMargins(5, 0, 5, 0);
        button.setLayoutParams(buttonParams);

        button.setBackgroundColor(ContextCompat.getColor(this, R.color.light_blue));
        button.setTextColor(Color.BLACK);

        // Adjust internal padding
        button.setPadding(2, 2, 2, 2);

        button.setSingleLine(true);
        button.setEllipsize(TextUtils.TruncateAt.END);

        button.setOnClickListener(view -> {
            applyFormatting(formatType, richEditor);
            toggleButtonState(button);
        });

        return button;
    }

    private void toggleButtonState(Button button) {
        Object tag = button.getTag();
        boolean isPressed = tag != null && (boolean) tag;

        if (isPressed) {
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.light_blue));
            button.setTextColor(Color.BLACK); // Black text on light blue
            button.setTag(false);
        } else {
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_blue));
            button.setTextColor(Color.WHITE); // White text on dark blue
            button.setTag(true);
        }
    }

/*
    private void setupAutoSaveListeners() {
        dayNoteRichEditor.setOnTextChangeListener(new RichEditor.OnTextChangeListener() {
            int charCountSinceLastSave = 0;

            @Override
            public void onTextChange(String text) {
                if (charCountSinceLastSave == 0) {
                    setButtonColor(saveNoteButton, R.color.dark_red);
                }
                charCountSinceLastSave++;
                if (charCountSinceLastSave >= 10) {
                    String noteId = userId + "_" + CalendarNotesActivity.this.selectedDate;
                    saveCurrentNote(noteId, text, saveNoteButton, "dayNotes", true);
                    saveCurrentNote(noteId, text, saveNoteButton, "todoNotes", true);
                    setButtonColor(saveNoteButton, R.color.dark_green);
                }
            }
        });

    }*/

    // Helper method to set button color
    private void setButtonColor(Button button, int colorResId) {
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, colorResId)));
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
                .addOnSuccessListener(aVoid -> Toast.makeText(CalendarNotesActivity.this, "Note deleted successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(CalendarNotesActivity.this, "Error deleting note", Toast.LENGTH_SHORT).show());
    }
    private int adjustColorBrightness(int color, float brightnessFactor, float saturationFactor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);

        // Adjust the brightness (value)
        hsv[2] = Math.min(hsv[2] * brightnessFactor, 1.0f);

        // Reduce the saturation
        hsv[1] = Math.max(hsv[1] * saturationFactor, 0.0f);

        return Color.HSVToColor(hsv);
    }
    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5; // It's a dark color if the darkness is greater than 0.5
    }
    /*
        btnPickColor = findViewById(R.id.btnPickColor);
        if (btnPickColor != null) {
            btnPickColor.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_blue)));
        }

        btnPickColor.setOnClickListener(v -> {
            showColorPickerDialog(color -> {
                updateColorPickerButton(color); // Update global color picker button
            });
        });
        */
    private void updateColorPickerButton(int color) {
        selectedColorForNewNote = String.format("#%06X", (0xFFFFFF & color));
        // Update the background tint list of the color picker button to the selected color
        btnPickColor.setBackgroundTintList(ColorStateList.valueOf(color));
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

    private LinearLayout createFormattingButtonsLayout(RichEditor richEditor) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Add formatting buttons
        layout.addView(createFormatButton("Bold", richEditor));
        layout.addView(createFormatButton("Italic", richEditor));
        layout.addView(createFormatButton("Underline", richEditor));
        layout.addView(createFormatButton("Bullets", richEditor));
        layout.addView(createFormatButton("Strikethrough", richEditor));
        layout.addView(createFormatButton("Indent", richEditor));
        layout.addView(createFormatButton("Outdent", richEditor));
        //layout.addView(createCheckboxButton(richEditor)); // Add the checkbox button

        return layout;
    }
    /*private LinearLayout createButtonsLayout(String noteId, RichEditor richEditor) {
        // Parent layout
        LinearLayout parentLayout = new LinearLayout(this);
        parentLayout.setOrientation(LinearLayout.VERTICAL);
        parentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Layout for formatting buttons
        LinearLayout formatLayout = createFormattingButtonsLayout(richEditor);

        // Layout for action buttons
        LinearLayout actionLayout = new LinearLayout(this);
        actionLayout.setOrientation(LinearLayout.HORIZONTAL);
        actionLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Add action buttons
        actionLayout.addView(createActionButton("Save", R.color.dark_green, noteId, richEditor));
        actionLayout.addView(createActionButton("Pick Color", R.color.dark_purple, noteId, richEditor));

        // Add both layouts to the parent layout
        parentLayout.addView(formatLayout);
        parentLayout.addView(actionLayout);

        return parentLayout;
    }

    private Button createActionButton(String action, int colorResId, String noteId, RichEditor richEditor) {
        Button button = new Button(this);
        button.setText(action);
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, colorResId)));
        button.setTextColor(Color.WHITE);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f));

        switch (action) {
            case "Save":
                button.setOnClickListener(v -> {
                    String content = richEditor.getHtml();
                    //Remove this for now because I don't think i need this.
                    //saveCurrentNote(noteId, content, button, collectionName); // Add the collectionName parameter
                });
                break;
            case "Pick Color":
                button.setOnClickListener(v -> showColorPickerDialog(color -> {
                    // Update note color and refresh layout
                    updateNoteColor(noteId, String.format("#%06X", (0xFFFFFF & color)), richEditor, () -> refreshNotes());
                }));
                break;
        }

        return button;
    }*/
     /*private Button createButton(String text, int colorResId, String noteId, RichEditor richEditor) {
        Button button = new Button(new ContextThemeWrapper(this, R.style.CustomButtonStyle), null, 0);
        String buttonText = text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
        button.setText(buttonText);
        Log.d("ButtonCreation", "Button Text: " + buttonText);

        // Adjust height here
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                0, 80, 1f); // Adjust the height as needed
        button.setLayoutParams(buttonParams);
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, colorResId)));
        button.setTextColor(Color.WHITE);

        // Define button behavior based on its text
        switch (buttonText) {
            case "Save":
                button.setOnClickListener(view -> saveCurrentNote(noteId, richEditor, () -> refreshNotes()));
                break;

            case "Pick Color": // Ensure this matches exactly with buttonText
                button.setOnClickListener(view -> showColorPickerDialog(color -> updateNoteColor(noteId, String.format("#%06X", (0xFFFFFF & color)), richEditor, () -> refreshNotes())));
                break;
        }

        return button;
    }*/
    private void updateNoteColor(String noteId, String hexColor, RichEditor richEditor, Runnable onCompletion) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> noteUpdates = new HashMap<>();
        noteUpdates.put("color", hexColor);

        db.collection("notes").document(noteId)
                .update(noteUpdates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CalendarNotesActivity.this, "Note color updated", Toast.LENGTH_SHORT).show();
                    richEditor.setBackgroundColor(Color.parseColor(hexColor)); // Update RichEditor's background color
                    if (onCompletion != null) {
                        onCompletion.run(); // Run the completion handler if provided
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(CalendarNotesActivity.this, "Error updating color", Toast.LENGTH_SHORT).show());
    }
    private void injectCheckboxScript(RichEditor richEditor) {
        String js = "function insertCheckbox() {" +
                "  var editor = document.getElementById('editor');" +
                "  var checkbox = document.createElement('input');" +
                "  checkbox.type = 'checkbox';" +
                "  checkbox.onclick = function() { this.checked = !this.checked; };" +
                "  editor.appendChild(checkbox);" +
                "}";
        richEditor.evaluateJavascript(js, null);
    }

    private Button createCheckboxButton(RichEditor richEditor) {
        Button checkboxButton = new Button(this);
        checkboxButton.setText("Checkbox");
        checkboxButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12); // Align font size with other buttons
        checkboxButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 90)); // Match size with other buttons
        checkboxButton.setBackgroundColor(ContextCompat.getColor(this, R.color.light_blue));
        checkboxButton.setTextColor(Color.BLACK);
        checkboxButton.setTag(false); // Initial state is not pressed

        // Add margin between buttons
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) checkboxButton.getLayoutParams();
        params.setMargins(5, 0, 5, 0); // Align margins with other buttons
        checkboxButton.setLayoutParams(params);

        checkboxButton.setOnClickListener(view -> {
            richEditor.evaluateJavascript("insertCheckbox();", null);
            toggleButtonState(checkboxButton); // Add this to toggle the background color
        });

        return checkboxButton;
    }
    private Drawable createCircleDrawable(int color, int diameterInPixels) {
        ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
        drawable.setIntrinsicHeight(diameterInPixels);
        drawable.setIntrinsicWidth(diameterInPixels);
        drawable.getPaint().setColor(color);
        return drawable;
    }
    // Add this class definition inside the same file, but outside the CalendarNotesActivity class
    public class SelectedDayDecorator implements DayViewDecorator {
        private CalendarDay selectedDate;

        public SelectedDayDecorator() {
            selectedDate = CalendarDay.today();
        }

        public void setDate(CalendarDay date) {
            selectedDate = date;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return selectedDate != null && day.equals(selectedDate);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.setSelectionDrawable(ContextCompat.getDrawable(CalendarNotesActivity.this, R.drawable.your_blue_circle_drawable));
        }
    }
}
