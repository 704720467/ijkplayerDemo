package com.zp.libvideoedit.modle;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import com.zp.libvideoedit.EditConstants;
import com.zp.libvideoedit.GPUImage.Core.GPUSize;
import com.zp.libvideoedit.Time.CMTime;
import com.zp.libvideoedit.Time.CMTimeRange;
import com.zp.libvideoedit.exceptions.InvalidVideoSourceException;
import com.zp.libvideoedit.modle.Transition.Origentation;
import com.zp.libvideoedit.modle.effectModel.EffectAdapter;
import com.zp.libvideoedit.utils.EffectAdapterSortBySortPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;


/**
 * Created by gwd on 2018/3/8.
 */

public class AVProject {
    //需要序列化的filed
    private Context mContext;
    private Origentation orientation;
    private int version;
    private MusicModelBean musicModel;
    private float videosTotalDuration;//视频 段总时
    private int videoCount; //脚本视频的个数
    private float videosMinDuration;//视频 段最 总时
    private String globalFilterName;
    private float volumeProportion;
    private float gloatFilterStrength;
    private ArrayList<TimeScaleModel> speedPoints;
    private ArrayList<String> videoIndexes; //各个chunk引 原视频的索引, 如1段视频切成2个chunk,索引就是[0,0]
    private ArrayList<Chunk> chunks;
    private String projectId;
    private ArrayList<EffectAdapter> effectAdapters;
    private ArrayList<EffectAdapter> maskEffectAdapters;

    private AudioFile backGroundMusic;
    private String backGroundMusicPath;
    private GPUSize projectRenderSize;
    private Date createTime;
    private Date modifiTime;
//    private AVProjectVo projectVo;
    private long projectDuration;
    private Bitmap coverImage;
    public String tailorListStr;
    public String allResolveMapStr;
    private String allLvjingMapStr;
    private String allLvjingToningMapStr;
    private String otherObjectJson;
    private boolean needSave;
    private ArrayList<RecodeModel> recodeModels;

    private ArrayList<ModulesBean> modulesBeans;

    private ArrayList<AudioChunk> audioChunks;


    /**
     * 脚本相关
     */


