package au.smartflash.smartflash;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.smartflash.smartflash.model.AICard;

public class CardPairsAdapter extends RecyclerView.Adapter<CardPairsAdapter.YourViewHolder> {
    private List<CategorySubcategoryPair> listOfPairs;
    private final OnCardClickListener listener;
    private Map<CategorySubcategoryPair, Integer> cardCountMap;
    private Set<Integer> selectedPositions = new HashSet<>();
    // Method to get the current list of pairs
    public List<CategorySubcategoryPair> getPairs() {
        return new ArrayList<>(listOfPairs); // Return a copy of the current list
    }
    public CardPairsAdapter(List<CategorySubcategoryPair> pairs, OnCardClickListener cardClickListener, Map<CategorySubcategoryPair, Integer> countMap) {
        this.listOfPairs = pairs;
        this.listener = cardClickListener;
        this.cardCountMap = countMap;
    }

    public void updateList(List<CategorySubcategoryPair> newListOfPairs, Map<CategorySubcategoryPair, Integer> newCardCountMap) {
        listOfPairs.clear();
        listOfPairs.addAll(newListOfPairs);
        cardCountMap.clear();
        cardCountMap.putAll(newCardCountMap);
        notifyDataSetChanged();
    }

    public void removeSelectedPairs() {
        List<CategorySubcategoryPair> toRemove = new ArrayList<>();
        for (Integer position : selectedPositions) {
            if (position >= 0 && position < listOfPairs.size()) {
                toRemove.add(listOfPairs.get(position));
            }
        }
        listOfPairs.removeAll(toRemove);
        selectedPositions.clear(); // Clear the selections after removal
        notifyDataSetChanged();
    }

    public void updatePairs(List<CategorySubcategoryPair> newPairs) {
        // Replace the current list with the new list of pairs.
        listOfPairs.clear();
        listOfPairs.addAll(newPairs);
        selectedPositions.clear(); // Optionally clear the selection as the data set has changed
        notifyDataSetChanged(); // Notify any registered observers that the data set has changed.
    }


    public int getItemCount() {
        return this.listOfPairs.size();
    }


    public void onBindViewHolder(YourViewHolder holder, int position) {
        CategorySubcategoryPair pair = this.listOfPairs.get(position);
        holder.tvCategory.setText(pair.getCategory());
        holder.tvSubcategory.setText(pair.getSubcategory());

        Integer cardCount = this.cardCountMap.get(pair);
        holder.tvCardCount.setText(cardCount != null ? String.valueOf(cardCount) : "0");


        boolean isSelected = selectedPositions.contains(position);
        if (isSelected) {
            holder.selectionCircle.setBackgroundResource(R.drawable.your_red_circle_drawable);
        } else {
            holder.selectionCircle.setBackgroundResource(R.drawable.your_circle_drawable);
        }
    }

    public YourViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new YourViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.cardpairlist_item, parent, false), this.listener);
    }

    public void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);
    }

    public boolean isSelected(int position) {
        return selectedPositions.contains(position);
    }


    public interface OnCardClickListener {
        void onCardClick(int position);
    }
    public List<CategorySubcategoryPair> getSelectedPairs() {
        List<CategorySubcategoryPair> selectedPairs = new ArrayList<>();
        for (Integer position : selectedPositions) {
            if (position >= 0 && position < listOfPairs.size()) {
                selectedPairs.add(listOfPairs.get(position));
            }
        }
        return selectedPairs;
    }
    public class YourViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        OnCardClickListener onCardClickListener;
        View selectionCircle;
        TextView tvCardCount;
        TextView tvCategory;
        TextView tvSubcategory;

        public YourViewHolder(View itemView, OnCardClickListener cardClickListener) {
            super(itemView);

            this.tvCategory = itemView.findViewById(R.id.tvCategory);
            this.tvSubcategory = itemView.findViewById(R.id.tvSubcategory);
            this.tvCardCount = itemView.findViewById(R.id.tvCardCount);
            this.selectionCircle = itemView.findViewById(R.id.selectionCircle);
            this.onCardClickListener = cardClickListener;
            itemView.setOnClickListener(this);
        }

        public void onClick(View view) {
            int position = getAdapterPosition();
            if (position != -1 && onCardClickListener != null) {
                onCardClickListener.onCardClick(position);
            }
        }
    }
}

