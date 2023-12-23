package com.osrsprofile.tracker;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.osrsprofile.OsrsProfileConfig;
import com.osrsprofile.api.Api;
import com.osrsprofile.tracker.dto.TrackingObject;
import com.osrsprofile.tracker.dto.TrackingRequest;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneScapeProfileType;
import okhttp3.Response;

import javax.inject.Inject;

@Slf4j
public class PlayerTracker {

    private Map<String, TrackingObject> playerData = new HashMap<>();

    @Inject
    private Client client;

    @Inject
    private Gson gson;

    @Inject
    private ConfigManager configManager;

    @Inject
    private Api api;

    public void fetchVars(OsrsProfileConfig config)
    {
        try {
            Response response = this.api.get("vars?quests="+config.trackQuests()
                    +"&skills="+config.trackSkills()
                    +"&minigames="+config.trackMinigames()
                    +"&diaries="+config.trackDiaries()
                    +"&combat="+config.trackCombat()
                    +"&bosskills="+config.trackBossKills()
                    +"&slayermonsters="+config.trackSlayerMonstersKills()
                    +"&collectionlog="+config.trackCollectionLog()
                );

            if (response.code() == 200) {
                Type type = new TypeToken<Map<String, TrackingObject>>() {}.getType();
                String responseString = response.body().string();

                this.playerData = gson.fromJson(responseString, type);
            } else {
                log.error("Could not fetch vars from api, api returned: ("+response.code()+") - "+response.body().string());
            }
        } catch (Exception e) {
            log.error("Could not fetch vars from api", e);
        }
    }

    public void submitToApi()
    {
        if (this.shouldNotMakeRequests()) {
            return;
        }

        this.updatePlayerModel();

        try {
            TrackingRequest requestObj = new TrackingRequest();
            requestObj.data = this.playerData;
            requestObj.username = client.getLocalPlayer().getName();
            requestObj.accountType = this.getAccountType();

            Gson gson = this.gson.newBuilder().serializeNulls().create();
            String json = gson.toJson(requestObj);

            this.api.post(String.valueOf(client.getAccountHash()), json);

        } catch (Exception e) {
            log.error("Could not submit player data to api", e);
        }
    }

    private void updatePlayerModel()
    {
        this.playerData.forEach((key, item) -> item.value = this.getValue(item.index, item.type));
    }

    private Integer getValue(String index, String type) {
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
            case "killcount":
                value = configManager.getRSProfileConfiguration("killcount", index, int.class);
                break;
            case "personalbest":
                Double pb = configManager.getRSProfileConfiguration("personalbest", index, double.class);
                value = pb != null ? Math.toIntExact(Math.round(pb)) : null;
                break;
            default:
                value = null;
                break;
        }

        return value;
    }

    private String getAccountType() {
        return AccountType.getType(client.getVarbitValue(Varbits.ACCOUNT_TYPE)).toString();
    }

    private boolean shouldNotMakeRequests() {
        return RuneScapeProfileType.getCurrent(client) != RuneScapeProfileType.STANDARD
            || client.getGameState() == GameState.LOGIN_SCREEN
            || client.getGameState() == GameState.LOGIN_SCREEN_AUTHENTICATOR;
    }
}
