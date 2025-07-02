package com.lis;

import com.lis.util.HttpSender;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import javax.swing.*;
import java.io.File;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecione a pasta a ser monitorada");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) {
            logger.info("Nenhuma pasta selecionada. Encerrando o programa.");
            return;
        }

        File selectedDirectory = chooser.getSelectedFile();
        Path path = selectedDirectory.toPath();

        String serverUrl = System.getenv("HL7_ENDPOINT_URL");
        if (serverUrl == null || serverUrl.isEmpty()) {
            System.err.println("Erro: A variável de ambiente HL7_ENDPOINT_URL não está definida.");
            return;
        }

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            logger.info("Iniciando monitoramento da pasta: {}", path);
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            boolean running = true;
            while (running) {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path filePath = path.resolve((Path) event.context());

                        if (!filePath.toString().toLowerCase().endsWith(".hl7")) {
                            logger.warn("Ignorando arquivo não .hl7: {}", filePath);
                            continue;
                        }

                        logger.info("Novo arquivo detectado: {}", filePath);
                        processFile(filePath, serverUrl);
                    }
                }

                boolean valid = key.reset();
                if (!valid) running = false;
            }

        } catch (IOException | InterruptedException e) {
            logger.error("Erro no monitoramento da pasta", e);
        }
    }

    private static void processFile(Path filePath, String serverUrl) {
        try {
            byte[] bytes = Files.readAllBytes(filePath);
            String content = new String(bytes, StandardCharsets.UTF_8)
                    .replace("\n", "\r")  // Garantir compatibilidade com HL7 (usar CR)
                    .replace("\r\r", "\r"); // Evitar CR duplicado

            String jsonPayload = String.format("{\"hl7\": \"%s\"}", content.replace("\"", "\\\""));

            HttpSender.sendToServer(serverUrl, jsonPayload);
            logger.info("Arquivo processado e enviado: {}", filePath);
        } catch (IOException e) {
            logger.error("Erro ao ler o arquivo: {}", filePath, e);
        } catch (Exception e) {
            logger.error("Erro ao enviar o arquivo: {}", filePath, e);
        }
    }
}