package com.lis.util;


import org.apache.logging.log4j.core.util.internal.HttpInputStreamUtil;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.apache.logging.log4j.core.util.internal.HttpInputStreamUtil.readStream;

public class HttpSender {
    public static void sendToServer(String serverUrl, String jsonPayload) throws Exception {
        URL url = new URL(serverUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != 200 && responseCode != 201) {
            String error = Arrays.toString(HttpInputStreamUtil.readStream(connection.getErrorStream()));
            throw new RuntimeException(
                    "Erro ao enviar requisição. Código HTTP: " + responseCode +
                            " | Resposta: " + error
            );
        }
    }
}
