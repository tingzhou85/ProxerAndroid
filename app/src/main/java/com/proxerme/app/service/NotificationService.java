package com.proxerme.app.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;

import com.proxerme.app.application.MainApplication;
import com.proxerme.app.manager.NotificationRetrievalManager;
import com.proxerme.app.util.helper.NotificationHelper;
import com.proxerme.app.util.helper.PagingHelper;
import com.proxerme.app.util.helper.StorageHelper;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.connection.ProxerException;
import com.proxerme.library.entity.Conference;
import com.proxerme.library.entity.LoginUser;
import com.proxerme.library.entity.News;
import com.proxerme.library.util.ProxerInfo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * An IntentService, which retrieves the News and shows a notification if there are unread
 * ones.
 *
 * @author Ruben Gees
 */
public class NotificationService extends IntentService {

    public static final String ACTION_LOAD_NEWS =
            "com.proxerme.app.service.action.LOAD_NEWS";
    public static final String ACTION_LOAD_MESSAGES =
            "com.proxerme.app.service.action.LOAD_MESSAGES";
    private static final String SERVICE_TITLE = "Notification Service";

    public NotificationService() {
        super(SERVICE_TITLE);
    }

    public static void load(@NonNull Context context, @NonNull @NotificationAction String action) {
        Intent intent = new Intent(context, NotificationService.class);

        intent.setAction(action);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null && ((MainApplication) getApplication()).getStartedActivities() <= 0) {
            final String action = intent.getAction();

            if (ACTION_LOAD_NEWS.equals(action)) {
                handleActionLoadNews();
            } else if (ACTION_LOAD_MESSAGES.equals(action)) {
                handleActionLoadMessages();
            }
        }
    }

    private void handleActionLoadNews() {
        String lastId = StorageHelper.getLastNewsId();

        try {
            if (lastId != null) {
                List<News> news = ProxerConnection.loadNews(1).executeSynchronized();
                int offset = PagingHelper.calculateOffsetFromStart(news, lastId,
                        ProxerInfo.NEWS_ON_PAGE);

                StorageHelper.setNewNews(offset);
                NotificationHelper.showNewsNotification(this, news, offset);
            }
        } catch (ProxerException ignored) {

        }
    }

    private void handleActionLoadMessages() {
        LoginUser user = StorageHelper.getUser();

        if (user != null) {
            try {
                ProxerConnection.login(user).executeSynchronized();
                List<Conference> conferences = ProxerConnection.loadConferences(1)
                        .executeSynchronized();

                for (int i = 0; i < conferences.size(); i++) {
                    if (conferences.get(i).isRead()) {
                        conferences = conferences.subList(0, i);

                        break;
                    }
                }
                NotificationHelper.showMessagesNotification(this, conferences);
                StorageHelper.setNewMessages(conferences.size());
            } catch (ProxerException ignored) {

            }

            StorageHelper.incrementMessagesInterval();
            NotificationRetrievalManager.retrieveMessagesLater(this);
        } else {
            NotificationRetrievalManager.cancelMessagesRetrieval(this);
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({ACTION_LOAD_NEWS, ACTION_LOAD_MESSAGES})
    public @interface NotificationAction {
    }

}
