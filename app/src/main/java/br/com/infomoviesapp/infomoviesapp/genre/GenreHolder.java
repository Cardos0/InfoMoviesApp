package br.com.infomoviesapp.infomoviesapp.genre;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import br.com.infomoviesapp.infomoviesapp.R;

public class GenreHolder extends RecyclerView.ViewHolder {
    public TextView genreTextView;

    public GenreHolder(View itemView) {
        super(itemView);
        genreTextView = itemView.findViewById(R.id.genreTextView);
    }
}
