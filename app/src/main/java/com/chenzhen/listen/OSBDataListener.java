package com.chenzhen.listen;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import com.chenzhen.pojo.OSBData;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
;
import java.util.List;

@Slf4j
public class OSBDataListener implements ReadListener<OSBData> {

    /**
     * 每隔100条存储数据库，然后清理list,内存回收
     */
    private static final int BATCH_COUNT = 100;

    /**
     * 缓存的数据
     */
    private List<OSBData> cacheDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

    /**
     * 这个每一条数据解析都会来调用
     * @param osbData
     * @param analysisContext
     */
    @Override
    public void invoke(OSBData osbData, AnalysisContext analysisContext) {
        log.info("解析到一条数据：{}", new Gson().toJson(osbData));
        cacheDataList.add(osbData);
        // 达到BATCH_COUNT，需要去存储一次数据库
        if(cacheDataList.size() >= BATCH_COUNT) {
            saveData();
            cacheDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
        }
    }


    /**
     * 所有数据解析完成调用
     * @param analysisContext
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        saveData();
        log.info("数据解析完成");
    }

    /**
     * 存储数据库
     */
    private void saveData() {
    }
}