    public AVProject(Context context, final String projectId, boolean needSave) {
        chunks = new ArrayList<Chunk>();
        audioChunks = new ArrayList<AudioChunk>();
        effectAdapters = new ArrayList<EffectAdapter>();
        maskEffectAdapters = new ArrayList<EffectAdapter>();
        speedPoints = new ArrayList<TimeScaleModel>();
        recodeModels = new ArrayList<RecodeModel>();
        mContext = context;
        this.needSave = needSave;
        this.projectId = projectId;
        if (needSave) {
            //创建Projectvo
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo = realm.createObject(AVProjectVo.class);
//                    projectVo.setProjectId(projectId);
//                }
//            });
        }
        //设置创建时间
        this.setCreateTime(new Date());
        //设置修改时间
        this.setModifiTime(new Date());
        //设置音量
        this.setVolumeProportion(0.5f);
        //设置默认的大小
        this.setProjectRenderSize(new GPUSize(1280, 720));
    }


//    public static AVProject projectFromScriptBean(Context context, ScriptJsonBean scriptJsonBean) {
//        AVProject project = new AVProject(context, UUID.randomUUID().toString(), false);
//        project.setGlobalFilterName(LookupInstance.getInstance(context).getName(scriptJsonBean.getGlobalFilterId()));
//        project.setGloatFilterStrength((float) scriptJsonBean.getGlobalFilterStrength());
//        project.setRotation(Origentation.getOrigentation(scriptJsonBean.getOrientation()));
//        project.setVersion(scriptJsonBean.getVersion());
//        project.setVideoCount(scriptJsonBean.getVideoCount());
//        project.setVideoIndexes(new ArrayList<String>(scriptJsonBean.getVideoIndexs()));
//        project.setVideosMinDuration((float) scriptJsonBean.getVideosMinDuration());
//        project.setVideosTotalDuration((float) scriptJsonBean.getVideosTotalDuration());
//        project.setVolumeProportion((float) scriptJsonBean.getVolumeProportion());
//        //慢放相关
//        CMTime tmpTime = CMTime.zeroTime();
//        ScriptJsonBean.SpeedPointsBean nextSpeedPointBean = null;
//        for (int index = 0; index < scriptJsonBean.getSpeedPoints().size(); index++) {
//            ScriptJsonBean.SpeedPointsBean speedPointsBean = scriptJsonBean.getSpeedPoints().get(index);
//            if (index < scriptJsonBean.getSpeedPoints().size() - 1) {
//                nextSpeedPointBean = scriptJsonBean.getSpeedPoints().get(index + 1);
//            }
//            if (nextSpeedPointBean != null) {
//                TimeScaleModel scaleModel = new TimeScaleModel(new CMTime(speedPointsBean.getTimePosition().get(0), speedPointsBean.getTimePosition().get(1)), 1.f / nextSpeedPointBean.getSpeedScale());
//                project.addSpeedPoint(scaleModel);
//            } else {
//                TimeScaleModel scaleModel = new TimeScaleModel(new CMTime(speedPointsBean.getTimePosition().get(0), speedPointsBean.getTimePosition().get(1)), 1.f);
//                project.addSpeedPoint(scaleModel);
//
//            }
//
//        }
//        //恢复音乐
////        project.setMusicModel(scriptJsonBean.getMusicModel());
//        if (scriptJsonBean.getMusicModel() != null && !TextUtils.isEmpty(scriptJsonBean.getMusicModel().getLoaclMusicPath())) {
//            CMTimeRange timeRange = new CMTimeRange(CMTime.zeroTime(), new CMTime(scriptJsonBean.getVideosTotalDuration()));
//            TrackType trackType = TrackType.TrackType_Audio_BackGround;//7 背景音
//            AudioChunk audioChunk = new AudioChunk(scriptJsonBean.getMusicModel().getLoaclMusicPath(), null, context, trackType, true);
//            audioChunk.setInsertTime(CMTime.zeroTime());
//            audioChunk.setChunkEditTimeRange(timeRange);
//            project.getAudioChunks().add(audioChunk);
//        }
//
//        //恢复chunks
//        for (int index = 0; index < scriptJsonBean.getChunks().size(); index++) {
//            ScriptJsonBean.ChunksBean chunksBean = scriptJsonBean.getChunks().get(index);
//            Chunk chunk = Chunk.chunkFromeBean(context, chunksBean);
//            chunk.setTransition(chunk.getTransitionStyle(), project.getRotation(), chunk.getChunkTransitionTime());
//            project.addChunk(chunk);
//        }
//        //添加素材 赋值
//        int mTextTpyePosition = 0;
//        if (scriptJsonBean.getModules() != null) {
//            for (int i = 0; i < scriptJsonBean.getModules().size(); i++) {
//                ScriptJsonBean.ModulesBean modulesBean = scriptJsonBean.getModules().get(i);
//                if (modulesBean.getType() == 2) {
//
////                    if (SelectActivity.getTypesetBitmaps() == null || SelectActivity.getTypesetBitmaps().size() <= mTextTpyePosition)
////                        continue;
////                    String imgPath = SelectActivity.getTypesetBitmaps().get(mTextTpyePosition).getImgPath();
////                    Bitmap bitmap = BitmapUtil.loadFileToBitmap(imgPath);
////                    if (bitmap == null) continue;
//
//                    EffectAdapter adapter = new EffectAdapter(UUID.randomUUID().toString(), EffectType.EffectType_Pic);
//                    CMTimeRange timeRange = new CMTimeRange(new CMTime(modulesBean.getTimeRange().get(0), modulesBean.getTimeRange().get(1)), new CMTime(modulesBean.getTimeRange().get(2), modulesBean.getTimeRange().get(3)));
//                    adapter.setTimeRange(timeRange);
////                    adapter.setBitmap(bitmap);
//                    adapter.setPosition(mTextTpyePosition);
//                    project.getEffectAdapters().add(adapter);
//                    mTextTpyePosition++;
//                } else if (modulesBean.getType() == 4 || modulesBean.getType() == 7 || modulesBean.getType() == 9) {//添加音频
////                    String dirPath = TypesetUtils.getAudioPath(modulesBean.getName());
//                    String dirPath;
//                    if (modulesBean.getType() == 4) {
//                        dirPath = AeResourceDownloadModule.getAudioRecordPath(
//                                scriptJsonBean.getAudioResourceUrl(), modulesBean.getName());
//                    } else {
//                        dirPath = AeResourceDownloadModule.getLocalPath(modulesBean.getUrl(), 1, null);
//                    }
//                    if (EditConstants.VERBOSE) Log.i("debbug", "dirPath=" + dirPath);
//                    CMTimeRange timeRange = new CMTimeRange(modulesBean.getContentTimeRangeString());
//                    CMTime atTime = new CMTime(modulesBean.getTimeRange().get(0), modulesBean.getTimeRange().get(1));
//
//                    TrackType trackType = TrackType.TrackType_Audio_BackGround;//7 背景音
//                    if (modulesBean.getType() != 7)//4 录音  9 音效
//                        trackType = modulesBean.getType() == 4 ? TrackType.TrackType_Audio_Recoder : TrackType.TrackType_Audio_SOUND_EFFECT;
//                    AudioChunk audioChunk = new AudioChunk(dirPath, null, context, trackType, true);
//                    audioChunk.setInsertTime(atTime);
//                    audioChunk.setChunkEditTimeRange(timeRange);
//                    project.getAudioChunks().add(audioChunk);
////                    RecodeModel recodeModel = new RecodeModel(timeRange, atTime, dirPath);
////                    project.setRecodeModel(recodeModel, true);
//                }
////                else {
////                    final String[] dirPathArray = {null};
////                    Common.runOnMainQueueWithoutDeadlocking(new Runnable() {
////                        @Override
////                        public void run() {
////                            dirPathArray[0] = TypesetUtils.getVideoTypesetPath(modulesBean.getId());
////                        }
////                    });
////                    String dirPath = dirPathArray[0];
////                    EffectAdapter adapter = new EffectAdapter(UUID.randomUUID().toString(), EffectType.EffectType_Video);
////                    CMTimeRange timeRange = new CMTimeRange(new CMTime(modulesBean.getTimeRange().get(0), modulesBean.getTimeRange().get(1)), new CMTime(modulesBean.getTimeRange().get(2), modulesBean.getTimeRange().get(3)));
////                    String maskPath = dirPath + "/t";
////                    String maskExtPath = dirPath + "/t_m";
////                    String oriStr = "_v";
////                    if (project.getRotation() == Origentation.kVideo_Horizontal) {
////                        oriStr = "_h";
////                    }
////                    maskPath = maskPath + oriStr + ".mp4";
////                    maskExtPath = maskExtPath + oriStr + ".mp4";
////                    try {
////                        if (EditConstants.VERBOSE)
////                            Log.i("Check_Typesetting_Path", "projectFromScriptBean_Typesetting Path maskPath：" + maskPath + "；Is Exists=" + FileUtils.fileIsExists(maskPath));
////                        if (EditConstants.VERBOSE)
////                            Log.i("Check_Typesetting_Path", "projectFromScriptBean_Typesetting Path maskExtPath：" + maskExtPath + "；Is Exists=" + FileUtils.fileIsExists(maskExtPath));
////                        adapter.setMaskVideoChunk(new Chunk(maskPath, context, false));
////                        adapter.setMaskExtVideoChunk(new Chunk(maskExtPath, context, false));
////                    } catch (InvalidVideoSourceException e) {
////                        e.printStackTrace();
////                    }
////                    adapter.setTimeRange(timeRange);
////                    project.getEffectAdapters().add(adapter);
////                }
//            }
//        }
//        //加载视频
//        return project;
//    }

    public AVProject(Context context, final String projectId) {
        chunks = new ArrayList<Chunk>();
        audioChunks = new ArrayList<AudioChunk>();
        effectAdapters = new ArrayList<EffectAdapter>();
        maskEffectAdapters = new ArrayList<EffectAdapter>();
        speedPoints = new ArrayList<TimeScaleModel>();
        recodeModels = new ArrayList<RecodeModel>();
        mContext = context;
//        this.projectVo = projectVo;
        this.projectId = projectId;
        this.setCreateTime(new Date());
        this.setModifiTime(new Date());
        this.setProjectRenderSize(new GPUSize(1280, 720));
    }


    public ArrayList<EffectAdapter> getEffectAdapters() {
        return effectAdapters;
    }

    public ArrayList<EffectAdapter> getMaskEffectAdapters() {
        return maskEffectAdapters;
    }

    public void addEffectFromDraf(EffectAdapter effectAdapter) {
        effectAdapters.add(effectAdapter);
    }

