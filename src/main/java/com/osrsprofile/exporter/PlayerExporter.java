package com.osrsprofile.exporter;

import com.osrsprofile.api.Api;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.kit.KitType;
import net.runelite.client.RuneLite;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.apache.commons.lang3.ArrayUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

@Slf4j
public class PlayerExporter {
    @Inject
    private Api api;

    @Inject
    private Client client;

    @Inject
    private PlayerModelExporter playerModelExporter;

    @Inject
    @Named("developerMode")
    boolean developerMode;

    /*
     6570 - Fire Cape
     21285 - Infernal Max Cape
     21295 - Infernal Cape
     */
    private final int[] capeBlacklist = {6570, 21285, 21295};

    /*
    12788 - Magic shortbow (i)
     */
    private final int[] weaponBlacklist = {12788};

    public void export() {
        try {
            if (this.hasBlacklistedEquipment(KitType.CAPE, this.capeBlacklist)) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "<col=ff0000>Animated capes are not allowed. Please unequip or switch for another cape", null);
                return;
            }

            if (this.hasBlacklistedEquipment(KitType.WEAPON, this.weaponBlacklist)) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "<col=ff0000>Animated weapons are not allowed. Please unequip or switch for another weapon", null);
                return;
            }

            ByteArrayOutputStream stream = this.playerModelExporter.export();
            String fileName = client.getAccountHash()+".ply";

            if (developerMode) {
                File file = new File(RuneLite.RUNELITE_DIR, fileName);
                if (file.isFile()) {
                    boolean deleted = file.delete();
                    if (!deleted) {
                        log.debug("Could not delete model file in runelite dir");
                    }
                }

                FileOutputStream fos = new FileOutputStream(file);
                stream.writeTo(fos);
            }

            RequestBody formBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("model", fileName,
                            RequestBody.create(MediaType.parse("application/ply"), stream.toByteArray()))
                    .build();

            this.api.post(client.getAccountHash() + "/model", formBody);
        } catch (Exception e) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "<col=ff0000>Could not export your player model, please try again. It might have been an animation or item equipped.", null);
            log.error("Could not export player model", e);
        }
    }

    private boolean hasBlacklistedEquipment(KitType equipmentType, int[] blacklist) {
        int currentCape = this.client.getLocalPlayer().getPlayerComposition().getEquipmentId(equipmentType);
        return ArrayUtils.contains(blacklist, currentCape);
    }
}
