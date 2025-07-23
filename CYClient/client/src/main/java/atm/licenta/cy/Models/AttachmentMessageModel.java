package atm.licenta.cy.Models;

public class AttachmentMessageModel extends ChatMessageModel{
    private final String fileExtension;
    private final String fileType;
    private final String fileName;
    private final String fileSize;
    private final String filePath;
    public AttachmentMessageModel(String sender, String timestamp, boolean isSentByMe,
                                  String filePath, String fileExtension, String fileType,
                                  String fileName, String fileSize) {
        super(sender, null, timestamp, isSentByMe, false);
        this.filePath = filePath;
        this.fileExtension = fileExtension;
        this.fileType = fileType;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }
    public String getFileExtension() {
        return fileExtension;
    }
    public String getFileType() {
        return fileType;
    }
    public String getFileName() {
        return fileName;
    }
    public String getFileSize() {
        return fileSize;
    }

    public String getFilePath() {
        return filePath;
    }
}
