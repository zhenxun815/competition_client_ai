import com.tqhy.client_ai.models.msg.server.ClientMsg;
import com.tqhy.client_ai.network.Network;
import com.tqhy.client_ai.utils.FileUtils;
import com.tqhy.client_ai.utils.GsonUtils;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

/**
 * @author Yiheng
 * @create 1/29/2019
 * @since 1.0.0
 */

public class UnitTests {

    Logger logger = LoggerFactory.getLogger(UnitTests.class);

    @Test
    public void testOther() {
        String str = "中文";
        byte[] bytes = str.getBytes();
        logger.info("length is {}", bytes.length);
    }


    @Test
    public void testFileUtils() {
       /* String imgPath = "C:\\Users\\qing\\Pictures\\shadow\\error\\test2\\13.jpeg";
        File imgFile = new File(imgPath);
        boolean isJpgFile = FileUtils.isJpgFile(imgFile);
        logger.info("is jpg file {}", isJpgFile);*/

        String dirPath = "C:\\Users\\qing\\Desktop\\追加数据";
        File dir = new File(dirPath);
        HashMap<File, String> filesMapInRootDir = FileUtils.getFilesMapInRootDir(dir, file -> FileUtils.isDcmFile(
                file) || FileUtils.isJpgFile(file));
        HashMap<File, String> tempTotalFile = new HashMap<>();
        tempTotalFile.putAll(filesMapInRootDir);
        logger.info("files {}", tempTotalFile.size());
    }


    @Test
    public void testNet() {
        try {
            ResponseBody responseBody = Network.getAicApi()
                                               .delSubject()
                                               .execute()
                                               .body();
            ClientMsg clientMsg = GsonUtils.parseResponseToObj(responseBody);
            logger.info("client msg {}", clientMsg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDownLoad() {
        String downloadUrl = "/home/tqhy/aic/file/15cc97aaa6964fc5b6caf00020d575a4/20190517174054644/b798abe6e1b1318ee36b0dcb3fb9e4d3/fd9c69eb905feede1a7fc2fdfcf0fbdb.jpg";

        Network.getAicApi()
               .download(downloadUrl)
               //.observeOn(Schedulers.io())
               //.subscribeOn(Schedulers.io())
               .subscribe(response -> {
                   String header = response.headers().get("Content-Disposition");
                   logger.info("header is {}", header);
                   String[] split = header.split("filename=");
                   String fileName = split[1];
                   File file = new File("C:\\Users\\qing\\Pictures\\shadow", fileName);

                   BufferedSink sink = null;
                   try {
                       sink = Okio.buffer(Okio.sink(file));
                       sink.writeAll(response.body().source());
                       sink.close();
                   } catch (FileNotFoundException e) {
                       e.printStackTrace();
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
               });
        //logger.info("response is: {}", response.string());
    }


    public static void main(String[] args) {
        try {

            File file = new File("F:\\dicom\\ct\\lung 2.5mm", "Image001.dcm");
            Optional<File> toJpg = FileUtils.transToJpg(file, new File("F:\\dicom\\ct\\lung 2.5mm"));
            File jpg = toJpg.get();
            System.out.println(jpg.getAbsolutePath());
            ImageReader reader = ImageIO.getImageReadersByFormatName("JPEG").next();
            String formatName = reader.getFormatName();
            System.out.println("reader format name is " + formatName);
            ImageInputStream imageInputStream = ImageIO.createImageInputStream(jpg);
            reader.setInput(imageInputStream);
            int imgWidth = reader.getWidth(0);
            int imgHeight = reader.getHeight(0);
            System.out.println("width " + imgWidth + " height " + imgHeight);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
