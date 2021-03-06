package br.com.infomoviesapp.infomoviesapp.activitys;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import br.com.infomoviesapp.infomoviesapp.helpers.Alerts;
import br.com.infomoviesapp.infomoviesapp.helpers.App;
import br.com.infomoviesapp.infomoviesapp.helpers.AppStatus;
import br.com.infomoviesapp.infomoviesapp.R;
import br.com.infomoviesapp.infomoviesapp.helpers.GoToActivity;
import br.com.infomoviesapp.infomoviesapp.helpers.InfoMoviesAppMenuItem;
import br.com.infomoviesapp.infomoviesapp.helpers.SetupHeader;
import br.com.infomoviesapp.infomoviesapp.helpers.WebServicePath;
import br.com.infomoviesapp.infomoviesapp.movie.Movie;
import br.com.infomoviesapp.infomoviesapp.movie.MovieAdapter;

/** Classe responsável por mostrar filmes por gênero
 */
public class MovieByGenreActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    private int idGenre;
    private String nameGenre;
    private RecyclerView movieRecyclerView;
    private MovieAdapter movieAdapter;
    private List <Movie> movies;
    private String[] urls = new String[3];
    private ProgressBar loadingProgressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DrawerLayout drawerLayout;
    private Toolbar mainToolBar;
    private NavigationView navigationView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movies_by_genre);

        /* Mostrar loading */
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        loadingProgressBar.setVisibility(View.VISIBLE);

        /* Pegar id do Genero */
        getExtras();
        movies = new ArrayList<>();

        /* Menu e Toolbar */
        mainToolBar = findViewById(R.id.mainToolBar);
        navigationView = findViewById(R.id.navView);
        drawerLayout = findViewById(R.id.drawerLayout);
        String title = nameGenre;
        new SetupHeader(this, mainToolBar, navigationView, drawerLayout, title);

        /* Swipe */
        swipeRefreshLayout = findViewById(R.id.moviesSwipeRefreshLayout);

        /* Construir ReciclerView */
        movieRecyclerView = findViewById(R.id.movieRecyclerView);
        setupRecycler();

        /* Montar Urls para solicitar os filmes no onResume*/
        WebServicePath webServicePath = new WebServicePath();
        webServicePath.setPathMovieByGenre();
        webServicePath.addOthers(App.getResourses().getString(R.string.with_genres, idGenre));

        int page = 1;

        // Acessar 3 páginas, são 20 filmes por páginas precisamos de 50
        while (page <= 3){
            webServicePath.setPage(page);
            urls[page-1] = webServicePath.getUrl();
            Log.d("DEBUG/URL_MOVIE/" + page, urls[page-1]);
            page++;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        getMovies();
    }

    @Override
    protected void onStop() {
        super.onStop();

        clearMovies();
    }



    /** Método privado responsável por capturar o id do gênero que será utilizado na requisição do webservice
     */
    private void getExtras(){
        if (getIntent().hasExtra("idGenre")){
            int id = getIntent().getIntExtra("idGenre", -1);
            setIdGenre(id);
        }
        if (getIntent().hasExtra("nameGenre")){
            String name = getIntent().getStringExtra("nameGenre");
            setNameGenre(name);
        }
    }

    /**
     * Método privado chamado para inicializar o Recycler
     */
    private void setupRecycler() {

        // Configurando o gerenciador de layout para ser uma lista.
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        movieRecyclerView.setLayoutManager(layoutManager);

        // Adiciona o adapter que irá anexar os objetos à lista.
        // Está sendo criado com lista vazia, pois será preenchida posteriormente.
        movieAdapter = new MovieAdapter(this, movies);
        movieRecyclerView.setAdapter(movieAdapter);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshContent();
            }

            private void refreshContent() {
                getMovies();
                swipeRefreshLayout.setRefreshing(false);

            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        InfoMoviesAppMenuItem infoMoviesAppMenuItem = new InfoMoviesAppMenuItem(this, item);
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void setIdGenre(int idGenre) {
        this.idGenre = idGenre;
    }

    public void setNameGenre(String nameGenre) {
        this.nameGenre = nameGenre;
    }

    /**
     * Classe privada chamada para obter os filmes do web service
     *  @version 1.0
     *  @since 1.0
     */
    private class getMovieList extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            try {
                StringBuilder stringBuilder = new StringBuilder("");
                stringBuilder.append("{\"moviePages\":[");

                for (String stringUrl: urls) {
                    if (stringUrl != urls[0]){
                        stringBuilder.append(",");
                    }
                    URL url = new URL(stringUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    InputStream stream = connection.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(stream);
                    //try with resources
                    try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                        String linha = null;
                        String json = "";
                        while ((linha = reader.readLine()) != null)
                            stringBuilder.append(linha);
                    }
                    connection.disconnect();
                }
                stringBuilder.append("]}");
                return stringBuilder.toString();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String jsonS) {
            if (jsonS != null) {
                try {
                    JSONObject json = new JSONObject(jsonS);
                    JSONArray pages = json.getJSONArray("moviePages");

                    int moviesCount = 0;

                    for (int i = 0; i < pages.length() && moviesCount < 50; i++) {                           // Paginas
                        JSONObject page = pages.getJSONObject(i);
                        JSONArray results = page.getJSONArray("results");

                        for (int j = 0; j < results.length() && moviesCount < 50; j++) {                     // Filmes por Páginas
                            JSONObject movie = results.getJSONObject(j);
                            String title = movie.getString("title");
                            int id = movie.getInt("id");
                            String poster_path = movie.getString("poster_path");
                            Movie newMovie = new Movie(poster_path, id, title);
                            movies.add(newMovie);
                            movieAdapter.notifyDataSetChanged();
                            Log.d("Add MOVIE " + Integer.toString(moviesCount), String.format("ID: %d, Title: %s, Poster Path: %s", id, title, poster_path));
                            moviesCount++;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }else{
                // Se não houver Json

            }
            loadingProgressBar.setVisibility(View.GONE);
        }
    }
    private void clearMovies(){
        movies.clear();
        movieAdapter.notifyDataSetChanged();
    }

    private void getMovies(){
        clearMovies();
        if (AppStatus.getInstance(MovieByGenreActivity.this).isOnline()){
            new getMovieList().execute(urls);
        }else{
            Alerts alerts = new Alerts(MovieByGenreActivity.this);
            alerts.noConnection();
        }
    }
}
