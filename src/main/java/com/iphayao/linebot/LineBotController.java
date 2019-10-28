package com.iphayao.linebot;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.iphayao.linebot.util.JsonUtil;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.MessageContentResponse;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.PostbackEvent;
import com.linecorp.bot.model.event.message.ImageMessageContent;
import com.linecorp.bot.model.event.message.LocationMessageContent;
import com.linecorp.bot.model.event.message.StickerMessageContent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.postback.PostbackContent;
import com.linecorp.bot.model.message.*;
import com.linecorp.bot.model.message.template.ButtonsTemplate;
import com.linecorp.bot.model.message.template.CarouselTemplate;
import com.linecorp.bot.model.message.template.ConfirmTemplate;
import com.linecorp.bot.model.message.template.Template;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Slf4j
@LineMessageHandler
public class LineBotController {
    @Autowired
    private LineMessagingClient lineMessagingClient;
    @Autowired
    private Environment env;
    @Autowired
    private JsonUtil jsonUtil;

    /////// Handle Message


    @EventMapping
    public void handleTextMessage(MessageEvent<TextMessageContent> event) {
        log.info(event.toString());
        TextMessageContent message = event.getMessage();
        handleTextContent(event.getReplyToken(), event, message);
    }

    @EventMapping
    public void handleStickerMessage(MessageEvent<StickerMessageContent> event) {
        log.info(event.toString());
//        StickerMessageContent message = event.getMessage();
//        reply(event.getReplyToken(), new StickerMessage(
//                message.getPackageId(), message.getStickerId()
//        ));
        StickerMessageContent message = event.getMessage();
        handleStickerContent(event.getReplyToken(), message);
    }

    @EventMapping
    public void handleLocationMessage(MessageEvent<LocationMessageContent> event) {
        log.info(event.toString());
        LocationMessageContent message = event.getMessage();
        reply(event.getReplyToken(), new LocationMessage(
                (message.getTitle() == null) ? "Location replied" : message.getTitle(),
                message.getAddress(),
                message.getLatitude(),
                message.getLongitude()
        ));
    }

    @EventMapping
    public void handleImageMessage(MessageEvent<ImageMessageContent> event) {
        log.info(event.toString());
        ImageMessageContent content = event.getMessage();
        String replyToken = event.getReplyToken();

        try {
            MessageContentResponse response = lineMessagingClient.getMessageContent(content.getId()).get();
            DownloadedContent jpg = saveContent("jpg", response);
            DownloadedContent previewImage = createTempFile("jpg");

            system("convert", "-resize", "240x",
                    jpg.path.toString(),
                    previewImage.path.toString());

            //old version
//            reply(replyToken, new ImageMessage(jpg.getUri(), previewImage.getUri()));
            //new version
            reply(replyToken, new ImageMessage(URI.create(jpg.getUri()) , URI.create(previewImage.getUri())));

        } catch (InterruptedException | ExecutionException e) {
            reply(replyToken, new TextMessage("Cannot get image: " + content));
            throw new RuntimeException(e);
        }

    }

    @EventMapping
    public void handlePostbackMessage(PostbackEvent event) {
        log.info(event.toString());
        PostbackContent content = event.getPostbackContent();
        handlePostbackContent(event.getReplyToken(), event, content);
    }

    /////// Handle Content

    private void handleTextContent(String replyToken, Event event, TextMessageContent content) {
        String text = content.getText();

        log.info("Got text message from {} : {}", replyToken, text);

        switch (text) {
            case "Profile": {
                String userId = event.getSource().getUserId();
                if(userId != null) {
                    lineMessagingClient.getProfile(userId)
                            .whenComplete((profile, throwable) -> {
                                if(throwable != null) {
                                    this.replyText(replyToken, throwable.getMessage());
                                    return;
                                }
                                this.reply(replyToken, Arrays.asList(
                                        new TextMessage("Display name: " + profile.getDisplayName()),
                                        new TextMessage("Status message: " + profile.getStatusMessage()),
                                        new TextMessage("User ID: " + profile.getUserId())
                                ));
                            });
                }
                break;
            }
            default:
                String json = env.getProperty("ouybot." + text);
                if(json!=null){
                    try {
                        this.replyObject(replyToken, json);
                    } catch (IOException e) {
                        log.error("Error : {}", e.getMessage(), e);
                        e.printStackTrace();
                    }
                }else {
                    log.info("Return echo message {} : {}", replyToken, text);
                    this.replyText(replyToken, text);
                }
        }
    }

    private void handleStickerContent(String replyToken, StickerMessageContent content) {
        reply(replyToken, new StickerMessage(
                content.getPackageId(), content.getStickerId()
        ));
    }

