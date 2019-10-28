package com.tqhy.client_ai.utils;

import com.tqhy.client_ai.models.entity.OriginData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Yiheng
 * @create 4/2/2019
 * @since 1.0.0
 */
public class FileUtils {


    static Logger logger = LoggerFactory.getLogger(FileUtils.class);
    protected static char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * 生成病例名,若为根目录下文件则以文件名为病例名,否则以文件夹名作为病例名
     *
     * @param file
     * @return
     */
    public static String generateCaseName(File file) {
        File parentFile = file.getParentFile();
        String parentDirName = parentFile.getName();
        logger.info("parent file is {}", parentFile.getName());
        return PrimaryKeyUtil.getMd5(parentDirName);
    }

    /**
     * 获取文件夹下所有文件
     *
     * @param dir
     * @return
     */
    public static HashMap<File, String> getFilesMapInRootDir(File dir, Predicate<File> filter) {
        //logger.info("into get file map in dir...");
        File[] files = dir.listFiles();

        return Arrays.stream(files)
                     .filter(File::isFile)
                     .filter(file -> filter.test(file))
                     .collect(HashMap::new,
                              (map, file) -> {
                                  map.put(file, generateCaseName(file));
                                  //logger.info("add file map key: {}, value: {}", file.getAbsolutePath(), name);
                              },
                              HashMap::putAll);
    }

    /**
     * 获取子文件下所有文件
     *
     * @param dir
     * @return
     */
    public static HashMap<File, String> getFilesMapInSubDir(File jpgDir, File dir, Predicate<File> filter,
                                                            File rootDir) {
        //logger.info("into get file map in sub dir...");


        File[] files = dir.listFiles();
        if (null == files || files.length == 0) {
            return new HashMap<>();
        }
        return Arrays.stream(files)
                     .collect(HashMap::new,
                              (map, file) -> {
                                  if (filter.test(file)) {
                                      String caseId = generateCaseName(file);
                                      File jpgCaseDir = new File(jpgDir, caseId);

                                      if (!jpgCaseDir.exists()) {
                                          jpgCaseDir.mkdirs();
                                      }
                                      Optional<File> jpgFileOpt = transToJpg(file, jpgCaseDir);
                                      map.put(file, caseId);
                                  }
                              },
                              HashMap::putAll);
    }


    public static Optional<File> transToJpg(File fileToTrans, File jpgDir) {
        File jpgFile = Dcm2JpgUtil.convert(fileToTrans, jpgDir);
        logger.info("file to trans complete {}", fileToTrans.getAbsolutePath());
        return Optional.ofNullable(jpgFile);
    }


