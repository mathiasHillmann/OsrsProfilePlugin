package com.osrsprofile.api;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;

@Slf4j
public class Api {
    private final String API_URL = "http://osrsprofilebackend.test/runelite/player";
    private final String DEV_URL = "http://osrsprofilebackend.test/runelite/player";

    @Inject
    private OkHttpClient httpClient;

    @Inject
    @Named("developerMode")
    boolean developerMode;

    public Response get(String route) throws IOException {
        Request request = this.getDefaultRequestBuilder(route).build();
        Response response = httpClient.newCall(request).execute();

        log.debug("RECEIVED " + request.url() + ": " + response.peekBody(Long.MAX_VALUE).string());

        return response;
    }

    public Response post(String route, String json) throws IOException {
        Request request = this.getDefaultRequestBuilder(route)
                .post(RequestBody.create(MediaType.parse("application/json"), json))
                .addHeader("Content-Type", "application/json")
                .build();

        log.debug("SENT " + request.url() + ": " + json);

        Response response = httpClient.newCall(request).execute();

        log.debug("RECEIVED " + request.url() + ": " + response.peekBody(Long.MAX_VALUE).string());

        return response;
    }

    public Response post(String route, RequestBody formBody) throws IOException {
        Request request = this.getDefaultRequestBuilder(route)
                .post(formBody)
                .addHeader("Content-Type", "multipart/form-data")
                .build();

        log.debug("SENT " + request.url() + ": " + formBody);

        Response response = httpClient.newCall(request).execute();

        log.debug("RECEIVED " + request.url() + ": " + response.peekBody(Long.MAX_VALUE).string());

        return response;
    }

    private Request.Builder getDefaultRequestBuilder(String route) {
        return new Request.Builder()
                .url((developerMode ? DEV_URL : API_URL) +'/'+route)
                .addHeader("User-Agent", "RuneLite");
    }
}
