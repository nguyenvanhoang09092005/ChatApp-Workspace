package org.example.chatappclient.client.utils.helpers;

import java.util.HashMap;
import java.util.Map;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class SoundUtil {
    private static final Map<String, MediaPlayer> soundPlayers = new HashMap<>();
    private static boolean soundEnabled = true;

    /**
     * Play sound file
     */
    public static void playSound(String soundFileName) {
        if (!soundEnabled) return;

        try {
            String soundPath = "/resources/sounds/" + soundFileName;
            java.net.URL resource = SoundUtil.class.getResource(soundPath);

            if (resource == null) {
                System.err.println("Sound file not found: " + soundPath);
                return;
            }

            // Reuse or create MediaPlayer
            MediaPlayer player = soundPlayers.get(soundFileName);
            if (player == null) {
                Media sound = new Media(resource.toString());
                player = new MediaPlayer(sound);
                soundPlayers.put(soundFileName, player);
            }

            player.stop(); // Stop if already playing
            player.play();
        } catch (Exception e) {
            System.err.println("Error playing sound: " + e.getMessage());
        }
    }

    /**
     * Play notification sound
     */
    public static void playNotification() {
        playSound("notification.wav");
    }

    /**
     * Play message sent sound
     */
    public static void playMessageSent() {
        playSound("message-sent.wav");
    }

    /**
     * Play message received sound
     */
    public static void playMessageReceived() {
        playSound("message-received.wav");
    }

    /**
     * Play call ring sound
     */
    public static void playCallRing() {
        playSound("call-ring.wav");
    }

    /**
     * Enable/disable sounds
     */
    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    /**
     * Check if sounds are enabled
     */
    public static boolean isSoundEnabled() {
        return soundEnabled;
    }

    /**
     * Stop all sounds
     */
    public static void stopAllSounds() {
        for (MediaPlayer player : soundPlayers.values()) {
            player.stop();
        }
    }

    /**
     * Release all resources
     */
    public static void dispose() {
        for (MediaPlayer player : soundPlayers.values()) {
            player.dispose();
        }
        soundPlayers.clear();
    }
}
