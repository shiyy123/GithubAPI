public class Main {

    public static void getRepo() {
        Crawl crawl = new Crawl();

        int startYear = 2010;
        int startMonth = 1;
        int startDay = 1;

        int endYear = 2018;

        int nextYear = startYear;
        int nextMonth = startMonth + 1;
        int nextDay = startDay;

        while (startYear <= endYear) {

            nextMonth = startMonth + 1;

            if(startMonth == 12) {
                nextMonth = 1;
                nextYear++;
            }

            if(startYear == 2018 && startMonth == 12) {
                break;
            }

            String gap = Tools.toTime(startYear) + "-" + Tools.toTime(startMonth) + "-" + Tools.toTime(startDay)
                    + ".." + Tools.toTime(nextYear) + "-" + Tools.toTime(nextMonth) + "-" + Tools.toTime(nextDay);


            crawl.getRepos("topic:android+language:java+created:" + gap, StaticResource.basePath + "repos/repos.json");
            startMonth++;
            if(startMonth == 13) {
                startYear++;
                startMonth = 1;
            }
        }
    }

    public static void process4getRepos() {
        Crawl crawl = new Crawl();
        crawl.process4getRepos(StaticResource.basePath + "repos/repos.json", StaticResource.basePath + "repos/process_repo.json");
    }

    public static void getReposNameFromRepoInfo() {
        Crawl crawl = new Crawl();
        crawl.getReposNameFromRepoInfo(StaticResource.basePath + "repos/process_repo.json", StaticResource.basePath + "repos/repoName.txt");
    }

    public static void getReleaseInfo() {
        Crawl crawl = new Crawl();
        crawl.getReleaseInfo(StaticResource.basePath + "repos/repoName.txt", StaticResource.basePath + "release/");
    }

    public static void processHalfResOfRelease() {
        Crawl crawl = new Crawl();
        crawl.processHalfResOfRelease();
    }

    public static void countReleaseHaveApk() {
        Crawl crawl = new Crawl();
        crawl.countReleaseHaveApk(StaticResource.basePath + "release/release.json");
    }

    public static void getApkAndCodeDownloadUrl() {
        Crawl crawl = new Crawl();
        crawl.getApkAndCodeDownloadUrl(StaticResource.basePath + "release/apk.json");
    }

    public static void main(String[] args) {
        Crawl crawl = new Crawl();
        crawl.downloadApkAndCode(StaticResource.basePath +  "sourcecode/source_apk.txt");
    }
}
