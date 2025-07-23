package atm.licenta.cy.Models;

public class VoiceMessageModel extends ChatMessageModel{
    private final String audioPath;
    private final int durationSeconds;

    public VoiceMessageModel(String sender, String timestamp, boolean isSentByMe, String audioPath, int durationSeconds) {
        super(sender, null, timestamp, isSentByMe, false);
        this.audioPath = audioPath;
        this.durationSeconds = durationSeconds;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }
}
