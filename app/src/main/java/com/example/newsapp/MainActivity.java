package com.example.newsapp;

import androidx.appcompat.app.AppCompatActivity;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.SearchManager;

import android.content.Context;
import android.content.Intent;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newsapp.adapter.Adapter;
import com.example.newsapp.api.ApiClient;
import com.example.newsapp.api.ApiInterface;
import com.example.newsapp.models.Article;
import com.example.newsapp.models.News;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {


    public static final String API_KEY = "9838fe9fa4c049d1a8d58fffbbfcae53";
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private List<Article> articles = new ArrayList<>();
    private Adapter adapter;
    private String TAG = MainActivity.class.getSimpleName();

    private SwipeRefreshLayout swipeRefreshLayout;
    private Toolbar toolbarMain;


    private RelativeLayout errorLayout;
    private TextView topHeadline;
    private ImageView errorImage;
    private TextView errorTitle, errorMessage;
    private Button btnRetry;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        toolbarMain = findViewById(R.id.toolbarMain);
        setSupportActionBar(toolbarMain);
        getSupportActionBar().setTitle("Popular News");
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);



        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);

        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);


       recyclerView = findViewById(R.id.recyclerView);
       layoutManager = new LinearLayoutManager(MainActivity.this);
       recyclerView.setLayoutManager(layoutManager);
       recyclerView.setItemAnimator(new DefaultItemAnimator());
       recyclerView.setNestedScrollingEnabled(false);

       topHeadline = findViewById(R.id.topHeadline);

        onLoadingSwipeRefresh("");

        errorLayout = findViewById(R.id.errorLayout);
        errorImage = findViewById(R.id.errorImage);
        errorTitle = findViewById(R.id.errorTitle);
        errorMessage = findViewById(R.id.errorMessage);
        btnRetry = findViewById(R.id.btnRetry);


       LoadJson("");

    }


    public void LoadJson(final String keyword){

        errorLayout.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(true);


      ApiInterface apiInterface = ApiClient.getApiClient().create(ApiInterface.class);

      String country = Utils.getCountry();

      String language = Utils.getLanguage();
      Call<News> call;

      if (keyword.length() > 0){
          call = apiInterface.getNewsSearch(keyword, language, "publishedAt", API_KEY);
      }
      else {

          call = apiInterface.getNews(country, API_KEY);

      }



      call.enqueue(new Callback<News>() {
          @Override
          public void onResponse(Call<News> call, Response<News> response) {
              if(response.isSuccessful() && response.body().getArticles() != null)
              {
                  if (!articles.isEmpty())
                  {
                      articles.clear();
                  }

                  articles = response.body().getArticles();
                  adapter = new Adapter(articles, MainActivity.this);
                  recyclerView.setAdapter(adapter);
                  adapter.notifyDataSetChanged();

                  initListener();

                  topHeadline.setVisibility(View.VISIBLE);
                  swipeRefreshLayout.setRefreshing(false);



              }
              else {


                  topHeadline.setVisibility(View.INVISIBLE);
                  swipeRefreshLayout.setRefreshing(false);

                  String errorCode;
                  switch (response.code()) {
                      case 404:
                          errorCode = "404 not found";
                          break;
                      case 500:
                          errorCode = "500 server broken";
                          break;
                      default:
                          errorCode = "unknown error";
                          break;
                  }


                 showErrorMessage(R.drawable.no_result,
                         "No Result",
                         "Please try Again!\n"+errorCode);


              }
          }

          @Override
          public void onFailure(Call<News> call, Throwable t) {
             topHeadline.setVisibility(View.INVISIBLE);
             swipeRefreshLayout.setRefreshing(false);
             showErrorMessage(R.drawable.oops,
                     "Opps..",
                     "Network failure, Please Try Again\n"
                             +t.toString());

          }
      });


  }



    private void initListener() {
            adapter.setOnItemClickListener(new Adapter.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {

                    ImageView imageView = view.findViewById(R.id.img);
                    Intent intent = new Intent(MainActivity.this, NewsDetailActivity.class);

                    Article article = articles.get(position);
                    intent.putExtra("url", article.getUrl());
                    intent.putExtra("title", article.getTitle());
                    intent.putExtra("img",  article.getUrlToImage());
                    intent.putExtra("date",  article.getPublishedAt());
                    intent.putExtra("source",  article.getSource().getName());
                    intent.putExtra("author",  article.getAuthor());



                    Pair<View, String> pair = Pair.create(imageView, ViewCompat.getTransitionName(imageView));

                    ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    MainActivity.this,
                                    pair);


                    startActivity(intent, optionsCompat.toBundle());

                }



            });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);



        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setQueryHint("Search Latest News...");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                if (s.length() > 2){
                    LoadJson(s);
                }
                else {
                    Toast.makeText(MainActivity.this, "Type more than two letters!", Toast.LENGTH_SHORT).show();
                }
                return false;

            }

            @Override
            public boolean onQueryTextChange(String s) {
               LoadJson(s);
                return false;
            }
        });

      searchMenuItem.getIcon().setVisible(false, false);
      return true;
    }


    @Override
    public void onRefresh() {
        LoadJson("");

    }


    private void onLoadingSwipeRefresh(String s) {
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                LoadJson(s);
            }
        });

    }



    private void showErrorMessage(int no_result, String no_result1, String s) {

        if (errorLayout.getVisibility() == View.GONE) {
            errorLayout.setVisibility(View.VISIBLE);
        }

        errorImage.setImageResource(no_result);
        errorTitle.setText(no_result1);
        errorMessage.setText(s);

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onLoadingSwipeRefresh("");
            }
        });




    }


}