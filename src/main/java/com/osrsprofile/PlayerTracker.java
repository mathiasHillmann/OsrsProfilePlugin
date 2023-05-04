package com.osrsprofile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;

import javax.inject.Inject;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class PlayerTracker {
    private final String PLAYER_MODEL_URL = "http://localhost/public/model";
    private final String COLLECT_URL = "http://localhost/public/collect";

    private Map<String, Map<String, Integer>> model =  new HashMap<String, Map<String, Integer>>();

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(60))
            .build();

    public PlayerTracker() {
        this.fetchPlayerModel();
    }

    public void fetchPlayerModel()
    {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(PLAYER_MODEL_URL))
                    .setHeader("User-Agent", "RuneLite")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                Gson gson = new GsonBuilder().create();
                this.model = (Map<String, Map<String, Integer>>) gson.fromJson(response.body(), HashMap.class);
            } else {
                log.error("Could not fetch model from api, api returned code: "+response.statusCode());
            }
        } catch (Exception e) {
            log.error("Could not fetch model from api", e);
        }
    }

    public void submitToApi(Client client)
    {
        this.updatePlayerModel(client);

        try {
            Gson gson = new GsonBuilder().serializeNulls().create();
            String json = gson.toJson(this.model);

            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .uri(URI.create(COLLECT_URL))
                    .setHeader("User-Agent", "RuneLite")
                    .setHeader("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.error("Could not submit player data to api", e);
        }
    }

    private void updatePlayerModel(Client client)
    {
        for (Map.Entry<String, Map<String, Integer>> type : this.model.entrySet()) {
            for (Map.Entry<String, Integer> entry : type.getValue().entrySet()) {
                Integer value = null;
                switch(type.getKey()) {
                    case "skills":
                        value = client.getSkillExperience(Skill.valueOf(entry.getKey()));
                        break;
                }

                this.model.get(type.getKey()).put(entry.getKey(), value);
            }
        }
    }
}
