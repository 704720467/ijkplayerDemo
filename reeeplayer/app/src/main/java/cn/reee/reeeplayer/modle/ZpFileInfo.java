package cn.reee.reeeplayer.modle;

/**
 * Create by zp on 2019-12-10
 */
public class ZpFileInfo {
    public static final int FILE_TYPE_VIDEO = 0;
    public static final int FILE_TYPE_PICTURE = 1;

    protected int fileId;
    protected String filePath;
    protected String fileName;
    protected String thumbPath;
    protected boolean isSelected = false;
    protected long duration;
    protected int fileType = FILE_TYPE_VIDEO;

    public ZpFileInfo() {
    }

    public ZpFileInfo(int fileId, String filePath, String fileName, String thumbPath, int duration) {
        this.fileId = fileId;
        this.filePath = filePath;
        this.fileName = fileName;
        this.thumbPath = thumbPath;
        this.duration = duration;
    }


    @Override
    public boolean equals(Object obj) {
        ZpFileInfo temp = null;
        if (obj != null) {
            temp = (ZpFileInfo) obj;

            if (temp.fileName.equals(this.fileName) && temp.filePath.equals(this.filePath) && temp.fileId == this.fileId)
                return true;
            return false;
        }
        return super.equals(obj);
    }

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getThumbPath() {
        return thumbPath;
    }

    public void setThumbPath(String thumbPath) {
        this.thumbPath = thumbPath;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getFileType() {
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }

    @Override
    public String toString() {
        return "KLFileInfo{" +
                "fileId=" + fileId +
                ", filePath='" + filePath + '\'' +
                ", fileName='" + fileName + '\'' +
                ", thumbPath='" + thumbPath + '\'' +
                ", isSelected=" + isSelected +
                ", duration=" + duration +
                ", fileType=" + fileType +
                '}';
    }
}
