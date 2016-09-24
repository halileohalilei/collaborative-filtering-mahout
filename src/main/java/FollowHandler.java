import com.sun.net.httpserver.HttpExchange;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Created by halil on 24.09.2016.
 */

public class FollowHandler extends BaseHttpHandler {

    public void handle(HttpExchange httpExchange) throws IOException {
        Map<String, String> params = getParams(httpExchange);
        String userID = params.get("userID");
        String starID = params.get("starID");

        FileWriter pw = new FileWriter("F:\\connections.csv",true);
        pw.append(userID).append(",").append(starID);
        pw.flush();
        pw.close();
        RecommendationServer.trainRecommender();
    }
}
