package mil.nga.giat.mage.sdk.http.resource;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import mil.nga.giat.mage.sdk.R;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.http.HttpClientManager;
import mil.nga.giat.mage.sdk.http.converter.UserConverterFactory;
import mil.nga.giat.mage.sdk.http.converter.UsersConverterFactory;
import mil.nga.giat.mage.sdk.utils.MediaUtility;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.PartMap;
import retrofit2.http.Path;

/***
 * RESTful communication for users
 *
 * @author newmanw
 */

public class UserResource {

    public interface UserService {
        @POST("/auth/local/signin")
        Call<JsonObject> signin(@Body JsonObject body);

        @POST("/auth/{strategy}/authorize")
        Call<JsonObject> authorize(@Path("strategy") String strategy, @Body JsonObject body);

        @POST("/api/logout")
        Call<ResponseBody> logout(@Header("Authorization") String authorization);

        @GET("/api/users")
        Call<Collection<User>> getUsers();

        @POST("/api/users")
        Call<JsonObject> createUser(@Body JsonObject body);

        @GET("/api/users/{userId}")
        Call<User> getUser(@Path("userId") String userId);

        @GET("/api/users/{userId}/icon")
        Call<ResponseBody> getIcon(@Path("userId") String userId);

        @GET("/api/users/{userId}/avatar")
        Call<ResponseBody> getAvatar(@Path("userId") String userId);

        @POST("/api/users/{userId}/events/{eventId}/recent")
        Call<User> addRecentEvent(@Path("userId") String userId, @Path("eventId") String eventId);

        @Multipart
        @PUT("/api/users/myself")
        Call<User> createAvatar(@PartMap Map<String, RequestBody> parts);

        @PUT("/api/users/myself/password")
        Call<JsonObject> changePassword(@Body JsonObject body);
    }

    private static final String LOG_NAME = UserResource.class.getName();

    private Context context;

    public UserResource(Context context) {
        this.context = context;
    }

