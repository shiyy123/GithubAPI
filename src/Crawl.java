import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.omg.SendingContext.RunTime;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Crawl {

    /**
     * get the attribute total_count
     *
     * @param cmd
     * @return
     */
    private int getCnt(String cmd) {
        Tools.protectRateLimit(false);

        int cnt = 0;
        try {
            Process p = Runtime.getRuntime().exec(cmd);

            StringBuilder res = Tools.getProcessOutput(p.getInputStream(), false);

            p.waitFor();
            p.destroyForcibly();

            JSONObject item = new JSONObject(res.toString());
            cnt = item.getInt("total_count");
        } catch (IOException | InterruptedException | JSONException e) {
            e.printStackTrace();
        }
        return cnt;
    }

    /**
     * Get repos info with condition and store in dataStorePath
     *
     * @param condition     request condition
     * @param dataStorePath data store to
     */
    public void getRepos(String condition, String dataStorePath) {
//        String baseUrl = "curl https://api.github.com/search/repositories?access_token=" + StaticResource.token +"&q=topic:android-app+language:java+created:>2018-03-01";

        String baseUrl = "curl https://api.github.com/search/repositories?access_token=" + StaticResource.token + "&q=" + condition;

        System.out.println(baseUrl);

        try {
            int pageSize = 100;
            int totalNum = getCnt(baseUrl);
            System.out.println("totalNum=" + totalNum);

            int pageNum = totalNum / pageSize + (totalNum % pageSize == 0 ? 0 : 1);
            for (int pageCnt = 0; pageCnt < pageNum; pageCnt++) {
                System.out.println("page=" + pageCnt);
                String cmd = baseUrl + "&sort=stars&order=desc&page=" + pageCnt + "&per_page=" + pageSize;

                Tools.protectRateLimit(false);

                Process p = Runtime.getRuntime().exec(cmd);
                Tools.processMessage(p.getErrorStream(), true);
                Tools.readAndWrite(p.getInputStream(), dataStorePath, "onelineend");

                p.waitFor();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * process the result of method getRepos
     * output: process_repo.json one line is one repo
     *
     * @param dataFromPath
     */
    public void process4getRepos(String dataFromPath, String dataStorePath) {
        try {
//            List<String> lines = FileUtils.readLines(new File(dataStorePath), "utf-8");
            String data = FileUtils.readFileToString(new File(dataFromPath), "utf-8");
            String[] items = data.split("onelineend");
            for (String item : items) {
                JSONObject jsonObject = new JSONObject(item);
                JSONArray array = jsonObject.getJSONArray("items");
                for (int i = 0; i < array.length(); i++) {
                    FileUtils.writeStringToFile(new File(dataStorePath), array.getJSONObject(i).toString(), "utf-8", true);
                    FileUtils.write(new File(dataStorePath), "\n", "utf-8", true);
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get repo name from process_repo.json
     * output: repoName.txt
     *
     * @param dataFromPath
     */
    public void getReposNameFromRepoInfo(String dataFromPath, String dataStorePath) {
        try {
            List<String> jsons = FileUtils.readLines(new File(dataFromPath), "utf-8");
            Set<String> nameList = new HashSet<>();
            for (String json : jsons) {
                JSONObject item = new JSONObject(json);
                String name = item.getString("full_name");
                nameList.add(name);
            }
            FileUtils.writeLines(new File(dataStorePath), nameList, "\n");

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

    }

    /**
     * Get release info
     *
     * @param dataFromPath
     * @param dataStorePath
     */
    public void getReleaseInfo(String dataFromPath, String dataStorePath) {

        try {
            List<String> repos = FileUtils.readLines(new File(dataFromPath), "utf-8");

            int gap = repos.size() / 5;
            ExecutorService executorService = Executors.newCachedThreadPool();

            for (int i = 0; i < 5; i++) {
                int start = i * gap;
                int end = (i + 1) * gap;

                if (i == 4) {
                    end = repos.size();
                }

//                System.out.println("start = " + start);
//                System.out.println("end = " + end);

                final List<String> temp = repos.subList(start, end);
                final int index = i;
                final String storePath = dataStorePath;
                Runnable run = () -> {
                    try {
                        getSplitReleaseInfo(temp, storePath, index);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                };
                executorService.execute(run);
            }
            executorService.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get part release info
     *
     * @param repos
     * @param dataStorePath
     * @param id
     */
    public void getSplitReleaseInfo(List<String> repos, String dataStorePath, int id) {
        try {
            for (String repo : repos) {
                Tools.protectRateLimit(false, StaticResource.getToken(id));

                String baseURL = "curl https://api.github.com/repos/" + repo + "/releases?access_token=" + StaticResource.getToken(id);
                String cmd = baseURL + "&per_page=100";

                Process p = Runtime.getRuntime().exec(cmd);
                Tools.processMessage(p.getErrorStream(), true);

                Tools.readAndWrite(p.getInputStream(), dataStorePath + "/release" + id + ".json", "onereleaseend");

                p.waitFor();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * release(0-4).json
     */
    public void processHalfResOfRelease() {
        String releasePath = StaticResource.basePath + "/release/release";
        String dataStorePath = StaticResource.basePath + "/release/release.json";
        try {
            int cnt = 0;
            for (int i = 0; i < 5; i++) {
                String data = FileUtils.readFileToString(new File(releasePath + i + ".json"), "utf-8");
                String[] releases = data.split("onereleaseend");
                for (String release : releases) {

                    System.out.println(i + release);
                    // && (
                    if (release.contains("API rate limit exceeded for user") || release.contains("Moved Permanently") || release.contains("Not Found")) {
                        continue;
                    } else {
                        JSONArray array = new JSONArray(release);
                        if (array.length() == 0) {
                            continue;
                        } else {
                            cnt++;
                            FileUtils.write(new File(dataStorePath), array.toString(), "utf-8", true);
                            FileUtils.write(new File(dataStorePath), "\n", "utf-8", true);
                        }
                    }
                }
            }
            System.out.println(cnt);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param dataFromPath
     * @param dataStorePath
     */
    public void processReleaseInfo(String dataFromPath, String dataStorePath) {
        try {
            String data = FileUtils.readFileToString(new File(dataFromPath), "utf-8");
            String[] releases = data.split("onereleaseend");
            System.out.println(releases.length);
            int cnt = 0;
            for (int i = 0; i < releases.length; i++) {
                if (releases[i].contains("apk")) {
                    cnt++;
                }
            }
            System.out.println("Have apk=" + cnt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param releasePath
     */
    public void countReleaseHaveApk(String releasePath) {
        try {
            int cnt = 0;
            List<String> lines = FileUtils.readLines(new File(releasePath), "utf-8");
            for (String line : lines) {
                if (line.contains("apk")) {
                    cnt++;
                    FileUtils.write(new File(StaticResource.basePath + "release/apk.json"), line, "utf-8", true);
                    FileUtils.write(new File(StaticResource.basePath + "release/apk.json"), "\n", "utf-8", true);
                }
            }
            System.out.println(cnt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * download apk by apk.json
     *
     * @param apkPath
     */
    public void getApkAndCodeDownloadUrl(String apkPath) {
        try {
            List<String> apks = FileUtils.readLines(new File(apkPath), "utf-8");
            for (String apk : apks) {
                JSONArray array = new JSONArray(apk);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject jsonObject = array.getJSONObject(i);
                    String zipURL = jsonObject.getString("zipball_url");

                    JSONArray assets = jsonObject.getJSONArray("assets");
                    String apkURL = null;
                    for (int j = 0; j < assets.length(); j++) {
                        apkURL = assets.getJSONObject(j).getString("browser_download_url");
                        if (apkURL.endsWith(".apk")) {
                            break;
                        }
                    }

                    if (apkURL != null) {
                        System.out.println(zipURL);
                        System.out.println(apkURL);
                    }
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * download apk and code, one apk and corresponding code in one directory
     *
     * @param sourceCodePath
     */
    public void downloadApkAndCode(String sourceCodePath, int idx) {
        String apkAndCodeFilePath = sourceCodePath + "source_apk.txt";

        try {
            List<String> downloadUrls = FileUtils.readLines(new File(apkAndCodeFilePath), "utf-8");
            int start, end;
            if (idx == 0) {
                start = 0;
                end = downloadUrls.size() / 2;
            } else {
                start = downloadUrls.size() / 2;
                end = downloadUrls.size();
            }

//            System.out.println(start);
//            System.out.println(end);
//
//            System.exit(0);

            ExecutorService executorService = Executors.newCachedThreadPool();
            int threadNum = 16;
            int gap = (end - start) / threadNum;
            for (int i = 0; i < threadNum; i++) {
                int threadBegin = i * gap + start;
                int threadEnd = (i + 1) * gap + start;
                if (i == threadNum - 1) {
                    threadEnd = end;
                }

//                System.out.println(threadBegin);
//                System.out.println(threadEnd);

                final List<String> temp = downloadUrls.subList(threadBegin, threadEnd);
                final int needToAdd = threadBegin;
                final String path = sourceCodePath;
                final int id = i;
                Runnable run = () -> {
                    try {
                        singleDownloadApkAndCode(temp, path, needToAdd, id);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                };
                executorService.execute(run);
            }
            executorService.shutdown();

//            for (int i = start; i < end; i += 2) {
//                System.out.println(i / 2);
//
//                String workpath = sourceCodePath + File.separator + (i / 2);
//                new File(workpath).mkdir();
//
//                String codeUrl = downloadUrls.get(i);
//                String apkUrl = downloadUrls.get(i + 1);
//
//                String cmd1 = "wget " + codeUrl;
//                Process p1 = Runtime.getRuntime().exec(cmd1, null, new File(workpath));
//                p1.waitFor();
//
//                String cmd2 = "wget " + apkUrl;
//                Process p2 = Runtime.getRuntime().exec(cmd2, null, new File(workpath));
//                p2.waitFor();
//
//
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * For multi-thread
     *
     * @param apkAndUrls
     */
    public void singleDownloadApkAndCode(List<String> apkAndUrls, String sourceCodePath, int needToAdd, int id) {
        for (int i = 0; i < apkAndUrls.size(); i++) {
            try {
                String workPath = sourceCodePath + "/" + (i + needToAdd);
                if (new File(workPath).exists()) {
                    if (new File(workPath).listFiles().length == 2) {
                        continue;
                    }
                }
                new File(workPath).mkdir();
                String downloadUrl = apkAndUrls.get(i);

                FileUtils.write(new File(workPath + "/info.txt"), downloadUrl + "\n", "utf-8", false);
//                FileUtils.write(new File(workPath + "/info.txt"), "\n", "utf-8", true);

                String cmd1 = "wget " + downloadUrl;
//                if(!downloadUrl.endsWith(".apk")) {
//                    cmd1 = "wget --header=\'Authorization: token " + StaticResource.getToken(id) + "\' " + downloadUrl;
//                }

                Process p1 = Runtime.getRuntime().exec(Tools.getCmd(cmd1), null, new File(workPath));
                Tools.processMessage(p1.getErrorStream(), true);
                Tools.processMessage(p1.getInputStream(), true);

                p1.waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    void split() {
        List<JSONObject> res = new ArrayList<>();
        String basePath = "/home/cary/Documents/Data/crawl/";
        try {
            String all = FileUtils.readFileToString(new File(basePath + "request.json"), "utf-8");
            String[] contents = all.split("onelineend");

            BufferedWriter writer = new BufferedWriter(new FileWriter(basePath + "repo.json"));

            Set<Integer> set = new HashSet<>();

            for (String content : contents) {
                JSONObject h = new JSONObject(content);
                JSONArray items = h.getJSONArray("items");
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);

                    Repo repo = new Repo();
                    repo.url = item.getString("html_url");
                    repo.name = item.getString("full_name");
                    repo.size = item.getInt("size");
                    repo.starCount = item.getInt("stargazers_count");
                    repo.forkCount = item.getInt("forks_count");
                    repo.watchCount = item.getInt("watchers");

                    if (!set.contains(item.getInt("id"))) {
                        writer.write(repo.toJSONObject(repo).toString());
                        writer.newLine();
                        writer.flush();
                    }
                    set.add(item.getInt("id"));

//                    System.out.println(repo.name + ",,," + getIssueCnt(repo.name));

                    // TODO 根据爬取的issue和commit结构以及内容，将他们的结构抽象出来，最后整理出结果扔给GA
                }
            }
            writer.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    int getIssueCnt(String name) {

//        String cmd = "curl https://api.github.com/search/issues?access_token=406b8b1cdc15f3cb1757cdd7a3a170d3e11c8c0d&" +
//                "q=repo:" + name + "+type:issue+state:";
        String cmd = "curl https://api.github.com/search/issues?access_token=406b8b1cdc15f3cb1757cdd7a3a170d3e11c8c0d&" +
                "q=repo:" + name + "+state:";
        int closedCnt, openCnt;

        closedCnt = getCnt(cmd + "closed&page_size=1");
        openCnt = getCnt(cmd + "open&page_size=1");

        return closedCnt + openCnt;
    }

    List<String[]> getBranches(String name) {
        List<String[]> res = new ArrayList<>();
        String branchName;
        String cmd = "curl https://api.github.com/repos/" + name + "/branches?access_token=406b8b1cdc15f3cb1757cdd7a3a170d3e11c8c0d";
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            StringBuilder shas = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                shas.append(line);
            }
            p.waitFor();

            JSONArray shaJsonArray = new JSONArray(shas.toString());

            for (int i = 0; i < shaJsonArray.length(); i++) {
                JSONObject sha = shaJsonArray.getJSONObject(i);
                branchName = sha.getString("name");
                String[] s = {branchName, sha.getJSONObject("commit").getString("sha")};
                res.add(s);
            }

        } catch (IOException | InterruptedException | JSONException e) {
            e.printStackTrace();
        }
        return res;
    }

    int getCommitCnt(String name) {
        String cmd = "curl https://api.github.com/search/commits?access_token=406b8b1cdc15f3cb1757cdd7a3a170d3e11c8c0d&" +
                "q=repo:" + name;
        int cnt = getCnt(cmd);
        System.out.println(cnt);

        return cnt;
    }

    int getIssueCntFromFile(String name) {
        int res = 0;
        try {
            List<String> cnts = FileUtils.readLines(new File("/home/cary/Documents/Data/crawl/issueCnt.txt"), "utf-8");
            for (String cnt :
                    cnts) {
                if (cnt.contains(name)) {
                    res = Integer.parseInt(cnt.substring(cnt.indexOf(",,,") + 3));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    void getSingleIssue(String name, int totalNum) {
        System.out.println(name);
        String baseUrl = "curl https://api.github.com/repos/" + name + "/issues?access_token=406b8b1cdc15f3cb1757cdd7a3a170d3e11c8c0d&state=all";
        // &q=created:>2018-03-01
        int pageSize = 100;
        int pageNum = totalNum / pageSize + (totalNum % pageSize == 0 ? 0 : 1);
        try {
            for (int pageIdx = 0; pageIdx < pageNum; pageIdx++) {
                System.out.println(pageIdx);
                String cmd = baseUrl + "&page=" + pageIdx + "&per_page=" + pageSize;

                Tools.protectRateLimit(false);

                Process p = Runtime.getRuntime().exec(cmd);
                Tools.processMessage(p.getErrorStream(), false);

                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String fileName = name.replace('/', '_');
                BufferedWriter writer = new BufferedWriter(new FileWriter("/home/cary/Documents/Data/crawl/issue/"
                        + fileName + ".json", true));
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                    writer.flush();
                }
                if (pageIdx < pageNum - 1) {
                    writer.write("onepageend");
                    writer.newLine();
                    writer.flush();
                }
                writer.close();

                p.waitFor();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    void getAllIssue() {
        String basePath = "/home/cary/Documents/Data/crawl/";
//        String basePath = "/home/cary/ga/";
        try {
            List<String> repos = FileUtils.readLines(new File(basePath + "issueCnt.txt"), "utf-8");
            int cur = 0, start = 0;
            for (String repo : repos) {
                cur++;
                if (cur < start) {
                    continue;
                }
                String name = repo.substring(0, repo.indexOf(",,,"));
                int cnt = Integer.parseInt(repo.substring(repo.indexOf(",,,") + 3));

                getSingleIssue(name, cnt);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void getCommits() {
//        String base = "/home/cary/Documents/Data/crawl";
        String base = "/home/cary/ga/";

        try {
            List<String> names = FileUtils.readLines(new File(base + "/issueCnt.txt"), "utf-8");
            for (String name : names) {
                name = name.substring(0, name.indexOf(",,,"));

                Tools.protectRateLimit(false);

                BufferedWriter writer = new BufferedWriter(new FileWriter(base + "/commit/" + name.replace(
                        "/", "_") + ".json", true));

                List<String[]> branches = getBranches(name);
                for (String[] branch : branches) {


                    String branchName = branch[0];
                    String branchSHA = branch[1];

                    writer.write("branch,,," + branchName);
                    writer.newLine();
                    writer.flush();

                    System.out.println(name + ":" + branchName);

                    int curPage = 1;
                    while (true) {
                        System.out.println(curPage);

                        String cmd = "curl https://api.github.com/repos/" + name + "/commits?access_token=" +
                                "406b8b1cdc15f3cb1757cdd7a3a170d3e11c8c0d&per_page=100&sha=" + branchSHA + "&page=";
                        cmd += curPage++;

                        Tools.protectRateLimit(false);

                        Process process = Runtime.getRuntime().exec(cmd);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;
                        StringBuilder request = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            request.append(line);
                            writer.write(line);
                            writer.newLine();
                            writer.flush();
                        }
                        if (new JSONArray(request.toString()).length() == 0) {
                            break;
                        }
                        writer.write("onepageend");
                        writer.newLine();
                        writer.flush();

                        process.waitFor();
                        process.destroyForcibly();
                    }
                }
                writer.close();
            }
        } catch (IOException | JSONException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    void getRelease() {
        String base = "/home/cary/Documents/Data/crawl/";
        File[] repos = new File(base + "issue/").listFiles();
        List<String> haveIssue = new ArrayList<>();
        try {
            List<String> lines = FileUtils.readLines(new File(base + "issueCnt.txt/"), "utf-8");
            for (File repo : repos) {
                String name = repo.getName().substring(0, repo.getName().indexOf("_")) + "/"
                        + repo.getName().substring(repo.getName().indexOf("_") + 1, repo.getName().indexOf(".json"));
                for (String line : lines) {
                    if (name.equals(line.substring(0, line.indexOf(",,,")))) {
                        haveIssue.add(name);
                    }
                }
            }

            for (String issueRepo : haveIssue) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(base + "/release/" +
                        issueRepo.replace("/", "___") + ".json"));

                String cmd = "curl https://api.github.com/repos/" + issueRepo + "/git/refs/tags?access_token="
                        + "406b8b1cdc15f3cb1757cdd7a3a170d3e11c8c0d&per_page=100";
                System.out.println(issueRepo);
                Process p = Runtime.getRuntime().exec(cmd);

                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                    writer.flush();
                }
                writer.close();

                p.waitFor();

            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    void getTime() {
        String base = "/home/cary/Documents/Data/crawl/";
        File[] releases = new File(base + "release").listFiles();
        List<File> needs = new ArrayList<>();
        int cnt = 0;
        try {
            for (File release : releases) {
                String content = FileUtils.readFileToString(release, "utf-8");
                if (content.contains("\"message\": \"Not Found\",")) {
                    cnt++;
                } else {
                    needs.add(release);
                }
            }
            System.out.println("Have Release:" + needs.size());
            for (File need : needs) {
                JSONArray jsonArray = new JSONArray(FileUtils.readFileToString(need, "utf-8"));
                if (jsonArray.length() > 1) {
                    JSONObject end = jsonArray.getJSONObject(jsonArray.length() - 1);
                    JSONObject start = jsonArray.getJSONObject(jsonArray.length() - 2);

                    System.out.println("File:" + need.getName());
                    String startURL = start.getJSONObject("object").getString("url");
                    String startCmd = "curl " + startURL + "?access_token=406b8b1cdc15f3cb1757cdd7a3a170d3e11c8c0d";
                    System.out.println(getCURL(startCmd));

                    String endURL = end.getJSONObject("object").getString("url");
                    String endCmd = "curl " + endURL + "?access_token=406b8b1cdc15f3cb1757cdd7a3a170d3e11c8c0d";
                    System.out.println(getCURL(endCmd));
                    System.out.println("---------------");
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    String getCURL(String cmd) {
        if (!cmd.startsWith("curl")) {
            cmd = "curl " + cmd;
        }
        StringBuilder res = new StringBuilder();
        try {
            Tools.protectRateLimit(false);
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                res.append(line);
            }
            p.waitFor();
            p.destroyForcibly();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return res.toString();
    }

    void getNeedIssue() {
        String base = "/home/cary/Documents/Data/crawl/";
        File[] commits = new File(base + "issue/").listFiles();
        DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        try {
            List<String> timestamps = FileUtils.readLines(new File(base + "timestamp.txt"), "utf-8");
            List<StartEndTime> StartEndTimes = new ArrayList<>();
            for (int i = 0; i < timestamps.size(); i += 4) {
                String name = timestamps.get(i);
                JSONObject start = new JSONObject(timestamps.get(i + 1));
                JSONObject end = new JSONObject(timestamps.get(i + 2));

                String startDate;
                String endDate;
                if (timestamps.get(i + 1).contains("\"tagger\":")) {
                    startDate = start.getJSONObject("tagger").getString("date");
                } else {
                    startDate = start.getJSONObject("author").getString("date");
                }

                if (timestamps.get(i + 2).contains("\"tagger\":")) {
                    endDate = end.getJSONObject("tagger").getString("date");
                } else {
                    endDate = end.getJSONObject("author").getString("date");
                }

                StartEndTimes.add(new StartEndTime(name, startDate, endDate));
            }

            Map<String, StartEndTime> map = new HashMap<>();
            StartEndTimes.forEach(x -> map.put(x.name, x));

            //遍历所有的commit文件
            for (File commit : commits) {
                String name = commit.getName();

                System.out.println("name=" + name);

                //从包含两个以上release（有startTime和endTime）
                if (map.containsKey(name)) {
                    StartEndTime startEndTime = map.get(name);
                    Date start = simpleDateFormat.parse(startEndTime.start);
                    Date end = simpleDateFormat.parse(startEndTime.end);

                    List<String> jsons = getJsons(commit);

                    BufferedWriter writer = new BufferedWriter(new FileWriter(base + "/time_issue/" + commit.getName(), true));
                    for (String s : jsons) {
                        System.out.println("sssssssssss=" + s);

                        if (s.equals("[]") || s.isEmpty()) {
                            continue;
                        }
                        JSONArray issueJSON = new JSONArray(s);
                        for (int j = 0; j < issueJSON.length(); j++) {
                            JSONObject singleIssue = issueJSON.getJSONObject(j);

                            Date createTime = simpleDateFormat.parse(singleIssue.getString("created_at"));
                            if(singleIssue.getString("closed_at").equals("null")){
                                continue;
                            }
                            Date closedTime = simpleDateFormat.parse(singleIssue.getString("closed_at"));

                            if (createTime.after(start) && closedTime.before(end)) {
                                writer.write(singleIssue.toString());
                                writer.newLine();
                                writer.flush();
                            }
                        }
                    }
                    writer.close();
                } else {
                    FileUtils.copyFile(commit, new File(base + "/time_issue/" + commit.getName()));
                }
            }
        } catch (IOException | JSONException | ParseException e) {
            e.printStackTrace();
        }
    }

    void getNeedCommit() {
        String base = "/home/cary/Documents/Data/crawl/";
        File[] commits = new File(base + "commit/").listFiles();
        DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        try {
            List<String> timestamps = FileUtils.readLines(new File(base + "timestamp.txt"), "utf-8");
            List<StartEndTime> StartEndTimes = new ArrayList<>();
            for (int i = 0; i < timestamps.size(); i += 4) {
                String name = timestamps.get(i);
                JSONObject start = new JSONObject(timestamps.get(i + 1));
                JSONObject end = new JSONObject(timestamps.get(i + 2));

                String startDate;
                String endDate;
                if (timestamps.get(i + 1).contains("\"tagger\":")) {
                    startDate = start.getJSONObject("tagger").getString("date");
                } else {
                    startDate = start.getJSONObject("author").getString("date");
                }

                if (timestamps.get(i + 2).contains("\"tagger\":")) {
                    endDate = end.getJSONObject("tagger").getString("date");
                } else {
                    endDate = end.getJSONObject("author").getString("date");
                }

                StartEndTimes.add(new StartEndTime(name, startDate, endDate));
            }

            Map<String, StartEndTime> map = new HashMap<>();
            StartEndTimes.forEach(x -> map.put(x.name, x));

            //遍历所有的commit文件
            for (File commit : commits) {
                String name = commit.getName();

                System.out.println("name=" + name);

                //从包含两个以上release（有startTime和endTime）
                if (map.containsKey(name)) {
                    StartEndTime startEndTime = map.get(name);
                    Date start = simpleDateFormat.parse(startEndTime.start);
                    Date end = simpleDateFormat.parse(startEndTime.end);

                    List<String> jsons = getJsons(commit);

                    BufferedWriter writer = new BufferedWriter(new FileWriter(base + "/time_commit/" + commit.getName(), true));
                    for (String s : jsons) {
                        System.out.println("sssssssssss=" + s);

                        if (s.equals("[]") || s.isEmpty()) {
                            continue;
                        }
                        JSONArray issueJSON = new JSONArray(s);
                        for (int j = 0; j < issueJSON.length(); j++) {
                            JSONObject singleIssue = issueJSON.getJSONObject(j);

                            Date commitTime = simpleDateFormat.parse(singleIssue.getJSONObject("commit").getJSONObject("committer").getString("date"));

                            if (commitTime.after(start) && commitTime.before(end)) {
                                writer.write(singleIssue.toString());
                                writer.newLine();
                                writer.flush();
                            }
                        }
                    }
                    writer.close();
                } else {
                    FileUtils.copyFile(commit, new File(base + "/time_commit/" + commit.getName()));
                }
            }
        } catch (IOException | JSONException | ParseException e) {
            e.printStackTrace();
        }
    }

    List<String> getJsons(File commit) {
        List<String> res = new ArrayList<>();
        try {
            List<String> contents = FileUtils.readLines(commit, "utf-8");

            boolean f = false;
            for (int i = 0; i < contents.size(); i++) {
                StringBuilder stringBuilder = new StringBuilder();
                if (i > 0 && contents.get(i).equals("[") && (contents.get(i - 1).contains("branch,,,")
                        || contents.get(i - 1).equals("onepageend"))) {
                    stringBuilder.append(contents.get(i++));
                    f = true;
                }
                while (f && i < contents.size()) {
                    stringBuilder.append(contents.get(i++));
                    if (i < contents.size() - 1 && contents.get(i).equals("]") && (contents.get(i + 1).contains("branch,,,")
                            || contents.get(i + 1).equals("onepageend"))) {
                        stringBuilder.append(contents.get(i));
                        f = false;
                    }
                }
                System.out.println(stringBuilder.toString());
                res.add(stringBuilder.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    void preprocess() {
        String base = "/home/cary/Documents/Data/crawl/";
        File[] files = new File(base + "commit/").listFiles();
        DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        try {
            List<String> timestamps = FileUtils.readLines(new File(base + "timestamp.txt"), "utf-8");
            List<StartEndTime> StartEndTimes = new ArrayList<>();
            for (int i = 0; i < timestamps.size(); i += 4) {
                String name = timestamps.get(i);
                JSONObject start = new JSONObject(timestamps.get(i + 1));
                JSONObject end = new JSONObject(timestamps.get(i + 2));

                String startDate;
                String endDate;
                if (timestamps.get(i + 1).contains("\"tagger\":")) {
                    startDate = start.getJSONObject("tagger").getString("date");
                } else {
                    startDate = start.getJSONObject("author").getString("date");
                }

                if (timestamps.get(i + 2).contains("\"tagger\":")) {
                    endDate = end.getJSONObject("tagger").getString("date");
                } else {
                    endDate = end.getJSONObject("author").getString("date");
                }

                StartEndTimes.add(new StartEndTime(name, startDate, endDate));
            }

            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                List<String> commits = FileUtils.readLines(file, "utf-8");
                List<String> branches = new ArrayList<>();

                String name = file.getName();
                BufferedWriter writer = new BufferedWriter(new FileWriter(base + "/time_commit/" + name));

                boolean flag = false;
                for (StartEndTime startEndTime : StartEndTimes) {

                    Date start = simpleDateFormat.parse(startEndTime.start);
                    Date end = simpleDateFormat.parse(startEndTime.end);


                    if (startEndTime.name.equals(name)) {
                        flag = true;

                        StringBuilder sb = new StringBuilder();
                        for (int j = 0; j < commits.size(); j++) {
                            if (commits.get(j).contains("branch,,,")) {
                                branches.add(sb.toString());
                                sb = new StringBuilder();
                            } else {
                                sb.append(commits.get(j));
                            }
                        }

                        for (int k = 0; k < branches.size(); k++) {
                            String[] ss = branches.get(k).split("onepageend");
                            for (int l = 0; l < ss.length; l++) {
                                if (ss[l].length() == 0) {
                                    continue;
                                }
                                JSONArray tmp = new JSONArray(ss[l]);
                                for (int m = 0; m < tmp.length(); m++) {
                                    JSONObject jsonObject = tmp.getJSONObject(m);
                                    Date commitTime = simpleDateFormat.parse(jsonObject.getJSONObject("commit").getJSONObject("committer").getString("date"));
                                    if (commitTime.after(start) && commitTime.before(end)) {
                                        writer.write(jsonObject.toString());
                                        writer.flush();
                                    }
                                }
                            }
                        }
                    }
                }
                writer.close();

                if (!flag) {
                    FileUtils.copyFile(file, new File(base + "/time_commit/" + file.getName()));
                }
            }
        } catch (IOException | JSONException | ParseException e) {
            e.printStackTrace();
        }
    }

//    public static void main(String[] args) {
//        Crawl crawl = new Crawl();
////        crawl.getNeedCommit();
//        crawl.preprocess();
//    }

    class StartEndTime {
        String name;
        String start;
        String end;

        StartEndTime(String name, String start, String end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }

        public String toString() {
            return name + "," + start + "," + end;
        }
    }
}
