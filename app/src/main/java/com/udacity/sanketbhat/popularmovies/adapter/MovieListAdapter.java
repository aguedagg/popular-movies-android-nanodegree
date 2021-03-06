/*
 * Copyright 2018 Sanket Bhat
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.udacity.sanketbhat.popularmovies.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.udacity.sanketbhat.popularmovies.R;
import com.udacity.sanketbhat.popularmovies.model.Movie;
import com.udacity.sanketbhat.popularmovies.ui.MovieListActivity;
import com.udacity.sanketbhat.popularmovies.util.ImageUrlBuilder;

import java.util.List;

public class MovieListAdapter extends RecyclerView.Adapter {

    //Two view types
    // -> 1. For normal movie grid item.
    // -> 2. For the loading indicator at the end.
    public static final int VIEW_TYPE_MOVIE = 1;
    public static final int VIEW_TYPE_PROGRESS = 2;

    private final MovieClickListener clickListener;
    private final Context mContext;
    private List<Movie> movies;

    //Boolean flag for showing or not showing the loading indicator at the end of the grid.
    private boolean loading = false;

    public MovieListAdapter(Context context, List<Movie> movies, MovieClickListener clickListener) {
        this.movies = movies;
        this.clickListener = clickListener;
        mContext = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_MOVIE:
                View inflatedView = LayoutInflater.from(parent.getContext()).inflate(R.layout.movie_grid_item, parent, false);
                return new ViewHolder(inflatedView);

            case VIEW_TYPE_PROGRESS:
                View progressView = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_loading_indicator, parent, false);
                return new ProgressViewHolder(progressView);

            default:
                throw new IllegalArgumentException("Unsupported View type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case VIEW_TYPE_MOVIE:
                if (holder instanceof ViewHolder) {
                    //Bind view with data
                    final ViewHolder viewHolder = (ViewHolder) holder;
                    final Movie movie = movies.get(position);
                    viewHolder.movieGenre.setText(movie.getGenresString());
                    viewHolder.movieName.setText(movie.getTitle());
                    Picasso.with(mContext)
                            .load(ImageUrlBuilder.getPosterUrlString(movie.getPosterPath()))
                            .tag(MovieListActivity.class.getSimpleName())
                            .error(R.drawable.ic_movie_grid_item_image_error)
                            .placeholder(R.drawable.ic_loading_indicator)
                            .into(viewHolder.target);
                }
                break;

            case VIEW_TYPE_PROGRESS:
                //progress is showing
                break;

            default:
                throw new IllegalArgumentException("Unsupported View type");
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        //Cleanup resources after viewHolder recycled
        if (holder instanceof ViewHolder) ((ViewHolder) holder).cleanUp();
    }

    @Override
    public int getItemCount() {
        //If movies = null return 0, or if next page is loading return size of movies + 1
        //Else its normal, return movies.size()
        if (movies == null) return 0;
        else if (loading) return movies.size() + 1;
        else return movies.size();
    }

    @Override
    public int getItemViewType(int position) {
        //If loading new page, return last element type as loading indicator
        if ((position == movies.size()) && isLoading()) return VIEW_TYPE_PROGRESS;
        else return VIEW_TYPE_MOVIE;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        if (this.loading != loading) {
            //If loading is finished due to error or successful response-
            //notify the list to hide the loading indicator
            this.loading = loading;
            notifyDataSetChanged();
        }
    }

    //Swap the new available list with the current one
    public void swapMovies(List<Movie> movies) {
        this.movies = movies;
        notifyDataSetChanged();
    }

    //ViewHolder for normal movie item.
    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final ImageView moviePoster;
        final TextView movieName;
        final TextView movieGenre;
        final CardView rootView;
        private int cardBackground = 0;

        final Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                try {
                    if (cardBackground == 0) {
                        Palette.from(bitmap).generate(palette -> {
                            Palette.Swatch swatch = palette.getVibrantSwatch();
                            if (swatch != null) {
                                rootView.setCardBackgroundColor((cardBackground = swatch.getRgb()));
                                movieName.setTextColor(swatch.getBodyTextColor());
                                movieGenre.setTextColor(swatch.getTitleTextColor());
                            }
                        });

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(mContext, "Error loading cardView background color", Toast.LENGTH_SHORT).show();
                }
                moviePoster.setImageBitmap(bitmap);
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                moviePoster.setImageDrawable(errorDrawable);
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };

        ViewHolder(View itemView) {
            super(itemView);
            rootView = (CardView) itemView;
            itemView.setOnClickListener(this);
            moviePoster = itemView.findViewById(R.id.movie_grid_item_image);
            movieName = itemView.findViewById(R.id.movie_grid_item_title);
            movieGenre = itemView.findViewById(R.id.movie_grid_item_genre);
        }

        @Override
        public void onClick(View v) {
            if (clickListener != null) {
                clickListener.onClickItem(v, movies.get(getAdapterPosition()));
            }
        }

        private void cleanUp() {
            //Cancel request once viewHolder recycled.
            Picasso.with(moviePoster.getContext())
                    .cancelRequest(moviePoster);
            moviePoster.setImageDrawable(null);
            cardBackground = 0;
        }
    }

    //View Holder for progress indicator
    class ProgressViewHolder extends RecyclerView.ViewHolder {
        ProgressViewHolder(View itemView) {
            super(itemView);
        }
    }
}
