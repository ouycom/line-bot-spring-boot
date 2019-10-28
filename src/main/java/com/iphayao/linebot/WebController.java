package com.iphayao.linebot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

@RestController
@Slf4j
public class WebController {

    private String lineApi = "https://notify-api.line.me/api/notify";

    @Autowired
    private RestTemplate myRestTemplate;

    @GetMapping("/test")
    public String echoDate(){
        return new Date().toString();
    }

    @GetMapping("/ouynoti")
    public ResponseEntity<String> ouynoti(@RequestHeader HttpHeaders httpHeaders, @RequestBody String body){
        log.info("Request Header : {}", httpHeaders.toString());
        log.info("Request Body : {}", body);

        String token = "VF7JzcDNi92cMcy42JNuWxQgukegKw4b8MbeQzOL5V5";
        return sendLineNoti(token, body);
    }

    @GetMapping("/noti")
    public ResponseEntity<String> initiateSignOn(@RequestHeader HttpHeaders httpHeaders, @RequestBody String body) {
        log.info("Request Header : {}", httpHeaders.toString());
        log.info("Request Body : {}", body);

        String token = httpHeaders.getFirst("token");
        return sendLineNoti(token, body);
    }

    private ResponseEntity<String> sendLineNoti(String token, String body){
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setContentLength(body.length());
        headers.add("Authorization", "Bearer "+token);
//        headers.add("content", body);
//        headers.add("message", body);
        log.info(headers.toString());
        HttpEntity<String> request = new HttpEntity("message="+body, headers);

        try {

            ResponseEntity<String> response = myRestTemplate.exchange(lineApi, HttpMethod.POST, request, String.class);
//'header'=> "Content-Type: application/x-www-form-urlencoded\r\n"
//                      ."Authorization: Bearer ".$token."\r\n"
//                      ."Content-Length: ".strlen($queryData)."\r\n",
            return response;
        }catch (Exception e){
            log.error("{}", e.getMessage(), e);
        }

        return null;
    }
}
