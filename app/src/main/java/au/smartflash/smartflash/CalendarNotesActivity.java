package au.smartflash.smartflash;

import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class CalendarNotesActivity extends AppCompatActivity {
    // UI components
    private CalendarView calendarView;
    private EditText dayNoteEditText, todoNoteEditText;

    // Data
    private String currentSelectedDate;
    private String dayNoteContent, todoNoteContent;

    // Database reference (Firebase Firestore, for example)
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_notes);

        // Initialize database
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        calendarView = findViewById(R.id.calendarView);
        dayNoteEditText = findViewById(R.id.dayNoteEditText);
        todoNoteEditText = findViewById(R.id.todoNoteEditText);

        // Calendar date change listener
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            currentSelectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
            fetchNotesForDate(currentSelectedDate);
        });

        // Add text change listeners for auto-save
        setupAutoSaveListeners();
    }

    private void fetchNotesForDate(String date) {
        // Fetch daynote and todonote from database for the selected date
        // Update dayNoteEditText and todoNoteEditText with the fetched content
    }

    private void setupAutoSaveListeners() {
        // Implement listeners for dayNoteEditText and todoNoteEditText
        // Trigger saveNote() method after certain conditions are met (e.g., character count or focus change)
    }

    private void saveNote(String noteType, String content) {
        // Save the note content to the database
        // noteType can be "daynote" or "todonote"
    }
}
