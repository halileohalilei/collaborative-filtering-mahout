import com.sun.net.httpserver.HttpExchange;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by halil on 24.09.2016.
 */

public class FollowHandler extends BaseHttpHandler {

    public void handle(HttpExchange httpExchange) throws IOException {
        Map<String, String> params = getParams(httpExchange);
        String userID = params.get("userID");
        String starID = params.get("starID");

        FileWriter pw = new FileWriter("connections.csv",true);
        pw.append(userID).append(",").append(starID).append('\n');
        pw.flush();
        pw.close();
        RecommendationServer.trainRecommender();

        Map<String, String> response = new HashMap<>();
        response.put("Result", "OK");
        response.put("userID", userID);
        response.put("starID", starID);

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String jsonResponse = ow.writeValueAsString(response);

        sendResponse(httpExchange, jsonResponse, STATUS_OK);
    }
}