    public void addEffect(EffectAdapter effectAdapter) {
        effectAdapters.add(effectAdapter);
        Collections.sort(effectAdapters, new EffectAdapterSortBySortPosition());
        if (needSave) {
//            final EffectAdapterVo adapterVo = new EffectAdapterVo();
//            adapterVo.setEffectId(effectAdapter.getEffectId());
//            adapterVo.setTimeRange(effectAdapter.getTimeRange().timeRangeVo());
//            adapterVo.setEffectType(effectAdapter.getEffectType().getValue());
//            if (effectAdapter.getEffectType() == EffectType.EffectType_Video) {
//                adapterVo.setFilter("VNiVideoBlendFilter");
//            } else if (effectAdapter.getEffectType() == EffectType.EffectType_Pic) {
//                adapterVo.setFilter("GPUImageAlphaBlendFilter");
//            } else if (effectAdapter.getEffectType() == EffectType.EffectType_Sticker) {
//                adapterVo.setFilter("VNIStickerFilter");
//                adapterVo.setStickerConfigVo(effectAdapter.getStickerConfig().getStickerConfigVo());
//            } else if (effectAdapter.getEffectType() == EffectType.EffectType_Special_Effect) {
//                adapterVo.setFilter("VNISpecialEffectsFilter");
//                adapterVo.setSpecialEffectJson(effectAdapter.getSpecialEffectJson());
//            }
//            if (effectAdapter.getMaskVideoChunk() != null) {
//                adapterVo.setMaskVideoChunk(effectAdapter.getMaskVideoChunk().getChunkVo());
//            }
//            if (effectAdapter.getMaskExtVideoChunk() != null) {
//                adapterVo.setMaskExtVideoChunk(effectAdapter.getMaskExtVideoChunk().getChunkVo());
//            }
//            if (effectAdapter.getBitmap() != null) {
//                adapterVo.setBitmap(Common.bitMapToByte(effectAdapter.getBitmap()));
//            }
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.getEffectAdapters().add(adapterVo);
//                    realm.insertOrUpdate(projectVo);
//                }
//            });
        }
    }

    public void addMaskEffect(EffectAdapter effectAdapter) {
        maskEffectAdapters.add(effectAdapter);
        if (needSave) {
//            final EffectAdapterVo adapterVo = new EffectAdapterVo();
//            adapterVo.setEffectId(effectAdapter.getEffectId());
//            adapterVo.setEffectType(effectAdapter.getEffectType().getValue());
//            if (effectAdapter.getEffectType() == EffectType.EffectType_Video) {
//                adapterVo.setFilter("VNiVideoBlendFilter");
//            } else if (effectAdapter.getEffectType() == EffectType.EffectType_Pic) {
//                adapterVo.setFilter("GPUImageAlphaBlendFilter");
//            }
//            if (effectAdapter.getMaskVideoChunk() != null) {
//                adapterVo.setMaskVideoChunk(effectAdapter.getMaskVideoChunk().getChunkVo());
//            }
//            if (effectAdapter.getMaskExtVideoChunk() != null) {
//                adapterVo.setMaskExtVideoChunk(effectAdapter.getMaskExtVideoChunk().getChunkVo());
//            }
//            if (effectAdapter.getBitmap() != null) {
//                adapterVo.setBitmap(Common.bitMapToByte(effectAdapter.getBitmap()));
//            }
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.getMaskEffectAdapters().add(adapterVo);
//                }
//            });
        }
    }

    public void removeEffect(final EffectAdapter adapter) {
        final int index = effectAdapters.indexOf(adapter);
        effectAdapters.remove(adapter);
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    for (EffectAdapterVo effectAdapterVo : projectVo.getEffectAdapters()) {
//                        if (effectAdapterVo.getEffectId().equals(adapter.getEffectId())) {
//                            projectVo.getEffectAdapters().remove(effectAdapterVo);
//                            break;
//                        }
//                    }
//                    realm.insertOrUpdate(projectVo);
//                }
//            });
        }
    }

    public void removeMaskEffect(EffectAdapter adapter) {
        final int index = maskEffectAdapters.indexOf(adapter);
        maskEffectAdapters.remove(adapter);
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.getMaskEffectAdapters().remove(index);
//                }
//            });
        }
    }

    public void addChunk(String filePath) throws InvalidVideoSourceException {
        if (filePath == null) return;
        final Chunk newChunk = new Chunk(filePath, mContext, needSave);
        newChunk.setAudioVolumeProportion(getVolumeProportion());
        chunks.add(newChunk);
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.getChunks().add(newChunk.getChunkVo());
//                }
//            });
        }
    }

    public void loadVideoModel() {

    }

    public void addChunk(final Chunk chunk) {
        if (chunk != null) {
            chunks.add(chunk);
        }
    }

    public Chunk addChunk(String filePath, VideoFile videoFile, AudioFile audioFile, float jingdu, float weidu) throws InvalidVideoSourceException {
        if (filePath == null) return null;
        final Chunk newChunk = new Chunk(filePath, mContext, videoFile, audioFile, jingdu, weidu, needSave);
        newChunk.setAudioVolumeProportion(getVolumeProportion());
        if (newChunk != null) {
            chunks.add(newChunk);
            if (needSave) {
//                DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                    @Override
//                    public void execute(Realm realm) {
//                        projectVo.getChunks().add(newChunk.getChunkVo());
//                    }
//                });
            }
        }
        return newChunk;
    }

    public void exchangeChunk(final int sourceIndex, final int toIndex) {
        final Chunk tmp = chunks.get(sourceIndex);
        chunks.remove(sourceIndex);
        chunks.add(toIndex, tmp);
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.getChunks().remove(sourceIndex);
//                    projectVo.getChunks().add(toIndex, tmp.getChunkVo());
//                }
//            });
        }
    }