    public Response<JsonObject> signin(String username, String uid, String password) {
        Response<JsonObject> response = null;

        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));

        try {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(HttpClientManager.getInstance().httpClient())
                    .build();

            UserService service = retrofit.create(UserService.class);

            JsonObject json = new JsonObject();
            json.addProperty("username", username);
            json.addProperty("uid", uid);
            json.addProperty("password", password);

            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                json.addProperty("appVersion", String.format("%s-%s", packageInfo.versionName, packageInfo.versionCode));
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(LOG_NAME , "Problem retrieving package info.", e);
            }

            response = service.signin(json).execute();
        } catch (Exception e) {
            Log.e(LOG_NAME, "Bad request.", e);
        }

        return response;
    }

    public JsonObject authorize(String strategy, String uid) {
        JsonObject body = null;

        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));

        try {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(HttpClientManager.getInstance().httpClient())
                    .build();

            UserService service = retrofit.create(UserService.class);

            JsonObject json = new JsonObject();
            json.addProperty("uid", uid);

            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                json.addProperty("appVersion", String.format("%s-%s", packageInfo.versionName, packageInfo.versionCode));
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(LOG_NAME , "Problem retrieving package info.", e);
            }

            Response<JsonObject> response = service.authorize(strategy, json).execute();
            if (response.isSuccessful()) {
                body = response.body();
            } else {
                Log.e(LOG_NAME, "Bad request.");
                if (response.errorBody() != null) {
                    Log.e(LOG_NAME, response.errorBody().string());
                }
            }
        } catch (Exception e) {
            Log.e(LOG_NAME, "Bad request.", e);
        }

        return body;
    }

    public void logout(Callback<ResponseBody> callback) {
        try {
            String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(HttpClientManager.getInstance().httpClient())
                    .build();

            UserService service = retrofit.create(UserService.class);

            String token = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.tokenKey), null);
            service.logout(String.format("Bearer %s", token)).enqueue(callback);
        } catch (Exception e) {
            Log.e(LOG_NAME, "Bad request.", e);
        }
    }

    public Collection<User> getUsers() throws IOException {
        Collection<User> users = new ArrayList<>();

        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(UsersConverterFactory.create(context))
                .client(HttpClientManager.getInstance().httpClient())
                .build();

        UserService service = retrofit.create(UserService.class);
        Response<Collection<User>> response = service.getUsers().execute();

        if (response.isSuccessful()) {
            users = response.body();
        } else {
            Log.e(LOG_NAME, "Bad request.");
            if (response.errorBody() != null) {
                Log.e(LOG_NAME, response.errorBody().string());
            }
        }

        return users;
    }

    public JsonObject createUser(String username, String displayname, String email, String phone, String uid, String password) throws Exception {
        JsonObject user = null;

        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(HttpClientManager.getInstance().httpClient())
                .build();

        JsonObject json = new JsonObject();
        json.addProperty("username", username);
        json.addProperty("displayName", displayname);
        json.addProperty("email", email);
        json.addProperty("phone", phone);
        json.addProperty("uid", uid);
        json.addProperty("password", password);
        json.addProperty("passwordconfirm", password);

        UserService service = retrofit.create(UserService.class);
        Response<JsonObject> response = service.createUser(json).execute();

        if (response.isSuccessful()) {
            user = response.body();
        } else {
            String errorMessage = "Unable to create user account, please contact MAGE administrator.";
            if (response.errorBody() != null) {
                errorMessage = response.errorBody().string();
            }

            Log.e(LOG_NAME, errorMessage);
            throw new RuntimeException(errorMessage);
        }

        return user;
    }

    public User getUser(String userId) throws IOException {
        User user = null;

        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(UserConverterFactory.create(context))
                .client(HttpClientManager.getInstance().httpClient())
                .build();

        UserService service = retrofit.create(UserService.class);
        Response<User> response = service.getUser(userId).execute();

        if (response.isSuccessful()) {
            user = response.body();
        } else {
            Log.e(LOG_NAME, "Bad request.");
            if (response.errorBody() != null) {
                Log.e(LOG_NAME, response.errorBody().string());
            }
        }

        return user;
    }

    public InputStream getIcon(User user) throws IOException {
        InputStream inputStream = null;

        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(HttpClientManager.getInstance().httpClient())
                .build();

        UserService service = retrofit.create(UserService.class);
        Response<ResponseBody> response = service.getIcon(user.getRemoteId()).execute();

        if (response.isSuccessful()) {
            inputStream = response.body().byteStream();
        } else {
            Log.e(LOG_NAME, "Bad request.");
            if (response.errorBody() != null) {
                Log.e(LOG_NAME, response.errorBody().string());
            }
        }

        return inputStream;
    }

    public InputStream getAvatar(User user) throws IOException {
        InputStream inputStream = null;

        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(HttpClientManager.getInstance().httpClient())
                .build();

        UserService service = retrofit.create(UserService.class);
        Response<ResponseBody> response = service.getAvatar(user.getRemoteId()).execute();

        if (response.isSuccessful()) {
            inputStream = response.body().byteStream();
        } else {
            Log.e(LOG_NAME, "Bad request.");
            if (response.errorBody() != null) {
                Log.e(LOG_NAME, response.errorBody().string());
            }
        }

        return inputStream;
    }

    public User addRecentEvent(User user, Event event) throws IOException {
        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(UserConverterFactory.create(context))
                .client(HttpClientManager.getInstance().httpClient())
                .build();

        UserService service = retrofit.create(UserService.class);
        Response<User> response = service.addRecentEvent(user.getRemoteId(), event.getRemoteId()).execute();

        if (response.isSuccessful()) {
            return response.body();
        } else {
            Log.e(LOG_NAME, "Bad request.");
            if (response.errorBody() != null) {
                Log.e(LOG_NAME, response.errorBody().string());
            }

            return null;
        }
    }

    public User createAvatar(String avatarPath) {
        try {
            String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(UserConverterFactory.create(context))
                    .client(HttpClientManager.getInstance().httpClient())
                    .build();

            UserService service = retrofit.create(UserService.class);

            Map<String, RequestBody> parts = new HashMap<>();
            File avatar = new File(avatarPath);
            String mimeType = MediaUtility.getMimeType(avatarPath);
            RequestBody fileBody = RequestBody.create(MediaType.parse(mimeType), avatar);
            parts.put("avatar\"; filename=\"" + avatar.getName() + "\"", fileBody);

            Response<User> response = service.createAvatar(parts).execute();

            if (response.isSuccessful()) {
                User user = response.body();

                UserHelper userHelper = UserHelper.getInstance(context);
                User currentUser = userHelper.readCurrentUser();
                currentUser.setAvatarUrl(user.getAvatarUrl());
                currentUser.setLastModified(new Date(currentUser.getLastModified().getTime() + 1));
                UserHelper.getInstance(context).update(currentUser);

                userHelper.setAvatarPath(currentUser, null);

                Log.d(LOG_NAME, "Updated user with remote_id " + user.getRemoteId());

                return user;
            } else {
                Log.e(LOG_NAME, "Bad request.");
                if (response.errorBody() != null) {
                    Log.e(LOG_NAME, response.errorBody().string());
                }
            }
        } catch (Exception e) {
            Log.e(LOG_NAME, "Failure saving observation.", e);
        }

        return null;
    }

    public void changePassword(String username, String password, String newPassword, String newPasswordConfirm, Callback<JsonObject> callback) {
        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.serverURLKey), context.getString(R.string.serverURLDefaultValue));

        OkHttpClient httpClient = HttpClientManager.getInstance().httpClient().newBuilder().build();
        httpClient.interceptors().clear();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build();

        UserService service = retrofit.create(UserService.class);

        JsonObject json = new JsonObject();
        json.addProperty("username", username);
        json.addProperty("password", password);
        json.addProperty("newPassword", newPassword);
        json.addProperty("newPasswordConfirm", newPasswordConfirm);

        service.changePassword(json).enqueue(callback);
    }
}