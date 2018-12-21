public class Main {
    public static void main(String[] args) {
        Crawl crawl = new Crawl();
        crawl.getRepos("topic:android+language:java", StaticResource.basePath + "repos/repos.json");
    }
}
