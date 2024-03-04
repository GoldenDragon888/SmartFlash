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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Consumer;

import au.smartflash.smartflash.model.EventsAdapter;
import au.smartflash.smartflash.model.Note;
import jp.wasabeef.richeditor.RichEditor;

import com.google.firebase.firestore.SetOptions;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

public class CalendarNotesActivity extends AppCompatActivity implements EventsAdapter.ItemClickListener {
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
    private ListView lvCalendarEvents;
    private ArrayAdapter<String> eventsAdapter;
    private List<String> eventsList;
    private RecyclerView rvCalendarEvents;
    private EventsAdapter adapter;
    private View dragHandle;
    private LinearLayout eventsContainer;
    private int initialHeight;
    private Date startDate, endDate;
    private static final int DAYS_TO_LOAD = 7; // Number of days to load at once
    private Button btnToday, btnSwitchviews, btnToggleCalendarEvents, btnToggleDayNotes, btnToggleTodoNotes, btnToggleInsights, btnRefresh, homeButton;
    private EditText editTextSearch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_notes);

        // Initialize Firebase Auth and Firestore
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Set user ID
        if (user != null && user.getEmail() != null) {
            userId = user.getEmail();
            // Continue with initializing views and fetching notes
            initializeViews();
            setupListeners();
            initializeDateRange();
            setupEventListScrolling();

            // Now fetch notes for today
            this.selectedDate = getCurrentFormattedDate();
            fetchNotesForDate(this.selectedDate);
            fetchDailyInsightsForDate(this.selectedDate);
        } else {
            // Handle the case where the user is not logged in or email is not available
        }
        // Set initial date and update headers
        selectedDate = getCurrentFormattedDate(); // Method to get current date formatted
        updateHeadersWithDate();
    }

    private void initializeViews() {
        // Initialize Calendar View
        calendarView = findViewById(R.id.calendarView);
        CalendarDay today = CalendarDay.today();
        calendarView.setCurrentDate(today);
        calendarView.setSelectedDate(today);

        SelectedDayDecorator selectedDayDecorator = new SelectedDayDecorator();
        calendarView.addDecorator(selectedDayDecorator);
        selectedDayDecorator.setDate(today);  // Update the decorator with the selected date
        calendarView.invalidateDecorators();  // Refresh the calendar view to show the decorator

        // Initialize RichEditors
        dayNoteRichEditor = findViewById(R.id.dayNoteRichEditor);
        todoNoteRichEditor = findViewById(R.id.todoNoteRichEditor);
        dailyInsightsRichEditor = findViewById(R.id.dailyInsightsRichEditor);

        // Initialize formatting buttons for each RichEditor
        LinearLayout dayNoteFormattingButtonsContainer = findViewById(R.id.dayNoteFormattingButtonsContainer);
        LinearLayout todoNoteFormattingButtonsContainer = findViewById(R.id.todoNoteFormattingButtonsContainer);
        LinearLayout dailyInsightsFormattingButtonsContainer = findViewById(R.id.dailyInsightsFormattingButtonsContainer);

        // Add formatting buttons layout to the containers
        dayNoteFormattingButtonsContainer.addView(createFormattingButtonsLayout(dayNoteRichEditor));
        todoNoteFormattingButtonsContainer.addView(createFormattingButtonsLayout(todoNoteRichEditor));
        dailyInsightsFormattingButtonsContainer.addView(createFormattingButtonsLayout(dailyInsightsRichEditor));

        // Ensure the visibility of the containers
        dayNoteFormattingButtonsContainer.setVisibility(View.VISIBLE);
        todoNoteFormattingButtonsContainer.setVisibility(View.VISIBLE);
        dailyInsightsFormattingButtonsContainer.setVisibility(View.VISIBLE);
        // Initialize Buttons and Search field
        saveNoteButton = findViewById(R.id.saveNoteButton);
        btnToday = findViewById(R.id.btnToday);
        btnToday.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_blue)));

        btnSwitchviews = findViewById(R.id.btnSwitchviews);
        btnSwitchviews.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.orange)));

        btnToggleCalendarEvents = findViewById(R.id.btnToggleCalendarEvents);
        btnToggleCalendarEvents.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.yellow)));

        btnToggleDayNotes = findViewById(R.id.btnToggleDayNotes);
        btnToggleDayNotes.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.yellow)));

        btnToggleTodoNotes = findViewById(R.id.btnToggleTodoNotes);
        btnToggleTodoNotes.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.yellow)));

        btnToggleInsights = findViewById(R.id.btnToggleInsights);
        btnToggleInsights.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.yellow)));

        btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_purple)));

        homeButton = findViewById(R.id.btnHome);
        homeButton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.orange)));

        editTextSearch = findViewById(R.id.editTextSearch);

        // Initialize Event Container and Drag Handle
        eventsContainer = findViewById(R.id.calendarEventsContainer);
        dragHandle = findViewById(R.id.drag_handle);

        // Initialize RecyclerView for Events
        rvCalendarEvents = findViewById(R.id.rvCalendarEvents);
        rvCalendarEvents.setLayoutManager(new LinearLayoutManager(this));

        // Set maximum height programmatically
        ViewGroup.LayoutParams params = rvCalendarEvents.getLayoutParams();
        params.height = getResources().getDimensionPixelSize(R.dimen.recyclerview_max_height); // Define this dimension in your dimens.xml
        rvCalendarEvents.setLayoutParams(params);

        eventsList = new ArrayList<>();
        adapter = new EventsAdapter(this, eventsList);
        adapter.setClickListener(this::onItemClick);
        rvCalendarEvents.setAdapter(adapter);
        rvCalendarEvents.setNestedScrollingEnabled(true);

        // Initialize Buttons and Search field
        saveNoteButton = findViewById(R.id.saveNoteButton);
        // Save button onClickListener for manual saves
        saveNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Change button color to indicate save is in progress
                setButtonColor(saveNoteButton, R.color.dark_red);

                // Manually save all notes
                String dayNoteId = userId + "_" + selectedDate;
                saveCurrentNote(dayNoteId, dayNoteRichEditor.getHtml(), saveNoteButton, "dayNotes", false);

                String todoNoteId = userId + "_" + selectedDate;
                saveCurrentNote(todoNoteId, todoNoteRichEditor.getHtml(), saveNoteButton, "todoNotes", false);

                String insightsNoteId = userId + "_" + selectedDate;
                saveCurrentNote(insightsNoteId, dailyInsightsRichEditor.getHtml(), saveNoteButton, "daily_insights", false);
            }
        });

        // Styling
        setButtonColor(saveNoteButton, R.color.dark_green);
    }

    private void setupListeners() {
        // Calendar listeners
        setupCalendarListeners();

        // Button click listeners
        btnToday.setOnClickListener(this::onTodayClicked);
        btnSwitchviews.setOnClickListener(this::onSwitchViewsClicked);
        btnRefresh.setOnClickListener(this::onRefreshClicked);
        homeButton.setOnClickListener(v -> finish());

        // Drag handle for event container
        setupDragHandleListener();

        // Note toggle buttons
        btnToggleCalendarEvents.setOnClickListener(v -> toggleVisibility(rvCalendarEvents, btnToggleCalendarEvents));
        btnToggleDayNotes.setOnClickListener(v -> toggleVisibility(dayNoteRichEditor, btnToggleDayNotes));
        btnToggleTodoNotes.setOnClickListener(v -> toggleVisibility(todoNoteRichEditor, btnToggleTodoNotes));
        btnToggleInsights.setOnClickListener(v -> toggleVisibility(dailyInsightsRichEditor, btnToggleInsights));

        // Text change listener for search
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // This method is called to notify you that, within s,
                // the count characters beginning at start are about to be replaced
                // with new text with length after.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // This method is called to notify you that, within s,
                // the count characters beginning at start have just replaced old text that had length before.
                // It is an error to attempt to make changes to s from this callback.
                searchNotes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // This method is called to notify you that, somewhere within s, the text has been changed.
            }
        });
    }
    private void setupCalendarListeners() {

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            this.selectedDate = String.format("%02d-%02d-%d", date.getDay(), date.getMonth() + 1, date.getYear());
            updateHeadersWithDate();
            fetchNotesForDate(this.selectedDate);
            fetchDailyInsightsForDate(this.selectedDate);
        });

        // Add more listeners as required
    }
    private void onTodayClicked(View v) {
        CalendarDay today = CalendarDay.today();
        calendarView.setCurrentDate(today);
        calendarView.setSelectedDate(today);
        this.selectedDate = getCurrentFormattedDate();
        fetchNotesForDate(this.selectedDate);
        fetchDailyInsightsForDate(this.selectedDate);
        updateHeadersWithDate();

    }
    private void onSwitchViewsClicked(View v) {
        switchNoteViews(); // This method should handle the logic to switch views
    }

    private void setupDragHandleListener() {
        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            private float initialTouchY;
            private int initialHeight;
            private boolean isDragging = false;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialTouchY = event.getRawY();
                        initialHeight = eventsContainer.getHeight();
                        isDragging = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (!isDragging) {
                            if (Math.abs(event.getRawY() - initialTouchY) > 10) {
                                isDragging = true;
                            }
                        } else {
                            int newHeight = initialHeight + (int) (initialTouchY - event.getRawY());
                            if (newHeight >= 100 && newHeight <= 500) { // Set min and max height
                                ViewGroup.LayoutParams params = eventsContainer.getLayoutParams();
                                params.height = newHeight;
                                eventsContainer.setLayoutParams(params);
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        isDragging = false;
                        return true;
                }
                return false;
            }
        });
    }
    private void onRefreshClicked(View v) {
        refreshNotes("dayNotes"); // Refresh day notes
        refreshNotes("todoNotes"); // Refresh todo notes
        // Add additional refresh logic if needed
    }
    @Override
    public void onItemClick(View view, int position) {
        String selectedEvent = adapter.getItem(position);
        focusOnDate(selectedEvent);
    }
    private void toggleVisibility(View view, Button toggleButton) {
        if (view == null) {
            return;
        }

        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.GONE);
            if (toggleButton.getId() == R.id.btnToggleCalendarEvents) {
                toggleButton.setText("Show Events");
            } else if (toggleButton.getId() == R.id.btnToggleInsights) {
                toggleButton.setText("Show Insights");
            } else {
                toggleButton.setText("Show");
            }
        } else {
            view.setVisibility(View.VISIBLE);
            if (toggleButton.getId() == R.id.btnToggleCalendarEvents) {
                toggleButton.setText("Hide Events");
            } else if (toggleButton.getId() == R.id.btnToggleInsights) {
                toggleButton.setText("Hide Insights");
            } else {
                toggleButton.setText("Hide");
            }
        }
    }


    private void initializeDateRange() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -DAYS_TO_LOAD / 2);
        startDate = calendar.getTime();
        calendar.add(Calendar.DATE, DAYS_TO_LOAD);
        endDate = calendar.getTime();

        // Log for debugging
        Log.d("CalendarNotesActivity", "Dates Initialized: Start=" + startDate + ", End=" + endDate);

        // Now call loadCalendarEventsForRange
        String formattedStartDate = toISO8601String(startDate);
        String formattedEndDate = toISO8601String(endDate);
        loadCalendarEventsForRange(formattedStartDate, formattedEndDate);
    }

    private void loadCalendarEventsForRange(String startDateStr, String endDateStr) {
        if (db == null) {
            Log.e("CalendarNotesActivity", "FirebaseFirestore not initialized");
            return;
        }

        db.collection("calendarEvents")
                .whereEqualTo("userEmail", FirebaseAuth.getInstance().getCurrentUser().getEmail())
                .whereGreaterThanOrEqualTo("start", startDateStr)
                .whereLessThanOrEqualTo("start", endDateStr)
                .orderBy("start")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int previousSize = eventsList.size(); // Size before adding new items

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String title = document.getString("title");
                            String start = document.getString("start");
                            String formattedDate = convertDateFormat(start);
                            String eventDetail = formattedDate + ": " + title;

                            if (!eventsList.contains(eventDetail)) {
                                eventsList.add(eventDetail);
                            }
                        }

                        // Notify the adapter of the range of data inserted
                        int newItemsCount = eventsList.size() - previousSize;
                        adapter.notifyItemRangeInserted(previousSize, newItemsCount);

                    } else {
                        Log.e("CalendarEvents", "Error fetching events", task.getException());
                    }
                });
    }

    private void setupEventListScrolling() {
        rvCalendarEvents.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                // Scrolling down to the bottom
                if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    loadMoreEvents(true);
                }

                // Scrolling up to the top
                if (!recyclerView.canScrollVertically(-1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    loadMoreEvents(false);
                }
            }
        });
    }

    private boolean isLoadingData = false;

    private void loadMoreEvents(boolean loadFutureEvents) {
        if (isLoadingData) {
            return; // Prevent loading new data if currently loading
        }

        isLoadingData = true;

        if (startDate == null || endDate == null) {
            initializeDateRange(); // Initialize dates if null
        }

        Calendar calendar = Calendar.getInstance();
        if (loadFutureEvents) {
            // Load future events
            calendar.setTime(endDate);
            calendar.add(Calendar.DATE, 1);
            startDate = calendar.getTime();
            calendar.add(Calendar.DATE, DAYS_TO_LOAD);
            endDate = calendar.getTime();
            loadCalendarEventsForRange(toISO8601String(startDate), toISO8601String(endDate));
        } else {
            // Load past events
            calendar.setTime(startDate);
            calendar.add(Calendar.DATE, -DAYS_TO_LOAD);
            Date newEndDate = startDate;
            calendar.add(Calendar.DATE, -1);
            startDate = calendar.getTime();

            loadPastCalendarEventsForRange(toISO8601String(startDate), toISO8601String(newEndDate));
        }
    }

    private void loadPastCalendarEventsForRange(String startDateStr, String endDateStr) {
        if (db == null) {
            isLoadingData = false; // Ensure flag is reset if Firestore is not initialized
            Log.e("CalendarNotesActivity", "FirebaseFirestore not initialized");
            return;
        }

        db.collection("calendarEvents")
                .whereEqualTo("userEmail", FirebaseAuth.getInstance().getCurrentUser().getEmail())
                .whereGreaterThanOrEqualTo("start", startDateStr)
                .whereLessThanOrEqualTo("start", endDateStr)
                .orderBy("start")
                .get()
                .addOnCompleteListener(task -> {
                    isLoadingData = false; // Reset flag after loading data

                    if (task.isSuccessful()) {
                        List<String> newEvents = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String title = document.getString("title");
                            String start = document.getString("start");
                            String formattedDate = convertDateFormat(start);
                            String eventDetail = formattedDate + ": " + title;

                            if (!eventsList.contains(eventDetail)) {
                                newEvents.add(eventDetail);
                            }
                        }

                        // Add new events to the beginning of the list
                        eventsList.addAll(0, newEvents);

                        // Notify the adapter about the new items added at the beginning
                        adapter.notifyItemRangeInserted(0, newEvents.size());

                    } else {
                        Log.e("CalendarEvents", "Error fetching past events", task.getException());
                    }
                });
    }

    private String convertDateFormat(String originalDate) {
        try {
            SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            SimpleDateFormat targetFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

            // Since 'Z' is literal for UTC, we should set the time zone to UTC
            originalFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            Date date = originalFormat.parse(originalDate);
            return targetFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return "Invalid Date";
        }
    }
    private String toISO8601String(Date date) {
        if (date == null) {
            Log.e("CalendarNotesActivity", "toISO8601String: Received null date");
            return ""; // Or handle this case as needed
        }
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return isoFormat.format(date);
    }

    private void focusOnDate(String eventDetails) {
        // Splitting eventDetails on ": " and getting the date part
        String[] parts = eventDetails.split(": ");
        String dateStr = parts[0]; // Date part is the first element

        try {
            SimpleDateFormat originalFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            Date date = originalFormat.parse(dateStr);

            // Using Calendar to get year, month, and day
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH); // January is 0
            int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

            CalendarDay day = CalendarDay.from(year, month + 1, dayOfMonth); // month + 1 because Calendar.MONTH is 0-based
            calendarView.setSelectedDate(day);
            calendarView.setCurrentDate(day);
            this.selectedDate = dateStr; // Update the selectedDate variable

            Log.d("focusOnDate", "Selected date: " + selectedDate);
            // Update the headers
            TextView dayNoteHeader = findViewById(R.id.dayNoteHeader);
            TextView todoNoteHeader = findViewById(R.id.todoNoteHeader);
            TextView insightsHeader = findViewById(R.id.dailyInsightsHeader);

            String headerSuffix = " - " + dateStr;
            dayNoteHeader.setText("Day Note" + headerSuffix);
            todoNoteHeader.setText("Todo Note" + headerSuffix);
            insightsHeader.setText("Insights" + headerSuffix);

            fetchNotesForDate(selectedDate);
            fetchDailyInsightsForDate(selectedDate);

        } catch (ParseException e) {
            e.printStackTrace();
            // Handle the error appropriately
        }
        // Find the first event of the selected day in the events list
        int positionOfFirstEvent = findFirstEventOfDate(dateStr);
        if (positionOfFirstEvent != -1) {
            // Scroll to the position of the first event
            rvCalendarEvents.scrollToPosition(positionOfFirstEvent);
        }
    }
    private int findFirstEventOfDate(String dateStr) {
        for (int i = 0; i < eventsList.size(); i++) {
            if (eventsList.get(i).startsWith(dateStr)) {
                return i; // Return the position of the first event of the day
            }
        }
        return -1; // Return -1 if no event is found for that day
    }
    private boolean isDayNoteOnTop = true;

    private void switchNoteViews() {
        // Get the parent LinearLayouts for the RichEditors and their headers
        LinearLayout dayNotesContainer = findViewById(R.id.daynotesContainer);
        LinearLayout todoNotesContainer = findViewById(R.id.todonotesContainer);

        // Get the headers, hide buttons, and formatting button containers
        TextView dayNoteHeader = findViewById(R.id.dayNoteHeader);
        TextView todoNoteHeader = findViewById(R.id.todoNoteHeader);
        MaterialButton btnToggleDayNotes = findViewById(R.id.btnToggleDayNotes);
        MaterialButton btnToggleTodoNotes = findViewById(R.id.btnToggleTodoNotes);
        LinearLayout dayNoteFormattingButtonsContainer = findViewById(R.id.dayNoteFormattingButtonsContainer);
        LinearLayout todoNoteFormattingButtonsContainer = findViewById(R.id.todoNoteFormattingButtonsContainer);

        // Remove views from their current parents
        removeViewFromParent(dayNoteHeader);
        removeViewFromParent(todoNoteHeader);
        removeViewFromParent(btnToggleDayNotes);
        removeViewFromParent(btnToggleTodoNotes);
        removeViewFromParent(dayNoteFormattingButtonsContainer);
        removeViewFromParent(todoNoteFormattingButtonsContainer);
        removeViewFromParent(dayNoteRichEditor);
        removeViewFromParent(todoNoteRichEditor);

        // Create header layouts for Day and Todo Notes
        LinearLayout dayHeaderLayout = createHeaderLayout(dayNoteHeader, btnToggleDayNotes);
        LinearLayout todoHeaderLayout = createHeaderLayout(todoNoteHeader, btnToggleTodoNotes);

        // Switch the headers, RichEditors, and formatting buttons
        if (isDayNoteOnTop) {
            // Switch to Todo Notes on top
            todoNotesContainer.addView(dayHeaderLayout);
            todoNotesContainer.addView(dayNoteFormattingButtonsContainer);
            todoNotesContainer.addView(dayNoteRichEditor);

            dayNotesContainer.addView(todoHeaderLayout);
            dayNotesContainer.addView(todoNoteFormattingButtonsContainer);
            dayNotesContainer.addView(todoNoteRichEditor);

            isDayNoteOnTop = false;
        } else {
            // Switch back to Day Notes on top
            dayNotesContainer.addView(dayHeaderLayout);
            dayNotesContainer.addView(dayNoteFormattingButtonsContainer);
            dayNotesContainer.addView(dayNoteRichEditor);

            todoNotesContainer.addView(todoHeaderLayout);
            todoNotesContainer.addView(todoNoteFormattingButtonsContainer);
            todoNotesContainer.addView(todoNoteRichEditor);

            isDayNoteOnTop = true;
        }
    }

    private void removeViewFromParent(View view) {
        if (view.getParent() != null) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
    }

    private LinearLayout createHeaderLayout(TextView header, MaterialButton toggleButton) {
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Add header text view to the layout
        LinearLayout.LayoutParams headerLayoutParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        header.setLayoutParams(headerLayoutParams);
        headerLayout.addView(header);

        // Define the desired height for the button, for example, 35dp
        int buttonHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 35, getResources().getDisplayMetrics());

        // Add toggle button to the layout with the defined height
        LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, buttonHeight);
        toggleButton.setLayoutParams(buttonLayoutParams);
        headerLayout.addView(toggleButton);

        return headerLayout;
    }
    private void updateHeadersWithDate() {
        String formattedDate = selectedDate; // Assuming selectedDate is already formatted

        String dayNoteHeaderFormat = getString(R.string.day_note_header_format, formattedDate);
        String todoNoteHeaderFormat = getString(R.string.todo_note_header_format, formattedDate);

        TextView dayNoteHeader = findViewById(R.id.dayNoteHeader);
        TextView todoNoteHeader = findViewById(R.id.todoNoteHeader);

        dayNoteHeader.setText(dayNoteHeaderFormat);
        todoNoteHeader.setText(todoNoteHeaderFormat);
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
        Log.d("focus","fetchnote userId: " + userId);

        if (userId != null && date != null) {
            fetchNote(userId, date, "dayNotes", Color.parseColor("#D1EDF2")); // Light blue for day notes
            fetchNote(userId, date, "todoNotes", Color.parseColor("#90EE90")); // Light green for todo notes
        }
    }
    private void fetchNote(String userId, String date, String collectionName, int bgColor) {
        String noteId = userId + "_" + date;
        Log.d("focus","fetchnote noteId: " + noteId);
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
        String[] formats = {"Bold", "Italic", "Underline", "Bullets", "Strikethrough", "Indent", "Outdent"};
        for (String format : formats) {
            layout.addView(createFormatButton(format, richEditor));
            Log.d("RichEditorDebug", "Added formatting button: " + format);
        }

        return layout;
    }

    /*private LinearLayout createFormattingButtonsLayout(RichEditor richEditor) {
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
    }*/
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
