package anuj.wifidirect.beans;

import java.io.Serializable;

public class WiFiTransferModal implements Serializable {
    private String MessageContent;
    private String FileName;
    private Long FileLength;
    private String InetAddress;
    private String extension;


    public WiFiTransferModal() {

    }

    public WiFiTransferModal(String inetaddress) {
        this.InetAddress = inetaddress;
    }

    public WiFiTransferModal(String name, Long filelength) {
        this.FileName = name;
        this.FileLength = filelength;
    }

    public WiFiTransferModal(String message,Long filelength,String m) {
        this.MessageContent = message;
        this.FileLength = filelength;
    }
    public String getInetAddress() {
        return InetAddress;
    }

    public void setInetAddress(String inetAddress) {
        InetAddress = inetAddress;
    }


    public Long getFileLength() {
        return FileLength;
    }

    public void setFileLength(Long fileLength) {
        FileLength = fileLength;
    }

    public String getFileName() {
        return FileName;
    }
    public String getMessageContent() {
        return MessageContent;
    }
    public void setMessageContent(String messageC) {
        MessageContent = messageC;
    }

    public void setFileName(String fileName) {
        FileName = fileName;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }


}
