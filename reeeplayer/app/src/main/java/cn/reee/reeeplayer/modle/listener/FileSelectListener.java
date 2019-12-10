package cn.reee.reeeplayer.modle.listener;

import cn.reee.reeeplayer.modle.ZpFileInfo;

/**
 * 文件选择
 * Create by zp on 2019-11-24
 */
public interface FileSelectListener {
    public void selectFile(ZpFileInfo klFileInfo);

    public void removeFile(ZpFileInfo klFileInfo);
}
