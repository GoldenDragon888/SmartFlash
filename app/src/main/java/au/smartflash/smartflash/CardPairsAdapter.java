package au.smartflash.smartflash;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

import au.smartflash.smartflash.model.ImportedWord;

public class CardPairsAdapter extends RecyclerView.Adapter<CardPairsAdapter.YourViewHolder> {
    private Map<String, Integer> cardCountMap;

    private List<ImportedWord> listOfCardPairs;

    private final OnCardClickListener listener;

    public CardPairsAdapter(List<ImportedWord> paramList, OnCardClickListener paramOnCardClickListener, Map<String, Integer> paramMap) {
        this.listOfCardPairs = paramList;
        this.listener = paramOnCardClickListener;
        this.cardCountMap = paramMap;
    }

    public int getItemCount() {
        return this.listOfCardPairs.size();
    }

    public void onBindViewHolder(YourViewHolder paramYourViewHolder, int paramInt) {
        ImportedWord importedWord = this.listOfCardPairs.get(paramInt);
        paramYourViewHolder.tvCategory.setText(importedWord.getCategory());
        paramYourViewHolder.tvSubcategory.setText(importedWord.getSubcategory());
        String str = importedWord.getCategory() + "|" + importedWord.getSubcategory();
        paramYourViewHolder.tvCardCount.setText(String.valueOf(this.cardCountMap.get(str)));
        StringBuilder stringBuilder = (new StringBuilder()).append("CardPairsAdaptor Card at position ").append(paramInt).append(" is ");
        if (importedWord.isSelected()) {
            str = "selected";
        } else {
            str = "not selected";
        }
        Log.d("FLAG", stringBuilder.append(str).toString());
        if (importedWord.isSelected()) {
            paramYourViewHolder.selectionCircle.setBackgroundResource(2131231028);
        } else {
            paramYourViewHolder.selectionCircle.setBackgroundResource(2131231026);
        }
    }

    public YourViewHolder onCreateViewHolder(ViewGroup paramViewGroup, int paramInt) {
        return new YourViewHolder(LayoutInflater.from(paramViewGroup.getContext()).inflate(2131492904, paramViewGroup, false), this.listener);
    }

    public void toggleSelection(int paramInt) {
        ImportedWord importedWord = this.listOfCardPairs.get(paramInt);
        importedWord.setSelected(importedWord.isSelected() ^ true);
        notifyItemChanged(paramInt);
    }

    public static interface OnCardClickListener {
        void onCardClick(int param1Int);
    }

    public class YourViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        OnCardClickListener onCardClickListener;

        View selectionCircle;

        //final CardPairsAdapter this$0;

        TextView tvCardCount;

        TextView tvCategory;

        TextView tvSubcategory;

        public YourViewHolder(View param1View, OnCardClickListener param1OnCardClickListener) {
            super(param1View);

            this.tvCategory = (TextView) param1View.findViewById(R.id.tvCategory);
            this.tvSubcategory = (TextView) param1View.findViewById(R.id.tvSubcategory);
            this.tvCardCount = (TextView) param1View.findViewById(R.id.tvCardCount);
            this.selectionCircle = param1View.findViewById(R.id.selectionCircle);
            this.onCardClickListener = param1OnCardClickListener;
            param1View.setOnClickListener(this);
        }

        public void onClick(View param1View) {
            int i = getAdapterPosition();
            if (i != -1) {
                OnCardClickListener onCardClickListener = this.onCardClickListener;
                if (onCardClickListener != null)
                    onCardClickListener.onCardClick(i);
            }
        }
    }
}
