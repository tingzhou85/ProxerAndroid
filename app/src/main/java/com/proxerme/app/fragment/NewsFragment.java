package com.proxerme.app.fragment;

import android.os.Bundle;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;

import com.proxerme.app.activity.ImageDetailActivity;
import com.proxerme.app.adapter.NewsAdapter;
import com.proxerme.app.manager.NewsManager;
import com.proxerme.app.manager.NotificationManager;
import com.proxerme.app.manager.NotificationRetrievalManager;
import com.proxerme.app.util.MaterialDrawerHelper;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerException;
import com.proxerme.library.connection.ProxerTag;
import com.proxerme.library.connection.UrlHolder;
import com.proxerme.library.entity.News;

import java.util.Arrays;
import java.util.List;

/**
 * A Fragment, retrieving and displaying News.
 *
 * @author Ruben Gees
 */
public class NewsFragment extends PagingFragment<News, NewsAdapter> {

    @NonNull
    public static NewsFragment newInstance() {
        return new NewsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NotificationManager.cancel(getContext(), NotificationManager.NEWS_NOTIFICATION);
    }

    @Override
    protected void configAdapter(@NonNull NewsAdapter adapter) {
        adapter.setOnNewsInteractionListener(new NewsAdapter.OnNewsInteractionListener() {
            @Override
            public void onNewsClick(@NonNull View v, @NonNull News news) {
                getDashboardActivity().showPage(UrlHolder.getNewsPageUrl(news.getCategoryId(),
                        news.getThreadId()));
            }

            @Override
            public void onNewsImageClick(@NonNull View v, @NonNull News news) {
                ImageDetailActivity.navigateTo(getActivity(), (ImageView) v,
                        UrlHolder.getNewsImageUrl(news.getId(), news.getImageId()));
            }

            @Override
            public void onNewsExpanded(@NonNull View v, @NonNull News news) {
                getDashboardActivity().setLikelyUrl(UrlHolder.getNewsPageUrl(news.getCategoryId(),
                        news.getThreadId()));
            }
        });
    }

    @Override
    protected NewsAdapter createAdapter(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return new NewsAdapter();
        } else {
            return new NewsAdapter(savedInstanceState);
        }
    }

    @Override
    protected void load(@IntRange(from = 1) int page, boolean insert,
                        final @NonNull ProxerConnection.ResultCallback<List<News>> callback) {
        ProxerConnection.loadNews(page).execute(new ProxerConnection.ResultCallback<List<News>>() {
            @Override
            public void onResult(List<News> result) {
                callback.onResult(result);

                NewsManager manager = NewsManager.getInstance();

                manager.setNewNews(0);
                manager.setLastId(result.get(0).getId());
                NotificationRetrievalManager.retrieveNewsLater(getContext());

                if (getActivity() != null) {
                    getDashboardActivity().setBadge(MaterialDrawerHelper.DRAWER_ID_NEWS, null);
                }

                List<News> news = Arrays.asList(new News("1", 214241, "anoeivnrvevrvnjripevmerpv", "cwenvwo", "Subject", 12, "wev", "cwe", "mcwep", 21, "d", "cew"),
                        new News("1", 214241, "Description", "cwenvwo", "Subject", 12, "wev", "cwe", "mcwep", 21, "d", "cew"));

                NotificationManager.showNewsNotification(getContext(), news, 2);
            }

            @Override
            public void onError(@NonNull ProxerException exception) {
                callback.onError(exception);
            }
        });
    }

    @Override
    protected void cancelRequest() {
        ProxerConnection.cancel(ProxerTag.NEWS);
    }
}
