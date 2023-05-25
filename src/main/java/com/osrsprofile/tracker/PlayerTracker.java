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
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.inject.Inject;

@Slf4j
public class PlayerTracker {
    private final String API_URL = "https://api.osrsprofile.com/runelite/player";

    private Map<String, TrackingObject> playerData = new HashMap<>();

    @Inject
    private OkHttpClient httpClient;

    @Inject
    private Client client;

    @Inject
    private Gson gson;

    public String accountHash = null;

    public void fetchPlayerData(OsrsProfileConfig config)
    {
        if (this.accountHash == null) {
            return;
        }

        try {
            Request request = new Request.Builder()
                    .url(API_URL+'/'+this.accountHash
                        +"?quests="+config.trackQuests()
                        +"&skills="+config.trackSkills()
                        +"&minigames="+config.trackMinigames()
                        +"&diaries="+config.trackDiaries()
                        +"&combat="+config.trackCombat()
                        +"&bosskills="+config.trackBossKills()
                        +"&slayermonsters="+config.trackSlayerMonstersKills()
                    )
                    .addHeader("User-Agent", "RuneLite")
                    .build();

            Response response = httpClient.newCall(request).execute();

            if (response.code() == 200) {
                Type type = new TypeToken<Map<String, TrackingObject>>() {}.getType();
                String responseString = response.body().string();
                log.debug(responseString);

                this.playerData = gson.fromJson(responseString, type);
            } else {
                log.error("Could not fetch player data from api, api returned: ("+response.code()+") - "+response.body().string());
            }
        } catch (Exception e) {
            log.error("Could not fetch model from api", e);
        }
    }

    public void submitToApi()
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

            Gson gson = this.gson.newBuilder().serializeNulls().create();
            String json = gson.toJson(requestObj);

            log.debug(API_URL+'/'+this.accountHash+": "+json);

            Request request = new Request.Builder()
                    .url(API_URL+'/'+this.accountHash)
                    .post(RequestBody.create(MediaType.parse("application/json"), json))
                    .addHeader("User-Agent", "RuneLite")
                    .addHeader("Content-Type", "application/json")
                    .build();

            Response response = httpClient.newCall(request).execute();
            log.debug(response.body().string());
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