    private void handlePostbackContent(String replyToken, PostbackEvent event, PostbackContent content) {

        String data = content.getData();
        switch (data){
            case "Date1" :
                String date = content.getParams().get("date");
                this.replyText(replyToken, "you choose date " + date);
                break;
            default:
                String json = env.getProperty("ouybot." + data);
                if(json != null) {
                    try {
                        replyObject(replyToken, json);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    this.replyText(replyToken, "Unknow command : " + data);
                }
        }


    }


    ////////// Reply

    private void replyText(@NonNull  String replyToken, @NonNull String message) {
        if(replyToken.isEmpty()) {
            throw new IllegalArgumentException("replyToken is not empty");
        }

        if(message.length() > 1000) {
            message = message.substring(0, 1000 - 2) + "...";
        }
        this.reply(replyToken, new TextMessage(message));
    }

    private void reply(@NonNull String replyToken, @NonNull Message message) {
        reply(replyToken, Collections.singletonList(message));
    }

    private void reply(@NonNull String replyToken, @NonNull List<Message> messages) {
        try {
            BotApiResponse response = lineMessagingClient.replyMessage(
                    new ReplyMessage(replyToken, messages)
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void replyObject(String replyToken, String json) throws IOException {
        Map<String, Object> map = jsonUtil.json2Map(json);

        String type = (String)map.get("type");
        switch (type){
            case "template" : {
                Message message = createTemplateMessage(map);
                ReplyMessage replyMessage = new ReplyMessage(replyToken, message);
                lineMessagingClient.replyMessage(replyMessage);
                break;
            }
            case "image" : {
                Message message = createImageMessage(map);
                ReplyMessage replyMessage = new ReplyMessage(replyToken, message);
                lineMessagingClient.replyMessage(replyMessage);
                break;
            }
            case "sticker" : {
                Message message = createStickerMessage(map);
                ReplyMessage replyMessage = new ReplyMessage(replyToken, message);
                lineMessagingClient.replyMessage(replyMessage);
                break;
            }
            case "location" : {
                Message message = createLocationMessage(map);
                ReplyMessage replyMessage = new ReplyMessage(replyToken, message);
                lineMessagingClient.replyMessage(replyMessage);
                break;
            }
        }

    }


    /////// Create Template

    protected Message createTemplateMessage(Map<String, Object> map) throws IOException {
        String altText = (String)map.get("altText");
        Map<String, Object> templateMap = (Map)map.get("template");
        String templateType = (String)templateMap.get("type");
        Template template = null;
        switch (templateType){
            case "carousel" :
                template = (CarouselTemplate)jsonUtil.map2Object(templateMap, CarouselTemplate.class);
                break;
            case "buttons" :
                template = (ButtonsTemplate)jsonUtil.map2Object(templateMap, ButtonsTemplate.class);
                break;
            case "confirm" :
                template = (ConfirmTemplate)jsonUtil.map2Object(templateMap, ConfirmTemplate.class);
                break;
        }
        Message message = new TemplateMessage(altText, template );
        return message;
    }

    protected Message createImageMessage(Map<String, Object> map) throws IOException {
        ImageMessage message = (ImageMessage)jsonUtil.map2Object(map, ImageMessage.class);
        return message;
    }

    protected Message createStickerMessage(Map<String, Object> map) throws IOException {
        StickerMessage message = (StickerMessage)jsonUtil.map2Object(map, StickerMessage.class);
        return message;
    }

    protected Message createLocationMessage(Map<String, Object> map) throws IOException {
        LocationMessage message = (LocationMessage)jsonUtil.map2Object(map, LocationMessage.class);
        return message;
    }

    //////////////////////////  Other

    private void system(String... args) {
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        try {
            Process start = processBuilder.start();
            int i = start.waitFor();
            log.info("result: {} => {}", Arrays.toString(args), i);
        } catch (InterruptedException e) {
            log.info("Interrupted", e);
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static DownloadedContent saveContent(String ext, MessageContentResponse response) {
        log.info("Content-type: {}", response);
        DownloadedContent tempFile = createTempFile(ext);
        try (OutputStream outputStream = Files.newOutputStream(tempFile.path)) {
            ByteStreams.copy(response.getStream(), outputStream);
            log.info("Save {}: {}", ext, tempFile);
            return tempFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static DownloadedContent createTempFile(String ext) {
        String fileName = LocalDateTime.now() + "-" + UUID.randomUUID().toString() + "." + ext;
        Path tempFile = Application.downloadedContentDir.resolve(fileName);
        tempFile.toFile().deleteOnExit();
        return new DownloadedContent(tempFile, createUri("/downloaded/" + tempFile.getFileName()));

    }

    private static String createUri(String path) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(path).toUriString();
    }

    @Value
    public static class DownloadedContent {
        Path path;
        String uri;
    }
}