//    public LeastScript getLeastScript() {
//        LeastScript leastScript = new LeastScript();
//        leastScript.setOrientation(this.orientation.getValue());
//        ArrayList<String> videoDurations = new ArrayList<>();
//        for (int i = 0; i < getVideoCount(); i++) {
//            float duration = minVideoDurationOfVideoIndex(i);
//            videoDurations.add(String.valueOf(duration));
//        }
//        leastScript.setVideoDurations(videoDurations);
//        return leastScript;
//    }

    public ArrayList<RecodeModel> getRecodeModels() {
        return recodeModels;
    }

    public void setRecodeModels(ArrayList<RecodeModel> recodeModels) {
        this.recodeModels = recodeModels;
    }

    /**
     * 是否存在录音
     *
     * @return true 存在录音，false：不存在录音
     */
    public boolean isExistRecodeModel() {
        return (recodeModels != null && !recodeModels.isEmpty());
    }

    /**
     * 设置录音
     *
     * @param recodeModel
     * @param isAddOrUpdataRecodeModel 是否添加录音，true:添加或者更新录音，false：删除录音
     */
    public void setRecodeModel(final RecodeModel recodeModel, final boolean isAddOrUpdataRecodeModel) {
        if (isAddOrUpdataRecodeModel) {//添加录音或者更新录音
            boolean isNewRecoder = true;
            for (RecodeModel recodeModel1 : recodeModels) {
                if (recodeModel1.getFilePath().equals(recodeModel.getFilePath())) {
                    isNewRecoder = false;
                    recodeModel1.setTimeRange(recodeModel.getTimeRange());
                    recodeModel1.setAtTime(recodeModel.getAtTime());
                    break;
                }
            }
            if (isNewRecoder)
                recodeModels.add(recodeModel);
        } else {
            recodeModels.remove(recodeModel);
        }
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    if (isAddOrUpdataRecodeModel) {//添加录音或者更新录音
//                        ReCodeModelVo reCodeModelVo = null;
//                        for (ReCodeModelVo codeModelVo1 : projectVo.getReCodeModelVos()) {
//                            if (codeModelVo1.getFilePath().equals(recodeModel.getFilePath())) {
//                                reCodeModelVo = codeModelVo1;
//                                break;
//                            }
//                        }
//                        boolean isNewRecoder = false;
//                        if (reCodeModelVo == null) {//新增录音
//                            reCodeModelVo = realm.createObject(ReCodeModelVo.class, UUID.randomUUID().toString());
//                            isNewRecoder = true;
//                        }
//                        reCodeModelVo.setFilePath(recodeModel.getFilePath());
//                        CMTimeVo attime = realm.createObject(CMTimeVo.class, UUID.randomUUID().toString());
//                        attime.setTimeScale(recodeModel.getAtTime().getTimeScale());
//                        attime.setValue(recodeModel.getAtTime().getValue());
//                        reCodeModelVo.setAtTime(attime);
//                        CMTimeVo start = realm.createObject(CMTimeVo.class, UUID.randomUUID().toString());
//                        CMTimeVo end = realm.createObject(CMTimeVo.class, UUID.randomUUID().toString());
//                        start.setTimeScale(recodeModel.getTimeRange().getStartTime().getTimeScale());
//                        start.setValue(recodeModel.getTimeRange().getStartTime().getValue());
//                        end.setTimeScale(recodeModel.getTimeRange().getDuration().getTimeScale());
//                        end.setValue(recodeModel.getTimeRange().getDuration().getValue());
//                        CMTimeRangeVo timeRangevo = realm.createObject(CMTimeRangeVo.class, UUID.randomUUID().toString());
//                        timeRangevo.setStartTime(start);
//                        timeRangevo.setDuration(end);
//                        reCodeModelVo.setTimeRange(timeRangevo);
//                        if (isNewRecoder)
//                            projectVo.getReCodeModelVos().add(reCodeModelVo);
//                    } else {//删除录音
//                        ReCodeModelVo deleteRecodeModelVo = null;
//                        for (ReCodeModelVo reCodeModelVo : projectVo.getReCodeModelVos()) {
//                            if (reCodeModelVo.getFilePath().equals(recodeModel.getFilePath())) {
//                                deleteRecodeModelVo = reCodeModelVo;
//                                break;
//                            }
//                        }
//                        if (deleteRecodeModelVo != null)
//                            projectVo.getReCodeModelVos().remove(deleteRecodeModelVo);
//                    }
//                    realm.insertOrUpdate(projectVo);
//                }
//            });
        }
    }

    public void insertChunk(String chunk, final int chunkIndex) throws InvalidVideoSourceException {
        if (chunk == null) return;
        final Chunk newChunk = new Chunk(chunk, mContext, needSave);
        newChunk.setAudioVolumeProportion(getVolumeProportion());
        if (newChunk != null) {
            chunks.add(chunkIndex, newChunk);
            if (needSave) {
//                DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                    @Override
//                    public void execute(Realm realm) {
//                        projectVo.getChunks().add(chunkIndex, newChunk.getChunkVo());
//                    }
//                });
            }
        }
    }

    public void insertChunk(String chunk, final int chunkIndex, ChunkType type) throws InvalidVideoSourceException {
        if (chunk == null) return;
        final Chunk newChunk = new Chunk(chunk, mContext, needSave, type);
        newChunk.setAudioVolumeProportion(getVolumeProportion());
        if (newChunk != null) {
            chunks.add(chunkIndex, newChunk);
            if (needSave) {
//                DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                    @Override
//                    public void execute(Realm realm) {
//                        projectVo.getChunks().add(chunkIndex, newChunk.getChunkVo());
//                    }
//                });
            }
        }
    }


    public void insertChunk(final Chunk chunk, final int chunkIndex) {
        if (chunk == null) return;
        chunks.add(chunkIndex, chunk);
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.getChunks().add(chunkIndex, chunk.getChunkVo());
//                }
//            });
        }
    }

    public void deleteChunk(final int chunkIndex) {
        if (chunkIndex >= chunks.size()) return;
        chunks.remove(chunkIndex);
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.getChunks().remove(chunkIndex);
//                }
//            });
        }
    }

    public void deleteChunk(final Chunk chunk) {
        if (chunk != null && chunks.contains(chunk)) {
            chunks.remove(chunk);
            if (needSave) {
//                DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                    @Override
//                    public void execute(Realm realm) {
//                        projectVo.getChunks().remove(chunk.getChunkVo());
//                    }
//                });
            }
        }
    }

    //查找时间对应的chunk
    public ArrayList<Chunk> getChunkWithSecond(float second) {
        ArrayList<Chunk> chunks = new ArrayList<>();
        for (Chunk chunk : this.chunks) {
            try {
                if (second >= CMTime.getSecond(chunk.getStartTime()) && second < CMTime.getSecond(chunk.getEndTime())) {
                    chunks.add(chunk);
                }
            } catch (Exception e) {
                Log.w(EditConstants.TAG_M, "AVProject_getChunkWithSecond:" + second + ", chunk:" + chunk);
            }
        }

        if (chunks.isEmpty() && !this.chunks.isEmpty()) {
            Chunk chunk = this.chunks.get(this.chunks.size() - 1);
            if (chunk != null && chunk.getEndTime() != null && second >= CMTime.getSecond(chunk.getEndTime())) {
                chunks.add(chunk);
            }
        }
        return chunks;
    }

    public ArrayList<Chunk> getChunks() {
        return chunks;
    }

    public ArrayList<AudioChunk> getAudioChunks() {
        return audioChunks;
    }

    /**
     * 根据chunkId 返回audiochunk
     *
     * @param id
     * @return
     */
    public AudioChunk getAudioChunkById(String id) {
        if (audioChunks == null) return null;
        for (AudioChunk audioChunk : audioChunks) {
            if (audioChunk.getChunkId().equals(id)) return audioChunk;
        }
        return null;
    }


    public boolean hasAudio() {
        boolean hasaudio = false;
        for (Chunk chunk : chunks) {
            if (chunk.getAudioFile() != null) {
                hasaudio = true;
                break;
            }
        }
        if (hasaudio) return true;
        return false;
    }

    public ArrayList<TimeScaleModel> getSpeedPoints() {
        return speedPoints;
    }

    public void addBackGroundMusic(final String backGroundMusicPath) throws InvalidVideoSourceException {
        this.backGroundMusicPath = backGroundMusicPath;
        if (backGroundMusicPath == null || backGroundMusicPath.length() == 0) {
            this.backGroundMusic = null;
            return;
        }
        this.backGroundMusic = AudioFile.getAudioFileInfo(this.backGroundMusicPath, mContext);
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.setBackGroundMusicPath(backGroundMusicPath);
//                }
//            });
        }
    }

    public void setRotation(final Origentation rotation) {
        this.orientation = rotation;
        for (Chunk chunk : chunks) {
            chunk.setVideoAspectOrigentation(rotation);
        }
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.setRotation(rotation.getValue());
//                }
//            });
        }
    }

    public Origentation getRotation() {
        return orientation;
    }

    public void copyChunk(int index) {
        Chunk chunk = chunks.get(index);
        Chunk newChunk = chunk.mutableCopy();
        insertChunk(newChunk, index + 1);
    }

    public GPUSize getProjectRenderSize() {
        return projectRenderSize;
    }

    public void setProjectRenderSize(final GPUSize projectRenderSize) {
        this.projectRenderSize = projectRenderSize;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    GPUSizeVo vo2 = realm.where(GPUSizeVo.class).equalTo("id", projectVo.getProjectId()).findFirst();
//                    if (vo2 == null) {
//                        GPUSizeVo vo = realm.createObject(GPUSizeVo.class, projectVo.getProjectId());
//                        vo.setHeight(projectRenderSize.height);
//                        vo.setWidth(projectRenderSize.width);
//                        projectVo.setProjectRenderSize(vo);
//                    } else {
//                        vo2.setHeight(projectRenderSize.height);
//                        vo2.setWidth(projectRenderSize.width);
//                    }
//                }
//            });
        }
    }

    public float getVolumeProportion() {
        return volumeProportion;
    }

    public void setVolumeProportion(final float volumeProportion) {
        this.volumeProportion = volumeProportion;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.setVolumeProportion(volumeProportion);
//                }
//            });
        }
    }

    public String getGlobalFilterName() {
        return globalFilterName;
    }

    public float getGloatFilterStrength() {
        return gloatFilterStrength;
    }

    public void setGlobalFilterName(final String globalFilterName) {
        this.globalFilterName = globalFilterName;
        if (this.globalFilterName == null || this.globalFilterName.length() == 0) return;
        for (Chunk chunk : chunks) {
            chunk.setFilterName(globalFilterName);
        }
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.setGlobalFilterName(globalFilterName);
//                }
//            });
        }
    }

    public void setGloatFilterStrength(final float gloatFilterStrength) {
        this.gloatFilterStrength = gloatFilterStrength;
        for (Chunk chunk : chunks) {
            chunk.setStrengthValue(gloatFilterStrength);
        }
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.setGloatFilterStrength(gloatFilterStrength);
//                }
//            });
        }
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(final Date createTime) {
        this.createTime = createTime;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.setCreateTime(createTime);
//                }
//            });
        }
    }

    public Date getModifiTime() {
        return modifiTime;
    }

    public void setModifiTime(final Date modifiTime) {
        this.modifiTime = modifiTime;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.setModifiTime(modifiTime);
//                }
//            });
        }
    }

    public long getProjectDuration() {
        return projectDuration;
    }

    public void setProjectDuration(final long projectDuration) {
        this.projectDuration = projectDuration;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.setProjectDuration(projectDuration);
//                }
//            });
        }
    }

    public Bitmap getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(final Bitmap coverImage) {
        this.coverImage = coverImage;
//        Common.runOnMainQueueWithoutDeadlocking(new Runnable() {
//            @Override
//            public void run() {
//                if (needSave) {
//                    DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                        @Override
//                        public void execute(Realm realm) {
//                            if (coverImage != null) {
//                                byte[] bitmapData = Common.bitMapToByte(coverImage);
//                                projectVo.setCoverImageData(bitmapData);
//                            }
//                        }
//                    });
//                }
//            }
//        });


    }

    public String getTailorListStr() {
        return tailorListStr;
    }

    public void setTailorListStr(final String tailorListStr) {
        this.tailorListStr = tailorListStr;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.setTailorListStr(tailorListStr);
//                }
//            });
        }
    }

    public String getAllResolveMapStr() {
        return allResolveMapStr;
    }

    public void setAllResolveMapStr(final String allResolveMapStr) {
        this.allResolveMapStr = allResolveMapStr;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.setAllResolveMapStr(allResolveMapStr);
//                }
//            });
        }

    }

    public String getAllLvjingMapStr() {
        return allLvjingMapStr;
    }

    public void setAllLvjingMapStr(final String allLvjingMapStr) {
        this.allLvjingMapStr = allLvjingMapStr;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.setAllLvjingMapStr(allLvjingMapStr);
//                }
//            });
        }

    }

    public String getAllLvjingToningMapStr() {
        return allLvjingToningMapStr;
    }

    public void setAllLvjingToningMapStr(final String allLvjingToningMapStr) {
        this.allLvjingToningMapStr = allLvjingToningMapStr;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.setAllLvjingToningMapStr(allLvjingToningMapStr);
//                }
//            });
        }
    }

    public String getOtherObject() {
        return otherObjectJson;
    }

    public void setOtherObject(final String otherObject) {
        this.otherObjectJson = otherObject;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.setOtherObjectJson(otherObject);
//                }
//            });
        }
    }

    public AudioFile getBackGroundMusic() {
        return backGroundMusic;
    }

    public String getBackGroundMusicPath() {
        if (musicModel == null) return null;
        return musicModel.getLoaclMusicPath();
    }

