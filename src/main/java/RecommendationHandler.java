import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.apache.mahout.cf.taste.common.NoSuchUserException;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.model.IDMigrator;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by halil on 24.09.2016.
 */
public class RecommendationHandler extends BaseHttpHandler {

    public void handle(HttpExchange httpExchange) throws IOException {
        Headers headers = httpExchange.getResponseHeaders();
        headers.set(HEADER_CONTENT_TYPE, String.format("application/json; charset=%s", CHARSET));
        int statusCode = STATUS_OK;
        String response = null;
        Map<String, String> params = getParams(httpExchange);
        String userID = params.get("userID");
        int per = Integer.parseInt(params.get("per"));
        if (per < 1) per = 24;
        int page = Integer.parseInt(params.get("page"));
        if (page < 1) page = 1;

        if (userID != null) {
            List<RecommendedItem> recommendations;
            try {
                Recommender recommender = RecommendationServer.getRecommender();
                IDMigrator idMigrator = RecommendationServer.getMemoryIDMigrator();

                recommendations = recommender.recommend(idMigrator.toLongID(userID), page * per);
                List<Map<String, String>> actors = getRecommendedActors(recommendations.subList((page - 1) * per, page * per));
                ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                response = ow.writeValueAsString(actors);
//                System.out.println(response);
            } catch (NoSuchUserException e) {
                List<Map<String, String>> actors = getRegularActors(page, per);
                response = getAsJSON(actors);
//                System.out.println(response);
            } catch (TasteException e) {
                e.printStackTrace();
                statusCode = STATUS_INTERNAL_SERVER_ERROR;
                response = "";
            }
        }
        sendResponse(httpExchange, response, statusCode);
    }

    private List<Map<String, String>> getRegularActors(int page, int per)
    {
        String queryString = "select * from people limit " + per + " offset " + (page - 1) * per;
        Connection c;
        Statement stmt;
        List<Map<String, String>> actors = new ArrayList<Map<String, String>>();
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:movie_star_data.sqlite");
            c.setAutoCommit(false);

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery(queryString);
            while ( rs.next() ) {
                Map<String, String> actor = new HashMap<String, String>();
                String actorID = rs.getString("star_id");
                String actorName = rs.getString("star_name");
                actor.put("actorID", actorID);
                actor.put("actorName", actorName);
                actors.add(actor);
            }
            rs.close();
            stmt.close();
            c.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return actors;
    }

    private List<Map<String, String>> getRecommendedActors(List<RecommendedItem> recommendations)
            throws TasteException {
        IDMigrator idMigrator = RecommendationServer.getMemoryIDMigrator();
        String queryString = "select * from people where star_id in (";
        for (int i = 0; i < recommendations.size(); i++)
        {
            RecommendedItem item = recommendations.get(i);
            String starID = idMigrator.toStringID(item.getItemID());
            queryString += "\'" + starID + "\'";
            if (i == recommendations.size() - 1)
                queryString += ")";
            else
                queryString += ",";
        }

        Connection c;
        Statement stmt;
        List<Map<String, String>> actors = new ArrayList<Map<String, String>>();
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:movie_star_data.sqlite");
            c.setAutoCommit(false);

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery(queryString);
            while ( rs.next() ) {
                Map<String, String> actor = new HashMap<String, String>();
                String actorID = rs.getString("star_id");
                String actorName = rs.getString("star_name");
                actor.put("actorID", actorID);
                actor.put("actorName", actorName);
                actors.add(actor);
            }
            rs.close();
            stmt.close();
            c.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return actors;
    }
}