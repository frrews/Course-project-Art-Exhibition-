package com.bsuir.exhibition.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "gigachat")
public class GigachatProperties {


    private String oauthUrl = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";


    private String chatUrl = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions";


    private String authorizationKey = "";


    private String rqUid = "dea767c3-4e9e-46ad-8f7f-9def5b505503";

    private String model = "GigaChat";


    private boolean insecureSsl = true;
}