//    public AVProjectVo getProjectVo() {
//        return projectVo;
//    }
//
//    public void setProjectVo(AVProjectVo projectVo) {
//        this.projectVo = projectVo;
//    }


    /**
     * 脚本先关
     */
    //注意,是源视频的索引
    public float videoDurationOfVideoIndex(int index) {
        int chunkdIndex = videoIndexes.indexOf(new Integer(index).toString());
        return chunks.get(chunkdIndex).getVideoDuration();
    }

    public float minVideoDurationOfVideoIndex(int index) {
        float minDuration = 0.f;
        int chunkIndex = 0;
        ArrayList<Chunk> tmpChunks = new ArrayList<>();
        for (Chunk chunk : chunks) {
            if (chunk.getChunkType() == ChunkType.ChunkType_Default) {
                tmpChunks.add(chunk);
            }
        }
        for (String videoIndex : videoIndexes) {
            if (Integer.parseInt(videoIndex) == index) {
                minDuration = Math.max(minDuration, tmpChunks.get(chunkIndex).getMinVideoDuration());
            }
            chunkIndex++;
        }
        return minDuration;
    }


    public boolean containReverseVideo() {
        boolean contain = false;
        for (Chunk chunk : chunks) {
            if (chunk.isReverseVideo()) {
                contain = true;
                break;
            }
        }
        return contain;
    }

    public boolean needReverseVideoOfVideoIndex(int index) {
        boolean needReverse = false;
        ArrayList<Chunk> chunks = getChunks();
        for (Chunk chunk : chunks) {
            if (chunk.getVideoIndex() == index && chunk.isReverseVideo()) {
                needReverse = true;
                break;
            }
        }
        return needReverse;
    }

    public int getVideoCount() {
        return videoCount;
    }

    public void setVideoCount(int videoCount) {
        this.videoCount = videoCount;
    }

    public ArrayList<String> getVideoIndexes() {
        return videoIndexes;
    }

    public void setVideoIndexes(ArrayList<String> videoIndexes) {
        this.videoIndexes = videoIndexes;
    }

    public float getVideosTotalDuration() {
        return videosTotalDuration;
    }

    public void setVideosTotalDuration(float videosTotalDuration) {
        this.videosTotalDuration = videosTotalDuration;

    }

    public float getVideosMinDuration() {
        return videosMinDuration;
    }

    public void setVideosMinDuration(float videosMinDuration) {
        this.videosMinDuration = videosMinDuration;
    }

    public MusicModelBean getMusicModel() {
        return musicModel;
    }

    public void setMusicModel(final MusicModelBean musicModelBean) {
        this.musicModel = musicModelBean;
        this.backGroundMusicPath = (this.musicModel != null) ? (this.musicModel.getLoaclMusicPath()) : null;

        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    MusicModelVo musicModelVo = realm.where(MusicModelVo.class).equalTo("id", projectVo.getProjectId()).findFirst();
//                    if (musicModelVo == null) {
//                        musicModelVo = realm.createObject(MusicModelVo.class, projectVo.getProjectId());
//                        musicModelVo.musicModelToMusicModelVo(musicModel, realm);
//                        projectVo.setMusicModelVo(musicModelVo);
//                    } else {
////                        if (musicModel == null || TextUtils.isEmpty(musicModel.getLoaclMusicPath()))
////                            musicModelVo.musicModelToMusicModelVo(musicModel, realm);
////                        else
//                        if (musicModel == null) {
////                            DBManage.getInstance().removeVo(MusicModelVo.class, "id", projectVo.getProjectId());
//                            musicModelVo.deleteFromRealm();
//                            projectVo.setMusicModelVo(null);
//                        } else
//                            musicModelVo.updataMusicModelVoByMusicModel(musicModel, realm);
//                        projectVo.setBackGroundMusicPath(backGroundMusicPath);
//                        realm.insertOrUpdate(projectVo);
//
//                    }
//                }
//            });
        }

        if (musicModel == null || TextUtils.isEmpty(backGroundMusicPath)) {
            this.backGroundMusic = null;
            return;
        }
        try {
            this.backGroundMusic = AudioFile.getAudioFileInfo(this.backGroundMusicPath, mContext);
        } catch (InvalidVideoSourceException e) {
            e.printStackTrace();
        }

    }

    public void updateScriptInfo() {
        //按照引用视频分类chunk
        HashMap<String, ArrayList<Chunk>> classify = new HashMap<>();
        for (Chunk chunk : chunks) {
            String path = chunk.getFilePath();
            if (path.length() < 1) continue;
            ArrayList<Chunk> marr = classify.get(path);
            if (chunk.getChunkType() == ChunkType.ChunkType_White || chunk.getChunkType() == ChunkType.ChunkType_Black) {
                chunk.setTimePoint(0.f);
                chunk.setDuration((float) chunk.getChunkEditTimeRange().getDuration().getSecond());
                chunk.setVideoDuration(2.f);
                chunk.setMinVideoDuration(2.f);
                continue;
            }
            if (marr == null) {
                classify.put(path, new ArrayList());
            }
            classify.get(path).add(chunk);
        }
        for (String key : classify.keySet()) {
            ArrayList<Chunk> tempChunks = classify.get(key);
            CMTime minStartTime = CMTime.zeroTime();
            CMTime maxEndTime = CMTime.zeroTime();
            for (Chunk chunk : tempChunks) {
                CMTimeRange editTimeRange = chunk.editTimeRangeForVideoScript();
                CMTime originalStartTime = (chunk.isReverseVideo()) ?
                        (CMTime.subTime(chunk.getVideoFile().getcDuration(), editTimeRange.getEnd()))
                        : (editTimeRange.getStartTime());
                CMTime endStartTime = CMTime.addTime(originalStartTime, editTimeRange.getDuration());
                minStartTime = CMTime.Minimum(minStartTime, originalStartTime);
                maxEndTime = CMTime.Maxmum(maxEndTime, endStartTime);
            }
            //TODO CMTime.subTime 会出现负值的情况需要考虑
            CMTime effectiveDuration = CMTime.subTime(maxEndTime, minStartTime);
            //计算脚本视频点信息
            for (Chunk chunk : tempChunks) {
                float videoDuration = (float) CMTime.getSecond(effectiveDuration);
                CMTimeRange editTimeRange = chunk.editTimeRangeForVideoScript();
                if (videoDuration > 0) {

                    chunk.setTimePoint((float) CMTime.getSecond(CMTime.subTime(editTimeRange.getStartTime(), minStartTime)) / videoDuration);
                }
                chunk.setDuration((float) CMTime.getSecond(editTimeRange.getDuration()));
                if (chunk.getTimePoint() == 1.f) {
                    chunk.setMinVideoDuration(videoDuration);
                } else {
                    if (chunk.isReverseVideo()) {//倒播时选择片段时间问题和timePoint问题,timePoint是对原始视频来说的

                        float totalDuration = (float) chunk.getVideoFile().getcDuration().getSecond();
                        if (chunk.editTimeRangeForVideoScript().getStartTime().getSecond() == 0) {

                        }

                        float minVideoDuration = (float) (chunk.getVideoFile().getcDuration().getSecond() - editTimeRange.getStartTime().getSecond());
//                        float originalStartTime = videoDuration - (float) CMTime.getSecond(editTimeRange.getEnd());
//                        float timePoint = (float) originalStartTime / videoDuration;
                        float originalStartTime = (float) CMTime.getSecond(CMTime.subTime(chunk.getVideoFile().getcDuration(), editTimeRange.getEnd()));
                        float timePoint = originalStartTime / videoDuration;

                        chunk.setTimePoint(timePoint);
                        chunk.setMinVideoDuration(minVideoDuration);
                    } else
                        chunk.setMinVideoDuration(chunk.getDuration() / (1.f - chunk.getTimePoint()));
                }
                chunk.setVideoDuration(videoDuration);
            }
        }
        ArrayList<String> chunkIndexs = new ArrayList<>();
        int effectiveChunkCount = 0;
        ArrayList<String> paths = new ArrayList<>();
        HashMap<String, Float> minDurationMap = new HashMap();
        float totalDuration = 0;
        for (Chunk chunk : chunks) {
            String path = chunk.getFilePath();
            if (path.length() < 1 || chunk.getChunkType() != ChunkType.ChunkType_Default) {
                chunk.setVideoIndex(-1);
                continue;
            }
            if (paths.contains(path)) {
                int index = paths.indexOf(path);
                chunk.setVideoIndex(index);
                chunkIndexs.add(new Integer(index).toString());
                float minDuration = minDurationMap.get(new Integer(index).toString()).floatValue();
                if (chunk.getMinVideoDuration() > minDuration) {
                    minDurationMap.put(new Integer(index).toString(), new Float(chunk.getMinVideoDuration()));
                }
            } else {
                paths.add(path);
                chunk.setVideoIndex(effectiveChunkCount);
                chunkIndexs.add(new Integer(effectiveChunkCount).toString());
                minDurationMap.put(new Integer(effectiveChunkCount).toString(), new Float(chunk.getMinVideoDuration()));
                effectiveChunkCount++;
                totalDuration += chunk.getVideoDuration();
            }
        }
        videoIndexes = chunkIndexs;
        videoCount = effectiveChunkCount;
        videosTotalDuration = totalDuration;
        float minimunDuration = 0;
        for (Float duration : minDurationMap.values()) {
            minimunDuration += duration.floatValue();
        }
        videosMinDuration = minimunDuration;
    }


