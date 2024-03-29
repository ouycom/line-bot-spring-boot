package com.iphayao.linebot;

import com.iphayao.linebot.util.JsonUtil;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TemplateMessage;
import com.linecorp.bot.model.message.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@Slf4j
public class LineNotiController {

    private static final String strEndpoint = "https://notify-api.line.me/api/notify";

    @Value("${line.bot.channel-secret}")
    private String token;
    @Autowired
    private Environment env;
    @Autowired
    private JsonUtil jsonUtil;
    @Autowired
    private LineBotController lineBotController;


    @GetMapping("/do")
    public boolean callEvent(@RequestBody String body) throws IOException {
        log.info("callEvent : {}" , body);

        String json = env.getProperty("ouybot." + body);
        log.info("response : {}" , json);

        Map<String, Object> map = jsonUtil.json2Map(json);
        String type = (String)map.get("type");
        switch (type){
            case "template" : {
                log.info("createTemplateMessage from json");

                Message message = lineBotController.createTemplateMessage(map);
                log.info("templateMessage : {}", message.toString());
                break;
            }
            case "image" :
                break;
            case "flex" : {
                log.info("createFlexMessage from json");

                Message message = lineBotController.createFlexMessage(map);
                log.info("flexMessage : {}", message.toString());
                break;
            }
        }

//        return callEvent(token, json);
        return true;
    }

    /* same as WebController */
    public boolean callEvent(String token, String message) {
        boolean result = false;
        try {
            message = replaceProcess(message);
            message = URLEncoder.encode(message, "UTF-8");
            String strUrl = strEndpoint;
            URL url = new URL( strUrl );
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.addRequestProperty("Authorization",  "Bearer " + token);
            connection.setRequestMethod( "POST" );
            connection.addRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );
            connection.setDoOutput( true );
            String parameterString = new String("message=" + message);
            PrintWriter printWriter = new PrintWriter(connection.getOutputStream());
            printWriter.print(parameterString);
            printWriter.close();
            connection.connect();

            int statusCode = connection.getResponseCode();
            if ( statusCode == 200 ) {
                result = true;
            } else {
                throw new Exception( "Error:(StatusCode)" + statusCode + ", " + connection.getResponseMessage() );
            }
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private String replaceProcess(String txt){
        txt = replaceAllRegex(txt, "\\\\", "￥");		// \
        return txt;
    }
    private String replaceAllRegex(String value, String regex, String replacement) {
        if ( value == null || value.length() == 0 || regex == null || regex.length() == 0 || replacement == null )
            return "";
        return Pattern.compile(regex).matcher(value).replaceAll(replacement);
    }
}
