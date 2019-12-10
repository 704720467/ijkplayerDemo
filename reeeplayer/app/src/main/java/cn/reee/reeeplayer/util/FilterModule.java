package cn.reee.reeeplayer.util;

import android.content.Context;
import android.text.TextUtils;


import com.zp.libvideoedit.modle.FilterCateModel;
import com.zp.libvideoedit.modle.FilterModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.reee.reeeplayer.util.plist.PListXMLHandler;
import cn.reee.reeeplayer.util.plist.PListXMLParser;
import cn.reee.reeeplayer.util.plist.domain.Array;
import cn.reee.reeeplayer.util.plist.domain.Dict;
import cn.reee.reeeplayer.util.plist.domain.PList;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by qin on 2019/6/13.
 */

public class FilterModule {
//
//    private Context context;
//
//    public FilterModule(Context context) {
//        this.context = context;
//    }
//
//
//    public void initFilters() {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                AssetsCopyTOSDcard assetsCopyTOSDcard = new AssetsCopyTOSDcard(context);
//                for (int i = 0; i <= 55; i++) {
//                    String sdPath = Constant.TEMP_FILTER_PATH + "/" + i + ".png";
//                    if (new File(sdPath).exists()) {
//                        continue;
//                    }
//                    String assetpath = "lut/" + i + ".png";
//                    assetsCopyTOSDcard.assetToSD(assetpath, sdPath);
//                }
//
//                initLocalFilters();
//
//                loadNetFilter();
//            }
//        }).start();
//
//    }
//
//    private void initLocalFilters() {
//
//
//        List<FilterCateModel> filterCateModels = new ArrayList<>();
//
//        PListXMLParser parser = new PListXMLParser(); // 基于SAX的实现
//        PListXMLHandler handler = new PListXMLHandler();
//        parser.setHandler(handler);
//        try {
//            // waiter.plist是你要解析的文件，该文件需放在assets文件夹下
//            parser.parse(context.getAssets().open("lut/list.plist"));
//        } catch (IllegalStateException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        PList actualPList = ((PListXMLHandler) parser.getHandler()).getPlist();
//        Array rootElement = (Array) actualPList.getRootElement();
//        for (int i = 0; i < rootElement.size(); i++) {
//            Dict dict = (Dict) rootElement.get(i);
//            Map cateFilters = dict.getConfigMap();
//            Array filters = (Array) cateFilters.get("filters");
//            cn.reee.reeeplayer.util.plist.domain.String catgory = ( cn.reee.reeeplayer.util.plist.domain.String) cateFilters.get("title");
//            String title = catgory.getValue();
//            ArrayList<FilterModel> filterModels = new ArrayList<FilterModel>();
//            for (int j = 0; j < filters.size(); j++) {
//                Dict filterDict = (Dict) filters.get(j);
//                String file = filterDict.getConfiguration("file").getValue();
//                String titleName = filterDict.getConfiguration("name").getValue();
//                FilterModel filterModel = new FilterModel();
//                filterModel.setName(titleName);
//                filterModel.setUrl(file);
//                filterModels.add(filterModel);
//            }
//            filterCateModels.add(new FilterCateModel(title, filterModels));
//        }
//
//
//        NetFiltersBean netFiltersBean2 = new NetFiltersBean();
//        netFiltersBean2.setFilters(filterCateModels);
//        BusinessCacheUtil.saveNetFilters(netFiltersBean2);
//
//    }
//
//    private String getFilterLocalPath(String url) {
//        String path = Constant.TEMP_FILTER_PATH + url.substring(url.lastIndexOf("/"));
//        return path;
//    }
//
//    private void downFilterImg(final List<String> downUrl) {
//
//        final FileDownloadListener queueTarget = new FileDownloadListener() {
//            @Override
//            protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
//            }
//
//            @Override
//            protected void connected(BaseDownloadTask task, String etag,
//                                     boolean isContinue, int soFarBytes, int totalBytes) {
//            }
//
//            @Override
//            protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
//            }
//
//            @Override
//            protected void blockComplete(BaseDownloadTask task) {
//            }
//
//            @Override
//            protected void retry(final BaseDownloadTask task, final Throwable ex,
//                                 final int retryingTimes, final int soFarBytes) {
//            }
//
//            @Override
//            protected void completed(BaseDownloadTask task) {
//                com.vnision.videostudio.util.LogUtil.e("滤镜下载", "全部下载完成:" + task.getTag());
//            }
//
//            @Override
//            protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
//            }
//
//            @Override
//            protected void error(BaseDownloadTask task, Throwable e) {
//
//            }
//
//            @Override
//            protected void warn(BaseDownloadTask task) {
//            }
//        };
//
//        final FileDownloadQueueSet queueSet = new FileDownloadQueueSet(queueTarget);
//
//        final List<BaseDownloadTask> tasks = new ArrayList<>();
//        for (int i = 0; i < downUrl.size(); i++) {
//            tasks.add(FileDownloader.getImpl()
//                    .create(downUrl.get(i))
//                    .setTag(i + 1)
//                    .setPath(getFilterLocalPath(downUrl.get(i))));
//        }
//
//        queueSet.disableCallbackProgressTimes();
//
//        // 所有任务在下载失败的时候都自动重试一次
//        queueSet.setAutoRetryTimes(1);
//        // 并行执行该任务队列
//        queueSet.downloadTogether(tasks);
//
//        //主动调用start方法来启动该Queue
//        queueSet.start();
//    }
//
//    private void loadNetFilter() {
//        ApiService apiService = HttpManager.getInstance().getApiService();
//        apiService.netPackages(8)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new rx.Observer<NetFiltersBean>() {
//                    @Override
//                    public void onCompleted() {
//
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//                        EventBus.getDefault().post("updateLookup");
//                    }
//
//                    @Override
//                    public void onNext(NetFiltersBean netFiltersBean) {
//                        ArrayList<FilterModel> filters = new ArrayList<>();
//                        String title = context.getResources().getString(R.string.filter_no1);
//                        FilterModel filterModel = new FilterModel();
//                        filterModel.setTitle(title);
//                        filterModel.setName(context.getResources().getString(R.string.filter_no));
//                        filters.add(filterModel);
//                        FilterCateModel filterCateModel1 = new FilterCateModel(title, filters);
//                        List<FilterCateModel> filterCateModelList = netFiltersBean.getFilters();
//                        filterCateModelList.add(0, filterCateModel1);
//
//                        BusinessCacheUtil.saveNetFilters(netFiltersBean);
//
//                        List<String> toLoadUrls = new ArrayList<>();
//                        for (FilterCateModel filterCateModel : netFiltersBean.getFilters()) {
//                            if (TextUtils.equals(filterCateModel.getName(), title)) {
//                                continue;
//                            }
//                            ArrayList<com.vnision.VNICore.Model.FilterModel> filterModels = filterCateModel.getFilters();
//                            for (com.vnision.VNICore.Model.FilterModel filterModel1 : filterModels) {
//                                String path = Constant.TEMP_FILTER_PATH + "/" + filterModel1.getUrl();
//                                if (new File(path).exists()) {
//                                    continue;
//                                }
//                                toLoadUrls.add(Urls.getImageFullUrl(filterModel1.getUrl()));
//                            }
//                        }
//
//                        if (toLoadUrls.size() > 0) {
//                            downFilterImg(toLoadUrls);
//                        }
//
//                        LookupInstance.update();
//
//                        EventBus.getDefault().post("updateLookup");
//
//                    }
//                });
//    }
}