//    public String projectToJsonBean() {
//        updateScriptInfo();
//        ScriptJsonBean jsonBean = new ScriptJsonBean();
//        jsonBean.setOrientation(this.getRotation().getValue());
//        jsonBean.setGlobalFilterStrength(this.getGloatFilterStrength());
//        jsonBean.setVolumeProportion(this.getVolumeProportion());
//        jsonBean.setGlobalFilterId(LookupInstance.getInstance(mContext).getId(this.getGlobalFilterName()));
//        jsonBean.setVideosMinDuration(this.getVideosMinDuration());
//        jsonBean.setVideoCount(this.getVideoCount());
////        jsonBean.setVersion(this.getVersion());
//        jsonBean.setVersion(1);
//        jsonBean.setVideoIndexs(this.getVideoIndexes());
//        jsonBean.setVideosTotalDuration(this.getVideosTotalDuration());
//        jsonBean.setMusicModel(this.getMusicModel());
//        //速度相关
//        addSpeedData(jsonBean);
//        //chunk相关
//        ArrayList<ScriptJsonBean.ChunksBean> chunksBeans = new ArrayList<>();
//        for (Chunk chunk : this.chunks) {
//            ScriptJsonBean.ChunksBean chunksBean = new ScriptJsonBean.ChunksBean();
//            chunksBean.setDuration(chunk.getDuration());
//            chunksBean.setAudioMixProportion(chunk.getAudioVolumeProportion());
//            chunksBean.setColortemperatureValue(chunk.getColortemperatureValue());
//            chunksBean.setContrastValue(chunk.getContrastValue());
//            chunksBean.setDuration(chunk.getDuration());
//            chunksBean.setTransitionStyle(chunk.getTransitionStyle().getValue());
//            chunksBean.setFilterId(LookupInstance.getInstance(mContext).getId(chunk.getFilterName()));
//            chunksBean.setFilterStrength(chunk.getStrengthValue());
//            chunksBean.setTransitionDuration(StringUtil.stringFromCMTime(chunk.getChunkTransitionTime()));
//            chunksBean.setHighlightValue(chunk.getHighlightValue());
//            chunksBean.setIsReverseVideo(chunk.isReverseVideo());
//            chunksBean.setLightValue(chunk.getLightValue());
//            chunksBean.setMinVideoDuration(chunk.getMinVideoDuration());
//            chunksBean.setRotateTransform(StringUtil.stringFromMatrix4f(chunk.getRotateTransform()));
//            chunksBean.setFillTransform(StringUtil.stringFromMatrix4f(chunk.getFillTransform()));
//            if (chunk.getRotateType() == null) {
//                chunk.setRotateType(VideoRotateType.VideoRotateTypeNone);
//            }
//            chunksBean.setRotateType(chunk.getRotateType().getValue());
//            chunksBean.setSaturabilityValue(chunk.getSaturabilityValue());
//            if (chunk.getScreenType() == null) {
//                chunk.setScreenType(ChunkScreenActionType.ChunkScreenActionType_None);
//            }
//            chunksBean.setScreenType(chunk.getScreenType().getValue());
//            chunksBean.setShadowValue(chunk.getShadowValue());
//            chunksBean.setTimePoint(chunk.getTimePoint());
//            chunksBean.setDuration(chunk.getDuration());
//            chunksBean.setVideoIndex(chunk.getVideoIndex());
//            chunksBean.setChunkType(chunk.getChunkType().getValue());
//            chunksBeans.add(chunksBean);
//        }
//        jsonBean.setChunks(chunksBeans);
//        jsonBean.setModules(this.modulesBeans);
//        //资源resource
//        Gson gson = new Gson();
//        return gson.toJson(jsonBean);
//    }