    /**
     * 根绝后缀名判断是否jpg文件
     *
     * @param fileToJudge
     * @return
     */
    public static boolean isJpgFile(File fileToJudge) {
        logger.info("into judge file is jpg...");
        String fileName = fileToJudge.getName().toLowerCase();
        if (!(fileName.endsWith("jpg") || fileName.endsWith("jpeg"))) {
            return false;
        }

        try {
            ImageInputStream iis = ImageIO.createImageInputStream(fileToJudge);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            while (readers.hasNext()) {
                ImageReader reader = readers.next();
                String formatName = reader.getFormatName();
                if ("jpeg".equals(formatName.toLowerCase())) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 判断文件是否是DCM文件
     *
     * @param fileToJudge
     * @return
     */
    public static boolean isDcmFile(File fileToJudge) {
        //logger.info("into judge file is dcm...");
        if (fileToJudge.isDirectory()) {
            return false;
        }
        byte[] bytes = new byte[132];
        try (FileInputStream in = new FileInputStream(fileToJudge)) {
            int len = readAvailable(in, bytes, 0, 132);
            return 132 == len && bytes[128] == 'D' && bytes[129] == 'I' && bytes[130] == 'C' && bytes[131] == 'M';
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 判断是否可读取指定长度信息
     *
     * @param in
     * @param b   要读取的字节数组
     * @param off 开始位置偏移量
     * @param len 读取最大长度
     * @return 读取到长度
     * @throws IOException
     */
    public static int readAvailable(InputStream in, byte b[], int off, int len) throws IOException {
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }
        int wpos = off;
        while (len > 0) {
            int count = in.read(b, wpos, len);
            if (count < 0) {
                break;
            }
            wpos += count;
            len -= count;
        }
        return wpos - off;
    }

    /**
     * 删除文件夹
     *
     * @param temp
     * @return
     */
    public static void deleteDir(File temp) {
        logger.info("into delete");
        try {
            org.apache.tomcat.util.http.fileupload.FileUtils.deleteDirectory(temp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 按行读取文件,返回一个由每行处理结果对象组成的集合
     *
     * @param file     待读行文件
     * @param function 对每一行内容处理的{@link Function <String,T> Function}
     * @param <T>      返回集合泛型
     * @return
     */
    public static <T> List<T> readLine(File file, Function<String, T> function) {
        ArrayList<T> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                T apply = function.apply(line);
                if (null != apply) {
                    list.add(apply);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }


    /**
     * 写文件
     *
     * @param file
     * @param info     待写入信息
     * @param function 处理写入内容,为null则不做任何处理
     * @param create   当文件不存在时是否创建新文件
     * @param append   是否追加写入
     */
    public static void writeFile(File file, String info, @Nullable Function<StringBuilder, StringBuilder> function,
                                 boolean create, boolean append) {

        if (create && !file.exists()) {
            createNewFile(file);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, append))) {
            if (null == function) {
                writer.write(info);
            } else {
                String apply = function.apply(new StringBuilder(info))
                                       .toString();
                writer.write(apply);
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 不追加写入,重写文件,覆盖已有内容
     *
     * @param file
     * @param info     待写入信息
     * @param function 处理写入内容,为null则不做任何处理
     * @param create   当文件不存在时是否创建新文件
     */
    public static void writeFile(File file, String info, @Nullable Function<StringBuilder, StringBuilder> function,
                                 boolean create) {
        writeFile(file, info, function, create, false);
    }

    /**
     * 向文件追加内容,内部调用{@link FileUtils#writeFile(File, String, Function, boolean, boolean) writeFile} 方法
     *
     * @param file
     * @param info     待写入信息
     * @param consumer 处理写入内容,为null则不做任何处理
     * @param create   当文件不存在时是否创建新文件
     */
    public static void appendFile(File file, String info, @Nullable Function<StringBuilder, StringBuilder> consumer,
                                  boolean create) {
        writeFile(file, info, consumer, create, true);
    }


    /**
     * 创建新文件
     *
     * @param file
     * @return
     */
    public static boolean createNewFile(File file) {
        if (file.exists()) {
            file.delete();
        }

        try {
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            boolean newFile = file.createNewFile();
            return newFile;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 获取项目所在路径
     *
     * @return
     */
    public static String getAppPath() {
        String jarPath = FileUtils.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        int end = jarPath.lastIndexOf("/");
        String appPath = jarPath.substring(1, end);
        //logger.info("appPath is: "+appPath);
        return appPath;
    }

    /**
     * 获取相对jar包所在文件夹路径的{@link File File}对象
     *
     * @param relativePath 相对jar包所在文件夹相对路径.以<em>"/"<em/>开头
     * @param fileName     文件名
     * @return
     */
    public static File getLocalFile(String relativePath, String fileName) {
        String rootPath = getAppPath();
        String localFilePath = rootPath + relativePath;

        logger.info("localFilePath is: " + localFilePath);
        File localFile = new File(localFilePath, fileName);
        return localFile;
    }

    /**
     * 如果目标文件存在则替换之
     *
     * @param sourceFile
     * @param destFile
     * @return
     */
    public static boolean copyFile(File sourceFile, File destFile) {

        logger.info("source file: " + sourceFile.getAbsolutePath() + ", dest file: " + destFile.getAbsolutePath());
        if (!sourceFile.exists()) {
            return false;
        }
        if (destFile.exists()) {
            boolean delete = destFile.delete();
            if (!delete) {
                return false;
            }
        }

        if (createNewFile(destFile)) {
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile))) {
                try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destFile))) {
                    byte[] buffer = new byte[1024 * 8];
                    int i = 0;
                    while ((i = bis.read(buffer)) != -1) {
                        bos.write(buffer);
                    }
                    bos.flush();
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 复制jar包内资源文件到jar包外
     *
     * @param resourceName
     * @param destination
     * @return
     */
    public static boolean copyResource(String resourceName, String destination) {
        boolean succeess = true;
        InputStream resourceStream = FileUtils.class.getResourceAsStream(resourceName);
        logger.info("Copying ->" + resourceName + " to ->" + destination);

        try {
            Files.copy(resourceStream, Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            logger.error("copy resource error!!", ex);
            succeess = false;
        }

        return succeess;

    }

    /**
     * 文件集合生成文件名为key与文件绝对路径为value的map
     *
     * @param files
     * @return
     */
    public static Map<String, String> getFilesInfoMap(List<File> files) {
        HashMap<String, String> collect = files.stream()
                                               .collect(HashMap::new, (map, file) -> {
                                                   String[] fileNameSplit = file.getName().split("\\.");
                                                   map.put(fileNameSplit[0], file.getAbsolutePath());
                                               }, HashMap::putAll);
        return collect;
    }

    public static OriginData getOriginData(File file) {

        String imageAbsPath = file.getAbsolutePath().replaceAll("\\\\", "/");
        logger.info("start get origin data {}", imageAbsPath);

        String appPath = FileUtils.getAppPath();
        String imageRelPath = imageAbsPath.replace(appPath, "");
        //logger.info("appPath is {}", appPath);
        //logger.info("imageRelPath is {}", imageRelPath);
        String imgId = getFileMD5(file);
        logger.info("get imgId {}", imgId);
        try {
            ImageReader reader = ImageIO.getImageReadersByFormatName("JPEG").next();
            ImageInputStream imageInputStream = ImageIO.createImageInputStream(file);
            reader.setInput(imageInputStream);
            int imgWidth = reader.getWidth(0);
            int imgHeight = reader.getHeight(0);
            logger.info("width {}, height {}", imgWidth, imgHeight);
            return OriginData.of(imgId, imgWidth, imgHeight, imageRelPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getFileMD5(File file) {
        String name = "";

        try (InputStream inputStream = new FileInputStream(file)) {

            byte[] bytes = new byte[1024];
            int len = 0;
            MessageDigest messagedigest = MessageDigest.getInstance("MD5");
            while ((len = inputStream.read(bytes)) > 0) {
                messagedigest.update(bytes, 0, len);
            }
            name = bufferToHex(messagedigest.digest());
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        } catch (NoSuchAlgorithmException e) {
        }
        return name;
    }

    public static String bufferToHex(byte bytes[]) {
        return bufferToHex(bytes, 0, bytes.length);
    }

    private static String bufferToHex(byte bytes[], int m, int n) {
        StringBuffer stringbuffer = new StringBuffer(2 * n);
        int k = m + n;
        for (int l = m; l < k; l++) {
            appendHexPair(bytes[l], stringbuffer);
        }
        return stringbuffer.toString();
    }

    private static void appendHexPair(byte bt, StringBuffer stringbuffer) {
        char c0 = hexDigits[(bt & 0xf0) >> 4];// 取字节中高 4 位的数字转换
        // 为逻辑右移，将符号位一起右移,此处未发现两种符号有何不同
        char c1 = hexDigits[bt & 0xf];// 取字节中低 4 位的数字转换
        stringbuffer.append(c0);
        stringbuffer.append(c1);
    }
}
