import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Crawl {

    /**
     * get the attribute total_count
     * @param cmd
     * @return
     */
    private int getCnt(String cmd) {
        Tools.protectRateLimit();

        int cnt = 0;
        try {
            Process p = Runtime.getRuntime().exec(cmd);

            StringBuilder res = Tools.getProcessOutput(p.getInputStream(), true);

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
     * @param condition request condition
     * @param dataStorePath data store to
     */
    public void getRepos(String condition, String dataStorePath) {
//        String baseUrl = "curl https://api.github.com/search/repositories?access_token=406b8b1cdc15f3cb1" +
//                "757cdd7a3a170d3e11c8c0d&q=topic:android-app+language:java+created:>2018-03-01";
        String baseUrl = "curl https://api.github.com/search/repositories?access_token=" + StaticResource.token +
                "&q=" + condition;

        try {
            int pageSize = 100;
            int totalNum = getCnt(baseUrl);
            System.out.println(totalNum);

            int pageNum = totalNum / pageSize + (totalNum % pageSize == 0 ? 0 : 1);
            for (int pageCnt = 0; pageCnt < pageNum; pageCnt++) {
                System.out.println(pageCnt);
                String cmd = baseUrl + "&sort=stars&order=desc&page=" + pageCnt + "&per_page=" + pageSize;

                Process p = Runtime.getRuntime().exec(Tools.getCmd(cmd));
                Tools.readAndWrite(p.getInputStream(), dataStorePath, "onelineend");

                p.waitFor();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
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

                Tools.protectRateLimit();

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

                Tools.protectRateLimit();

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

                        Tools.protectRateLimit();

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
            Tools.protectRateLimit();
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

//    void getNeedIssue() {
//        String base = "/home/cary/Documents/Data/crawl/";
//        File[] issues = new File(base + "issue/").listFiles();
//        DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
//
//        try {
//            List<String> timestamps = FileUtils.readLines(new File(base + "timestamp.txt"), "utf-8");
//            List<StartEndTime> StartEndTimes = new ArrayList<>();
//            for (int i = 0; i < timestamps.size(); i+=4) {
//                String name = timestamps.get(i);
//                JSONObject start = new JSONObject(timestamps.get(i + 1));
//                JSONObject end = new JSONObject(timestamps.get(i + 2));
//
//                String startDate;
//                String endDate;
//                if(timestamps.get(i + 1).contains("\"tagger\":")) {
//                    startDate = start.getJSONObject("tagger").getString("date");
//                } else {
//                    startDate = start.getJSONObject("author").getString("date");
//                }
//
//                if(timestamps.get(i + 2).contains("\"tagger\":")) {
//                    endDate = end.getJSONObject("tagger").getString("date");
//                } else {
//                    endDate = end.getJSONObject("author").getString("date");
//                }
//
//                StartEndTimes.add(new StartEndTime(name, startDate, endDate));
//            }
//
////            StartEndTimes.forEach(System.out::println);
//            for (File issue : issues) {
//                String name = issue.getName();
//                boolean flag = false;
//                for (StartEndTime startEndTime : StartEndTimes) {
//                    Date start = simpleDateFormat.parse(startEndTime.start);
//                    Date end = simpleDateFormat.parse(startEndTime.end);
//
//                    if (name.equals(startEndTime.name)) {
//                        String[] contents = FileUtils.readFileToString(issue, "utf-8").split("onepageend");
//                        BufferedWriter writer = new BufferedWriter(new FileWriter(base + "/time_issue/" + issue.getName()));
//                        for (String content : contents) {
//                            JSONArray issueJSON = new JSONArray(content);
//                            for (int j = 0; j < issueJSON.length(); j++) {
//                                JSONObject singleIssue = issueJSON.getJSONObject(j);
//                                if(singleIssue.getString("closed_at").equals("null")) {
//                                    continue;
//                                }
//                                Date createTime = simpleDateFormat.parse(singleIssue.getString("created_at"));
//                                Date closedTime = simpleDateFormat.parse(singleIssue.getString("closed_at"));
//                                if (createTime.after(start) && closedTime.before(end)) {
//                                    System.out.println();
//                                    writer.write(singleIssue.toString());
//                                    writer.newLine();
//                                    writer.flush();
//                                }
//                            }
//                        }
//                        writer.close();
//                        flag = true;
//                    }
//                }
//                if(!flag) {
//                    FileUtils.copyFile(issue, new File(base + "/time_issue/" + issue.getName()));
//                }
//            }
//        } catch (IOException | JSONException | ParseException e) {
//            e.printStackTrace();
//        }
//    }

//    void getNeedCommit() {
//        String base = "/home/cary/Documents/Data/crawl/";
//        File[] commits = new File(base + "commit/").listFiles();
//        DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
//
//        try {
//            List<String> timestamps = FileUtils.readLines(new File(base + "timestamp.txt"), "utf-8");
//            List<StartEndTime> StartEndTimes = new ArrayList<>();
//            for (int i = 0; i < timestamps.size(); i+=4) {
//                String name = timestamps.get(i);
//                JSONObject start = new JSONObject(timestamps.get(i + 1));
//                JSONObject end = new JSONObject(timestamps.get(i + 2));
//
//                String startDate;
//                String endDate;
//                if(timestamps.get(i + 1).contains("\"tagger\":")) {
//                    startDate = start.getJSONObject("tagger").getString("date");
//                } else {
//                    startDate = start.getJSONObject("author").getString("date");
//                }
//
//                if(timestamps.get(i + 2).contains("\"tagger\":")) {
//                    endDate = end.getJSONObject("tagger").getString("date");
//                } else {
//                    endDate = end.getJSONObject("author").getString("date");
//                }
//
//                StartEndTimes.add(new StartEndTime(name, startDate, endDate));
//            }
//
////            StartEndTimes.forEach(System.out::println);
//            for (File commit : commits) {
//                String name = commit.getName();
//                boolean flag = false;
//                for (StartEndTime startEndTime : StartEndTimes) {
//                    Date start = simpleDateFormat.parse(startEndTime.start);
//                    Date end = simpleDateFormat.parse(startEndTime.end);
//
//                    if (name.equals(startEndTime.name)) {
//                        List<String> contents = FileUtils.readLines(commit, "utf-8");
//                        StringBuilder stringBuilder = new StringBuilder();
//                        boolean f = false;
//                        for (int i = 0; i < contents.size(); i++) {
//                            if(contents.get(i).equals("[") && (contents.get(i - 1).contains("branch,,,")
//                                    || contents.equals("onepageend"))) {
//                                stringBuilder.append(contents.get(i));
//                                f = true;
//                            }
//                            while (f) {
//                                stringBuilder.append(contents.get(i));
//                                if(contents.get(i).equals("]") && (contents.get(i - 1).contains("branch,,,")
//                                        || contents.equals("onepageend"))){
//
//                                }
//                            }
//                        }
//
//
//                        BufferedWriter writer = new BufferedWriter(new FileWriter(base + "/time_commit/" + commit.getName()));
//                        for (String content : contents) {
//                            JSONArray issueJSON = new JSONArray(content);
//                            for (int j = 0; j < issueJSON.length(); j++) {
//                                JSONObject singleIssue = issueJSON.getJSONObject(j);
//                                if(singleIssue.getString("closed_at").equals("null")) {
//                                    continue;
//                                }
//                                Date createTime = simpleDateFormat.parse(singleIssue.getString("created_at"));
//                                Date closedTime = simpleDateFormat.parse(singleIssue.getString("closed_at"));
//                                if (createTime.after(start) && closedTime.before(end)) {
//                                    System.out.println();
//                                    writer.write(singleIssue.toString());
//                                    writer.newLine();
//                                    writer.flush();
//                                }
//                            }
//                        }
//                        writer.close();
//                        flag = true;
//                    }
//                }
//                if(!flag) {
//                    FileUtils.copyFile(commit, new File(base + "/time_issue/" + commit.getName()));
//                }
//            }
//        } catch (IOException | JSONException | ParseException e) {
//            e.printStackTrace();
//        }
//    }

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


                    if(startEndTime.name.equals(name)) {
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
                                if(ss[l].length() == 0){
                                    continue;
                                }
                                JSONArray tmp = new JSONArray(ss[l]);
                                for (int m = 0; m < tmp.length(); m++) {
                                    JSONObject jsonObject = tmp.getJSONObject(m);
                                    Date commitTime = simpleDateFormat.parse(jsonObject.getJSONObject("commit").getJSONObject("committer").getString("date"));
                                    if(commitTime.after(start) && commitTime.before(end)) {
                                        writer.write(jsonObject.toString());
                                        writer.flush();
                                    }
                                }
                            }
                        }
                    }
                }
                writer.close();

                if(!flag) {
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
