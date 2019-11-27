package com.zp.libvideoedit.modle;

import com.zp.libvideoedit.utils.StrUtils;

import java.io.Serializable;
import java.util.List;

/**
 * Create by zp on 2019-11-26
 */
public class ModulesBean implements Serializable {
    /**
     * id : q5J
     * index : 0
     * font : []
     * version : 0
     * endChunkId : DCE2E1FD-4056-41DC-A8EA-74D5D71C2F3C
     * url : http://image.vnision.com/86825011b3c25e3457e2e203afd057de.zip
     * localResource : false
     * startChunkOffset : 0
     * type : 2
     * flag : 4045557259
     * textModel : {"currentRect":"{{0, 0}, {315, 311}}","rotation":0,"center":"{187.5, 333.5}","images":[{"center":"{190.59202072176606, 54.828125000000007}","name":"bg;","bounds":"{{0, 0}, {32.819261569318229, 109.65625000000001}}"}],"bounds":"{{0, 0}, {315, 311}}","flag":3780213778,"texts":[{"flipVertical":false,"rotation":0,"lineHeight":36,"pointSize":25.012500000000003,"verticalAlignment":1,"fixedPointSize":25.012500000000003,"fixedLineHeight":36,"type":"type=text;up=1;lang=en;line=8;","alignment":2,"defaultText":"I HOPE YOU LIKE\nTHE STARS I STOLE\u2028FOR YOU","strike":0,"kern":0,"flipHorizontal":false,"fontName":"PingFangSC-Semibold","fixedKern":0,"bounds":"{{0, 0}, {315, 311}}","center":"{157.5, 155.5}"}],"viewCenter":"{184.10729431721973, 569.47116200169717}","path":"/2/86825011b3c25e3457e2e203afd057de","scale":1}
     * custom : true
     * cover : 9a3c4c974cf785e6ba42e9f630a0c7de.png
     * endChunkOffset : 0.8079353529163954
     * startChunkId : DCE2E1FD-4056-41DC-A8EA-74D5D71C2F3C
     * prepareProgress : 1
     * timeRange : {{0, 100000}, {189864, 100000}}
     * name : 36
     * contentTimeRange : {{0, 0}, {0, 0}}
     * path : /Documents/audio/DF7314E1-FE27-4355-BBA0-7BB1D0248BDF.mp3
     */

    private String id;
    private int index;
    private String version;
    private String endChunkId;
    private String url;
    private boolean localResource;
    private double startChunkOffset;
    private int type;
    private long flag;
    private TextModelBean textModel;
    private boolean custom;
    private String cover;
    private double endChunkOffset;
    private String startChunkId;
    private int prepareProgress;
    private String timeRange;
    private String name;
    private String contentTimeRange;
    private String path;
    private List<FontData> font;

    //贴纸用
    private float scale;
    private float rotation;
    private String viewCenter;
    private String viewSize;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getEndChunkId() {
        return endChunkId;
    }