//    private void addSpeedData(ScriptJsonBean jsonBean) {
//        ArrayList<ScriptJsonBean.SpeedPointsBean> pointsBeans = new ArrayList<>();
//
//        for (TimeScaleModel model : this.getSpeedPoints()) {
//            ScriptJsonBean.SpeedPointsBean speedPointsBean = new ScriptJsonBean.SpeedPointsBean();
//            speedPointsBean.setSpeedScale(1 / model.getSpeedScale());
//            speedPointsBean.setTimePosition(StringUtil.stringFromCMTime(model.getTimePosition()));
//            pointsBeans.add(speedPointsBean);
//        }
//        //添加最后末尾的点 制作完整脚本
//        if (!pointsBeans.isEmpty()) {
//            TimeScaleModel model = this.getSpeedPoints().get(this.getSpeedPoints().size() - 1);
//            ScriptJsonBean.SpeedPointsBean speedPointsBean = new ScriptJsonBean.SpeedPointsBean();
//            speedPointsBean.setSpeedScale(1 / model.getSpeedScale());
//            speedPointsBean.setTimePosition(StringUtil.stringFromCMTime(new CMTime(this.getVideosTotalDuration())));
//            pointsBeans.add(speedPointsBean);
//        }
//        //大于2  将里面的数据变速值后移以为，第一个速度为1
//        if (pointsBeans.size() > 2) {
//            for (int i = pointsBeans.size() - 1; i >= 0; i--) {
//                float scale = (i == 0) ? 1f : (pointsBeans.get(i - 1).getSpeedScale());
//                pointsBeans.get(i).setSpeedScale(scale);
//            }
//        }
//        jsonBean.setSpeedPoints(pointsBeans);
//    }


    public void addSpeedPointFromDraf(TimeScaleModel point) {
        speedPoints.add(point);
    }

    /**
     * 慢放相关
     */
    public void addSpeedPoint(final TimeScaleModel point) {
        this.speedPoints.add(point);
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.getSpeedPoints().add(point.timeScaleModelVo());
//                    realm.insertOrUpdate(projectVo);
//
//                }
//            });
        }
    }

    public void insertSpeedPoint(final TimeScaleModel model, final int atIndex) {
        this.speedPoints.add(atIndex, model);
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//
//                    projectVo.getSpeedPoints().add(atIndex, model.timeScaleModelVo());
//                    realm.insertOrUpdate(projectVo);
//
//                }
//            });
        }
    }

    public void replaceSpeedPointAtIndex(int index, TimeScaleModel newPoint) {
        TimeScaleModel model = this.speedPoints.get(index);
        model.setTimePosition(newPoint.getTimePosition());
        model.setSpeedScale(newPoint.getSpeedScale());
    }

    public void updateSpeedPointAtIndex(final int index, final float speedScale) {
        TimeScaleModel originModel = this.speedPoints.get(index);
        originModel.setSpeedScale(speedScale);

        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    TimeScaleModelVo timeScaleModelVo = projectVo.getSpeedPoints().get(index);
//                    timeScaleModelVo.setSpeedScale(speedScale);
//                    realm.insertOrUpdate(projectVo);
//                }
//            });
        }
    }

    public void updateSpeedPointAtIndex(final int index, final CMTime timePosition) {
        TimeScaleModel originModel = this.speedPoints.get(index);
        originModel.setTimePosition(timePosition);

        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    TimeScaleModelVo timeScaleModelVo = projectVo.getSpeedPoints().get(index);
//                    timeScaleModelVo.setTimePosition(timePosition.timeVo());
//                    realm.insertOrUpdate(projectVo);
//                }
//            });
        }
    }

    public void removeSpeedPoint(final TimeScaleModel model) {
        if (speedPoints.contains(model)) {
            speedPoints.remove(model);
        }

        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    for (TimeScaleModelVo timeScaleModelVo : projectVo.getSpeedPoints()) {
//                        if (CMTime.compare(timeScaleModelVo.getTimePosition().cmTime(), model.getTimePosition()) == 0) {
//                            projectVo.getSpeedPoints().remove(timeScaleModelVo);
//                            break;
//                        }
//                    }
//                    realm.insertOrUpdate(projectVo);
//                }
//            });
        }
    }

    public void removeSpeedPointAtIndex(final int index) {
        if (index < speedPoints.size()) {
            speedPoints.remove(index);
        }
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.getSpeedPoints().remove(index);
//                    realm.insertOrUpdate(projectVo);
//                }
//            });
        }
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(final int version) {
        this.version = version;
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.setVersion(version);
//                }
//            });
        }
    }

    public void removeAllSpeedPoints() {
        this.speedPoints.clear();
        if (needSave) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.getSpeedPoints().clear();
//                }
//            });
        }
    }

    public void setNeedSave(boolean needSave) {
        this.needSave = needSave;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

//    public ArrayList<ScriptJsonBean.ModulesBean> getModulesBeans() {
//        return modulesBeans;
//    }
//
//    public void setModulesBeans(ArrayList<ScriptJsonBean.ModulesBean> modulesBeans) {
//        this.modulesBeans = modulesBeans;
//    }

    /**
     * 添加录音model
     *
     * @param recodeModel
     */
    public void addRecodeModel(RecodeModel recodeModel) {
        if (recodeModel != null)
            recodeModels.add(recodeModel);
    }

    /**
     * 添加音频
     *
     * @param audioChunk
     */
    public void addAudioChunk(final AudioChunk audioChunk) {
        if (audioChunks == null || audioChunk == null) return;
        audioChunks.add(audioChunk);
        if (needSave && audioChunk.getAudioChunkType() != TrackType.TrackType_Main_Audio) {
//            DBManage.getInstance().getDefaultRealm().executeTransaction(new Realm.Transaction() {
//                @Override
//                public void execute(Realm realm) {
//                    projectVo.getAudioChunkVos().add(audioChunk.getAudioChunkVo());
//                }
//            });
        }
    }

    /**
     * 添加音频
     *
     * @param path      音频路径
     * @param startTime 开始时间
     * @param trackType 音频类型
     */
    public void addAudioChunk(String path, String chunkId, CMTime startTime, TrackType trackType) {
        if (TextUtils.isEmpty(path) || startTime == null) return;
        AudioChunk audioChunk = new AudioChunk(path, chunkId, mContext, trackType, needSave);
        if (!audioChunk.isAudioPrepare()) return;
        audioChunk.setInsertTime(startTime);
        addAudioChunk(audioChunk);
    }

    /**
     * 添加音频
     *
     * @param path
     * @param chunkEditTimeRange 可播放区间
     * @param trackType
     */
    public void addAudioChunk(String path, String chunkId, CMTime startTime, CMTimeRange chunkEditTimeRange, TrackType trackType) {
        if (TextUtils.isEmpty(path) || chunkEditTimeRange == null) return;
        AudioChunk audioChunk = new AudioChunk(path, chunkId, mContext, trackType, needSave);
        if (!audioChunk.isAudioPrepare()) return;
        audioChunk.setInsertTime(startTime);
        audioChunk.setChunkEditTimeRange(chunkEditTimeRange);
        addAudioChunk(audioChunk);
    }

    /**
     * 移除 当前 视频中的原音
     */
    public void removeMainAudio() {
        if (audioChunks == null || audioChunks.isEmpty()) return;
        ArrayList<AudioChunk> removeAudio = new ArrayList<>();
        for (AudioChunk audioChunk : audioChunks) {
            if (audioChunk.getAudioChunkType() == TrackType.TrackType_Main_Audio)
                removeAudio.add(audioChunk);
        }
        if (removeAudio.isEmpty()) return;
        for (AudioChunk audioChunk : removeAudio) {
            audioChunks.remove(audioChunk);
        }
        removeAudio.clear();
        removeAudio = null;
    }
}
