import com.sun.net.httpserver.HttpServer;
import org.apache.mahout.cf.taste.impl.model.MemoryIDMigrator;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericBooleanPrefUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created by halil on 24.09.2016.
 */
public class RecommendationServer {

    private static final MemoryIDMigrator memoryIDMigrator = new MemoryIDMigrator();
    private static UserBasedRecommender recommender;
    private static final String DATA_PATH = "connections.csv";
    private static final double SIMILARITY_THRESHOLD = 0.1;

    public static void main(String[] args) throws Exception {
        trainRecommender();
        startServer();
    }

    public static void trainRecommender() throws IOException {
        DataModel model = new StringSupportFileDataModel(new File(DATA_PATH));
        UserSimilarity similarity = new TanimotoCoefficientSimilarity(model);
        UserNeighborhood neighborhood = new ThresholdUserNeighborhood(SIMILARITY_THRESHOLD, similarity, model);
        recommender = new GenericBooleanPrefUserBasedRecommender(model, neighborhood, similarity);
    }

    private static void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(3000), 0);
        server.createContext("/people", new RecommendationHandler());
        server.createContext("/follow", new FollowHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    public static Recommender getRecommender()
    {
        return recommender;
    }

    public static MemoryIDMigrator getMemoryIDMigrator()
    {
        return memoryIDMigrator;
    }

    private static class StringSupportFileDataModel extends FileDataModel
    {
        public StringSupportFileDataModel(File dataFile) throws IOException {
            super(dataFile);
        }

        @Override
        protected long readUserIDFromString(String stringID) {
            long result = memoryIDMigrator.toLongID(stringID);
            memoryIDMigrator.storeMapping(result, stringID);
            return result;
        }

        @Override
        protected long readItemIDFromString(String stringID) {
            long result = memoryIDMigrator.toLongID(stringID);
            memoryIDMigrator.storeMapping(result, stringID);
            return result;
        }
    }
}