    public void setEndChunkId(String endChunkId) {
        this.endChunkId = endChunkId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isLocalResource() {
        return localResource;
    }

    public void setLocalResource(boolean localResource) {
        this.localResource = localResource;
    }

    public double getStartChunkOffset() {
        return startChunkOffset;
    }

    public void setStartChunkOffset(double startChunkOffset) {
        this.startChunkOffset = startChunkOffset;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getFlag() {
        return flag;
    }

    public void setFlag(long flag) {
        this.flag = flag;
    }

    public TextModelBean getTextModel() {
        return textModel;
    }

    public void setTextModel(TextModelBean textModel) {
        this.textModel = textModel;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public double getEndChunkOffset() {
        return endChunkOffset;
    }

    public void setEndChunkOffset(double endChunkOffset) {
        this.endChunkOffset = endChunkOffset;
    }

    public String getStartChunkId() {
        return startChunkId;
    }

    public void setStartChunkId(String startChunkId) {
        this.startChunkId = startChunkId;
    }

    public int getPrepareProgress() {
        return prepareProgress;
    }

    public void setPrepareProgress(int prepareProgress) {
        this.prepareProgress = prepareProgress;
    }

    public List<Long> getTimeRange() {
        return StrUtils.stringToLongs(timeRange);
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Long> getContentTimeRange() {
        return StrUtils.stringToLongs(contentTimeRange);
    }

    public String getContentTimeRangeString() {
        return contentTimeRange;
    }

    public void setContentTimeRange(String contentTimeRange) {
        this.contentTimeRange = contentTimeRange;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<FontData> getFont() {
        return font;
    }

    public void setFont(List<FontData> font) {
        this.font = font;
    }

    public String getFileName() {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    public String getPathFileName() {
        return path == null ? "" : path.substring(path.lastIndexOf("/") + 1);
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public String getViewCenter() {
        return viewCenter;
    }

    public void setViewCenter(String viewCenter) {
        this.viewCenter = viewCenter;
    }

    public String getViewSize() {
        return viewSize;
    }

    public void setViewSize(String viewSize) {
        this.viewSize = viewSize;
    }

    public static class TextModelBean implements Serializable {
        private static final long serialVersionUID = -6195308500744363985L;
        /**
         * currentRect : {{0, 0}, {315, 311}}
         * rotation : 0
         * center : {187.5, 333.5}
         * images : [{"center":"{190.59202072176606, 54.828125000000007}","name":"bg;","bounds":"{{0, 0}, {32.819261569318229, 109.65625000000001}}"}]
         * bounds : {{0, 0}, {315, 311}}
         * flag : 3780213778
         * texts : [{"flipVertical":false,"rotation":0,"lineHeight":36,"pointSize":25.012500000000003,"verticalAlignment":1,"fixedPointSize":25.012500000000003,"fixedLineHeight":36,"type":"type=text;up=1;lang=en;line=8;","alignment":2,"defaultText":"I HOPE YOU LIKE\nTHE STARS I STOLE\u2028FOR YOU","strike":0,"kern":0,"flipHorizontal":false,"fontName":"PingFangSC-Semibold","fixedKern":0,"bounds":"{{0, 0}, {315, 311}}","center":"{157.5, 155.5}"}]
         * viewCenter : {184.10729431721973, 569.47116200169717}
         * path : /2/86825011b3c25e3457e2e203afd057de
         * scale : 1
         */

        private String currentRect;
        private double rotation;
        private String center;
        private String bounds;
        private long flag;
        private String viewCenter;
        private String path;
        private double scale;
        private int modelVersion;
        private List<ImagesBean> images;
        private List<TextsBean> texts;

        public int getModelVersion() {
            return modelVersion;
        }

        public void setModelVersion(int modelVersion) {
            this.modelVersion = modelVersion;
        }

        public List<Long> getCurrentRect() {
            return StrUtils.stringToLongs(currentRect);
        }

        public void setCurrentRect(String currentRect) {
            this.currentRect = currentRect;
        }

        public double getRotation() {
            return rotation;
        }

        public void setRotation(double rotation) {
            this.rotation = rotation;
        }

        public List<Long> getCenter() {
            return StrUtils.stringToLongs(center);
        }

        public String getCenter1() {
            return center;
        }

        public void setCenter(String center) {
            this.center = center;
        }

        public List<Long> getBounds() {
            return StrUtils.stringToLongs(bounds);
        }

        public String getBounds1() {
            return bounds;
        }

        public void setBounds(String bounds) {
            this.bounds = bounds;
        }

        public long getFlag() {
            return flag;
        }

        public void setFlag(long flag) {
            this.flag = flag;
        }

        public List<Long> getViewCenter() {
            return StrUtils.stringToLongs(viewCenter);
        }

        public String getViewCenter1() {
            return viewCenter;
        }

        public void setViewCenter(String viewCenter) {
            this.viewCenter = viewCenter;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public double getScale() {
            return scale;
        }

        public void setScale(double scale) {
            this.scale = scale;
        }

        public List<ImagesBean> getImages() {
            return images;
        }

        public void setImages(List<ImagesBean> images) {
            this.images = images;
        }

        public List<TextsBean> getTexts() {
            return texts;
        }

        public void setTexts(List<TextsBean> texts) {
            this.texts = texts;
        }

        public static class ImagesBean implements Serializable {
            /**
             * center : {190.59202072176606, 54.828125000000007}
             * name : bg;
             * bounds : {{0, 0}, {32.819261569318229, 109.65625000000001}}
             */

            private String center;
            private String name;
            private String bounds;

            public String getCenter() {
                return center;
            }

            public void setCenter(String center) {
                this.center = center;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getBounds() {
                return bounds;
            }

            public void setBounds(String bounds) {
                this.bounds = bounds;
            }
        }

        public static class TextsBean implements Serializable {
            /**
             * flipVertical : false
             * rotation : 0
             * lineHeight : 36
             * pointSize : 25.012500000000003
             * verticalAlignment : 1
             * fixedPointSize : 25.012500000000003
             * fixedLineHeight : 36
             * type : type=text;up=1;lang=en;line=8;
             * alignment : 2
             * defaultText : I HOPE YOU LIKE
             * THE STARS I STOLE FOR YOU
             * strike : 0
             * kern : 0
             * flipHorizontal : false
             * fontName : PingFangSC-Semibold
             * fixedKern : 0
             * bounds : {{0, 0}, {315, 311}}
             * center : {157.5, 155.5}
             */

            private boolean flipVertical;
            private double rotation;
            private double lineHeight;
            private double pointSize;
            private double verticalAlignment;
            private double fixedPointSize;
            private double fixedLineHeight;
            private String type;
            private double alignment;
            private String defaultText;
            private double strike;
            private double kern;
            private boolean flipHorizontal;
            private String fontName;
            private double fixedKern;
            private String bounds;
            private String center;
            private String currentText;
            private String textColor;

            public boolean isFlipVertical() {
                return flipVertical;
            }

            public String getFlipVertical() {
                if (flipVertical) {
                    return "1.0";
                } else {
                    return "0.0";
                }
            }

            public void setFlipVertical(boolean flipVertical) {
                this.flipVertical = flipVertical;
            }

            public String getTextColor() {
                return textColor;
            }

            public void setTextColor(String textColor) {
                this.textColor = textColor;
            }

            public double getRotation() {
                return rotation;
            }

            public void setRotation(double rotation) {
                this.rotation = rotation;
            }

            public double getLineHeight() {
                return lineHeight;
            }

            public void setLineHeight(double lineHeight) {
                this.lineHeight = lineHeight;
            }

            public double getPointSize() {
                return pointSize;
            }

            public void setPointSize(double pointSize) {
                this.pointSize = pointSize;
            }

            public double getVerticalAlignment() {
                return verticalAlignment;
            }

            public void setVerticalAlignment(double verticalAlignment) {
                this.verticalAlignment = verticalAlignment;
            }

            public double getFixedPointSize() {
                return fixedPointSize;
            }

            public void setFixedPointSize(double fixedPointSize) {
                this.fixedPointSize = fixedPointSize;
            }

            public double getFixedLineHeight() {
                return fixedLineHeight;
            }

            public void setFixedLineHeight(double fixedLineHeight) {
                this.fixedLineHeight = fixedLineHeight;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public double getAlignment() {
                return alignment;
            }

            public void setAlignment(double alignment) {
                this.alignment = alignment;
            }

            public String getDefaultText() {
                return defaultText;
            }

            public void setDefaultText(String defaultText) {
                this.defaultText = defaultText;
            }

            public double getStrike() {
                return strike;
            }

            public void setStrike(double strike) {
                this.strike = strike;
            }

            public double getKern() {
                return kern;
            }

            public void setKern(double kern) {
                this.kern = kern;
            }

            public boolean isFlipHorizontal() {
                return flipHorizontal;
            }

            public String getFlipHorizontal() {
                if (flipHorizontal) {
                    return "1.0";
                } else {
                    return "0.0";
                }
            }

            public void setFlipHorizontal(boolean flipHorizontal) {
                this.flipHorizontal = flipHorizontal;
            }

            public String getFontName() {
                return fontName;
            }

            public void setFontName(String fontName) {
                this.fontName = fontName;
            }

            public double getFixedKern() {
                return fixedKern;
            }

            public void setFixedKern(double fixedKern) {
                this.fixedKern = fixedKern;
            }

            public String getBounds1() {
                return bounds;
            }

            public void setBounds(String bounds) {
                this.bounds = bounds;
            }

            public List<Long> getCenter() {
                return StrUtils.stringToLongs(center);
            }

            public String getCenter1() {
                return center;
            }

            public void setCenter(String center) {
                this.center = center;
            }

            public String getCurrentText() {
                return currentText;
            }

            public void setCurrentText(String currentText) {
                this.currentText = currentText;
            }
        }
    }


    public static class FontData implements Serializable {
        private static final long serialVersionUID = -5223098819332248716L;
        private String id;

        private String object_url;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

//        public String getObject_url() {
//            return Urls.getImageFullUrl(object_url);
//        }

        public void setObject_url(String object_url) {
            this.object_url = object_url;
        }

        public String getFontName() {
            return object_url.substring(object_url.lastIndexOf("/") + 1);
        }

    }

}
