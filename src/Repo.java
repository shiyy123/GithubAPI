import org.json.JSONException;
import org.json.JSONObject;

public class Repo {
    String name;
    String url;
    int size;
    int starCount;
    int forkCount;
    int watchCount;
    Issue issue;
    Commit commit;
    Repo() {

    }

    JSONObject toJSONObject(Repo repo) {
         JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", repo.name);
            jsonObject.put("url", repo.url);
            jsonObject.put("size", repo.size);
            jsonObject.put("starCount", repo.starCount);
            jsonObject.put("forkCount", repo.forkCount);
            jsonObject.put("watchCount", repo.watchCount);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
