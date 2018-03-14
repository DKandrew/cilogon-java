import com.mashape.unirest.http.*;
import org.json.JSONObject;
import spark.*;
import static spark.Spark.*;
import java.io.*;
import java.util.*;


public class Application {
    private static String CILOGON_CLIENT_ID;
    private static String CILOGON_CLIENT_SECRET;
    private static final String CILOGON_CALLBACK_URI = "https://cilogon-web-java.herokuapp.com/callback";

    private static final String PATH_INDEX = "/";
    private static final String PATH_START = "/start";
    private static final String PATH_CALLBACK = "/callback";
    private static final String PATH_LOGOUT = "/logout";

    // Client Profile
    private static Map<String, Object> sessionProfile = new HashMap<>();

    public static void main(String[] args) {
        // Configure Spark
        port(getHerokuAssignedPort());
        //getCILogonSecretViaEnv();
        getCILogonSecretViaFile("/Users/xxx/xxx/xxx/CILogon_Java/src/main/resources/secret.txt");

        // Initial session profile
        setDefaultSessionProfile();

        // Set up routRes
        get(PATH_INDEX, loadHomePage);
        get(PATH_START, (request, response) -> {
            response.redirect("https://cilogon.org/authorize?" +
                    "response_type=code&" +
                    "client_id=" + CILOGON_CLIENT_ID + "&" +
                    "redirect_uri=" + CILOGON_CALLBACK_URI + "&" +
                    "scope=openid+profile+email+org.cilogon.userinfo");
            return null;
        });
        get(PATH_CALLBACK, callbackFunction);
        get(PATH_LOGOUT, logoutCallback);
    }

    private static void setDefaultSessionProfile() {
        // Url
        sessionProfile.put("url_start", PATH_START);
        sessionProfile.put("url_logout", PATH_LOGOUT);

        // Client
        sessionProfile.put("userIsLogin", false);
        sessionProfile.put("userId", 0);
        sessionProfile.put("fullName", "");
        sessionProfile.put("email", "");
        sessionProfile.put("authority", "");
    }

    // Find the port number in heroku
    public static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 4567;
    }

    /*
    Read the CILogon secret from environment variable
     */
    private static void getCILogonSecretViaEnv(){
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("CILOGON_CLIENT_ID") != null) {
            CILOGON_CLIENT_ID = processBuilder.environment().get("CILOGON_CLIENT_ID");
        }
        if (processBuilder.environment().get("CILOGON_CLIENT_SECRET") != null) {
            CILOGON_CLIENT_SECRET = processBuilder.environment().get("CILOGON_CLIENT_SECRET");
        }
    }

    /*
    Read the CILogon Secret from local file
    Assume the first line is client id, the second line is client secret. You can modify this function based on your own needs
    The path has to be an abosolute path
     */
    private static void getCILogonSecretViaFile(String path) {
        try {
            File file = new File(path);
            BufferedReader br = new BufferedReader(new FileReader(file));

            String st;
            if ((st = br.readLine()) != null) {
                CILOGON_CLIENT_ID = st;
            }
            if ((st = br.readLine()) != null) {
                CILOGON_CLIENT_SECRET = st;
            }
        } catch (java.io.FileNotFoundException e) {
            System.out.println("Cannot find the secret file! " + e);
        } catch (Exception e){
            System.out.println("Read secret failed with error:" + e);
        }
    }

    private static Route loadHomePage = (Request request, Response response) -> {
        String html;
        try {
            html = new FreeMarkerTemplateEngine("src/main/resources/templates").render(new ModelAndView(sessionProfile, "main_page.ftl"));
        } catch (Exception e) {
            html = "";
            System.out.println("Error in loading template:" + e);
        }
        return html;
    };
    
    private static Route callbackFunction = (Request request, Response response) -> {
        // 1. Get the authorization token
        String code = request.queryParams("code");

        // Submit authorization token to the CILogon and receive the access token
        Map<String, Object> model = new HashMap<>();
        model.put("grant_type", "authorization_code");
        model.put("client_id", CILOGON_CLIENT_ID);
        model.put("client_secret", CILOGON_CLIENT_SECRET);
        model.put("code", code);
        model.put("redirect_uri", CILOGON_CALLBACK_URI);

        // 2. Get the access_token
        HttpResponse<JsonNode> jsonResponse = Unirest.post("https://cilogon.org/oauth2/token")
                .fields(model)
                .asJson();
        String access_token = jsonResponse.getBody().getObject().getString("access_token");
        model.put("access_token", access_token);

        // 3. Get the user's information
        jsonResponse = Unirest.post("https://cilogon.org/oauth2/userinfo")
                .fields(model)
                .asJson();

        JSONObject user_profile = jsonResponse.getBody().getObject();
        String userID = user_profile.getString("cert_subject_dn");
        String fullName = user_profile.getString("given_name") + user_profile.getString("family_name");
        String email = user_profile.getString("email");
        String authority = user_profile.getString("idp_name");

        sessionProfile.put("userIsLogin", true);
        sessionProfile.put("userId", userID);
        sessionProfile.put("fullName", fullName);
        sessionProfile.put("email", email);
        sessionProfile.put("authority", authority);

        response.redirect("/");
        return null;
    };

    private static Route logoutCallback = (Request request, Response response) -> {
        setDefaultSessionProfile();
        response.redirect("/");
        return null;
    };
}