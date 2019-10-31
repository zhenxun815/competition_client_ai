package com.tqhy.client_ai.task;

import com.google.gson.Gson;
import com.tqhy.client_ai.config.Constants;
import com.tqhy.client_ai.models.entity.Case;
import com.tqhy.client_ai.models.msg.server.ClientMsg;
import com.tqhy.client_ai.network.Network;
import com.tqhy.client_ai.utils.*;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import lombok.*;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yiheng
 * @create 4/1/2019
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor(staticName = "with")
public class DcmTransWorkerTask extends Task {

    public static final String PROGRESS_MSG_ERROR = "error";
    public static final String PROGRESS_MSG_COMPLETE = "complete";
    public static final String PROGRESS_MSG_TRANS = "trans";
    public static final String PROGRESS_MSG_COLLECT = "collect";

    Logger logger = LoggerFactory.getLogger(DcmTransWorkerTask.class);
    BooleanProperty stopUploadFlag = new SimpleBooleanProperty(false);

    @NonNull
    File dirToUpload;

    @NonNull
    String localDataPath;

    @NonNull
    String batchNumber;

    /**
     * 上传成功文件数
     */

    /**
     * 本次上传任务信息记录文件
     */
    File uploadInfoFile;
    private List<Case> originCases;
    private File jpgDir;
    private int completeCount;
    private int total2Transform;

    @Override
    protected Object call() throws Exception {
        logger.info("start upload task...");

        List<Case> cases = prepareTask();
        String caseJson = new Gson().toJson(cases);
        String uploadMsg = PROGRESS_MSG_COMPLETE + ";" + caseJson;
        updateMessage(uploadMsg);
        return null;
    }

    private List<Case> prepareTask() {
        uploadInfoFile = FileUtils.getLocalFile(localDataPath, batchNumber + ".txt");
        String jpgDirPath = PropertyUtils.getProperty("jpgDir");
        this.jpgDir = new File(jpgDirPath);
        if (this.jpgDir.exists()) {
            FileUtils.deleteDir(this.jpgDir);
        }
        this.jpgDir.mkdirs();

        originCases = collectAll(dirToUpload);
        return originCases;
    }


    /**
     * 删除生成的临时jpg文件
     */
    private void deleteTempFiles() {
        FileUtils.deleteDir(jpgDir);
    }


    /**
     * 收集全部文件信息并更新进度条状态
     *
     * @param dirToUpload
     * @return
     */
    private ArrayList<Case> collectAll(File dirToUpload) {
        File[] caseDirs = dirToUpload.listFiles(File::isDirectory);
        ArrayList<Case> cases = new ArrayList<>();

        for (File caseDir : caseDirs) {
            File[] dcmFiles = caseDir.listFiles();
            completeCount = 0;
            total2Transform = dcmFiles.length;
            String caseDirName = caseDir.getName();
            File destJpgDir = new File(jpgDir, caseDirName);
            if (!destJpgDir.exists()) {
                destJpgDir.mkdirs();
            }
            logger.info("start convert case dir #{}#", caseDirName);
            for (File dcmFile : dcmFiles) {
                File jpgFile = Dcm2JpgUtil.convert(dcmFile, destJpgDir);

                HashMap<String, String> requestParamMap = new HashMap<>();
                requestParamMap.put("caseName", caseDirName);
                if (null != jpgFile) {
                    logger.info("convert {} complete...", jpgFile.getAbsolutePath());
                    Map<String, RequestBody> requestMap = NetworkUtils.createRequestParamMap(requestParamMap);
                    //OriginData originData = FileUtils.getOriginData(jpgFile);
                    doUpLoad(requestMap, jpgFile);
                    //OriginData originData = OriginData.of("tt", 0, 0, "pp");
                    //originDatas.add(originData);
                }
            }
            logger.info("convert case dir #{}# complete...", caseDirName);
            //cases.add(Case.of(caseId, caseDirName, originDatas));
        }

        return cases;
    }


    /**
     * 更新图片格式转换进度状态
     */
    private void updateTransImgStatus(int completeCount, int total2Transform) {
        double progress = (completeCount + 0D) / total2Transform * 100;
        String uploadMsg = PROGRESS_MSG_TRANS + ";progress;" + progress;
        updateMessage(uploadMsg);
        updateProgress(completeCount, total2Transform);
    }

    private void doUpLoad(Map<String, RequestBody> requestParamMap, File fileToUpload) {

        logger.info("start upload file: " + fileToUpload.getAbsolutePath());
        if (shouldStop()) {
            logger.info("do upload should stop...");
            return;
        }
        MultipartBody.Part filePart = NetworkUtils.createFilePart("file", fileToUpload.getAbsolutePath());
        Network.getAicApi().uploadFiles(requestParamMap, filePart)
               .observeOn(Schedulers.io())
               .subscribeOn(Schedulers.io())
               .blockingSubscribe(new UploadObserver(fileToUpload));
    }

    private boolean shouldStop() {
        if (stopUploadFlag.get()) {
            logger.info("should stop..");
            return true;
        }
        return false;
    }

    private class UploadObserver implements Observer<ResponseBody> {
        Disposable d;
        File fileToUpload;

        public UploadObserver(File fileToUpload) {
            this.fileToUpload = fileToUpload;
        }

        @Override
        public void onSubscribe(Disposable d) {
            logger.info("on subscribe Disposable: " + d);
            if (shouldStop()) {
                logger.info("on subscribe should stop {}", d);
                d.dispose();
            }
            this.d = d;
        }

        @Override
        public void onNext(ResponseBody responseBody) {
            ClientMsg clientMsg = GsonUtils.parseResponseToObj(responseBody);
            Integer flag = clientMsg.getFlag();

            logger.info("upload onnext flag is {}", flag);
            if (2 == flag) {
                List<String> msgs = clientMsg.getMsg();

                String failInfo = StringUtils.join(msgs, ",",
                                                   msg -> "已存在".equals(msg) ? ";1" : msg);
                String resMsg = fileToUpload.getAbsolutePath().concat(failInfo);
                logger.info("server get file fail...{}", resMsg);
                FileUtils.appendFile(uploadInfoFile, resMsg,
                                     builder -> builder.append(Constants.NEW_LINE), true);
            }
        }

        @Override
        public void onError(Throwable e) {
            logger.error("upload " + fileToUpload.getAbsolutePath() + " failed", e);
            completeCount++;
            updateTransImgStatus(completeCount, total2Transform);
            FileUtils.appendFile(uploadInfoFile, fileToUpload.getAbsolutePath(),
                                 builder -> builder.append(Constants.NEW_LINE), true);
        }

        @Override
        public void onComplete() {
            completeCount++;
            updateTransImgStatus(completeCount, total2Transform);
        }
    }

}
