package au.smartflash.smartflash.model;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import au.smartflash.smartflash.R;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.ViewHolder> {

    private final List<String> mEvents;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // Data is passed into the constructor
    public EventsAdapter(Context context, List<String> events) {
        this.mInflater = LayoutInflater.from(context);
        this.mEvents = events != null ? events : new ArrayList<>();
    }

    // Inflates the row layout from XML
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.list_item_event, parent, false);
        return new ViewHolder(view);
    }

    // Binds data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String event = mEvents.get(position);
        holder.myTextView.setText(event);
    }

    // Total number of rows
    @Override
    public int getItemCount() {
        return mEvents.size();
    }

    // Stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView myTextView;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.textViewItem);
            itemView.setOnClickListener(this);
        }
        @Override
        public void onClick(View view) {
            // Save original background
            Drawable originalBackground = view.getBackground();

            // Change to your desired color (e.g., gray)
            view.setBackgroundColor(ContextCompat.getColor(view.getContext(), R.color.gray));

            // Use a Handler to revert to the original background after 1 second
            new Handler().postDelayed(() -> view.setBackground(originalBackground), 1000);

            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // Convenience method for getting data at click position
    // Make this method public
    public String getItem(int id) {
        return mEvents.get(id);
    }

    // Allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // Parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}

