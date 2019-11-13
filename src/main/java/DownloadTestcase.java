import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author cary.shi on 2019/10/24
 */
public class DownloadTestcase {
    static List<String> getAK(String akPath) {
        File AKFile = new File(akPath);
        try {
            return FileUtils.readLines(AKFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        //oss://mooctest-share/mooctest-accept-test-dataset/
        String endpoint = "http://oss-cn-hangzhou.aliyuncs.com";
        List<String> ak = getAK("G:\\token\\AK.txt");
        String accessKeyId = ak.get(0);
        String accessKeySecret = ak.get(1);

        String bucketName = "mooctest-share";
        String objectName = "mooctest-accept-test-dataset";

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        ObjectListing objectListing = ossClient.listObjects(bucketName);

        List<OSSObjectSummary> sums = objectListing.getObjectSummaries();
        for (OSSObjectSummary s : sums) {
            System.out.println("\t" + s.getKey());
        }

        ossClient.shutdown();
    }
}
