import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by halil on 24.09.2016.
 */
public abstract class BaseHttpHandler implements HttpHandler {

    protected static final String HEADER_ALLOW = "Allow";
    protected static final String HEADER_CONTENT_TYPE = "Content-Type";

    protected static final Charset CHARSET = StandardCharsets.UTF_8;

    protected static final int STATUS_OK = 200;
    protected static final int STATUS_METHOD_NOT_ALLOWED = 405;

    protected static final int NO_RESPONSE_LENGTH = -1;

    protected static final String METHOD_GET = "GET";
    protected static final String METHOD_OPTIONS = "OPTIONS";
    protected static final String ALLOWED_METHODS = METHOD_GET + "," + METHOD_OPTIONS;


    protected Map<String, String> getParams(HttpExchange httpExchange) {
        String query = httpExchange.getRequestURI().getQuery();
        Map<String, String> result = new HashMap<String, String>();
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            if (pair.length > 1) {
                result.put(pair[0], pair[1]);
            }else{
                result.put(pair[0], "");
            }
        }
        return result;
    }

}
