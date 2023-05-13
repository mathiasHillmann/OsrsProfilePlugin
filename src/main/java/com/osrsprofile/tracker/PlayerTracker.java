package com.osrsprofile.tracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.osrsprofile.OsrsProfileConfig;
import com.osrsprofile.tracker.dto.TrackingObject;
import com.osrsprofile.tracker.dto.TrackingRequest;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;

import java.lang.reflect.Type;
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
    private final String API_URL = "http://osrsprofilebackend.test/public/player";

    private Map<String, TrackingObject> playerData = new HashMap<>();

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(60))
            .build();

    public String accountHash = null;

    public void fetchPlayerData(OsrsProfileConfig config)
    {
        if (this.accountHash == null) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(API_URL+'/'+this.accountHash
                        +"?quests="+config.trackQuests()
                        +"&skills="+config.trackSkills()
                        +"&minigames="+config.trackMinigames()
                        +"&diaries="+config.trackDiaries()
                        +"&combat="+config.trackCombat()
                    ))
                    .setHeader("User-Agent", "RuneLite")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                Gson gson = new GsonBuilder().create();
                Type type = new TypeToken<Map<String, TrackingObject>>() {}.getType();
                this.playerData = gson.fromJson(response.body(), type);

            } else {
                log.error("Could not fetch player data from api, api returned: ("+response.statusCode()+") - "+response.body());
            }
        } catch (Exception e) {
            log.error("Could not fetch model from api", e);
        }
    }

    public void submitToApi(Client client)
    {
        if (this.accountHash == null) {
            return;
        }
        this.updatePlayerModel(client);

        try {
            TrackingRequest requestObj = new TrackingRequest();
            requestObj.data = this.playerData;
            requestObj.username = client.getLocalPlayer().getName();
            requestObj.accountType = client.getAccountType().toString();

            Gson gson = new GsonBuilder().serializeNulls().create();
            String json = gson.toJson(requestObj);

            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .uri(URI.create(API_URL+'/'+this.accountHash))
                    .setHeader("User-Agent", "RuneLite")
                    .setHeader("Content-Type", "application/json")
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.error("Could not submit player data to api", e);
        }
    }

    private void updatePlayerModel(Client client)
    {
        this.playerData.forEach((key, item) -> item.value = this.getValue(item.index, item.type, client));
    }

    private Integer getValue(String index, String type, Client client) {
        Integer value = null;

        switch(type) {
            case "skill":
                value = client.getSkillExperience(Skill.valueOf(index));
                break;
            case "varb":
                value = client.getVarbitValue(Integer.parseInt(index));
                break;
            case "varp":
                value = client.getVarpValue(Integer.parseInt(index));
                break;
        }

        return value;
    }
}
