import entrance.ProcessUtils;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author cary.shi on 2019/10/16
 */
public class CrawlCpp {
    public static String basePath = "G:\\share\\testcase";

    public static String getToken(String tokenFilePath) {
        File tokenFile = new File(tokenFilePath);
        String token = null;
        try {
            token = FileUtils.readFileToString(tokenFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return token;
    }

    public static boolean getCppRepos(String condition, String storePath, String logPath) {
        String tokenFilePath = "G:\\token\\token.txt";
        String cmd = "curl https://api.github.com/search/repositories?access_token=" + getToken(tokenFilePath) + "&q=" + condition;
        File storeFile = new File(storePath);
        File logFile = new File(logPath);

        try {
            Process process = Runtime.getRuntime().exec(cmd);
            List<String> inputList = ProcessUtils.processMessageToString(process.getInputStream());
            List<String> errorList = ProcessUtils.processMessageToString(process.getErrorStream());

            FileUtils.writeLines(storeFile, inputList, true);
            FileUtils.writeLines(logFile, errorList, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static List<String> resolveJson(String jsonPath) {
        List<String> urls = new ArrayList<>();
        File jsonFile = new File(jsonPath);
        try {
            String content = FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8);
            String[] contentList = content.split("onepageend");
            for (int i = 0; i < contentList.length; i++) {
                JSONObject jsonObject = new JSONObject(contentList[i]);
                JSONArray jsonArray = jsonObject.getJSONArray("items");
                for (int j = 0; j < jsonArray.length(); j++) {
                    JSONObject item = jsonArray.getJSONObject(j);
                    urls.add("https://github.com/" + item.get("full_name") + "/archive/master.zip");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return urls;
    }

    public static void downloadZips(List<String> urls) {
        for (String url : urls) {
            String[] tmps = url.split("/");
            String name = tmps[tmps.length - 4] + "_" + tmps[tmps.length - 3];

            File zipFile = new File("/mnt/share/dataset/" + name + ".zip");
            if (zipFile.exists()) {
                continue;
            }

            String cmd = "wget " + url + " -O " + "/mnt/share/dataset/" + name + ".zip";

            try {
                Process process = Runtime.getRuntime().exec(cmd);
                ProcessUtils.processMessageToNull(process.getInputStream());
                List<String> stringList = ProcessUtils.processMessageToString(process.getErrorStream());
                stringList.forEach(System.out::println);
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        // "topic:android+language:java+created:"
//        getCppRepos("language:cpp&sort=start&order=desc&page=3&per_page=100", basePath + File.separator + "data.json", basePath + File.separator + "log");
//        List<String> urls = resolveJson("G:\\share\\testcase\\data.json");
        List<String> urls = resolveJson("/mnt/share/testcase/data.json");

        ExecutorService executorService = Executors.newCachedThreadPool();
        int threadSize = 8;
        int gap = urls.size() / threadSize;

        for (int i = 0; i < threadSize; i++) {
            int start = i * gap;
            int end = (i + 1) * gap;

            if (i == threadSize - 1) {
                end = urls.size();
            }

            final List<String> singleList = urls.subList(start, end);

            Runnable run = () -> {
                try {
                    downloadZips(singleList);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
            executorService.execute(run);
        }
        executorService.shutdown();
    }
}
