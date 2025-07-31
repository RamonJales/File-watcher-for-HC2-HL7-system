package com.lis;

import com.lis.util.HttpSender;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.json.JSONObject;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);
    private static final String ENV_FILENAME = ".env";
    private static Dotenv dotenv;

    public static void main(String[] args) {
        logger.info("Aplicação LIS File Watcher iniciada.");
        // 1. Tentar carregar .env do diretório do JAR
        try {
            dotenv = Dotenv.configure()
                    .directory(".")
                    .filename(ENV_FILENAME)
                    .ignoreIfMissing()
                    .load();

            // 2. Se não encontrou, tenta no home do usuário
            if (dotenv.get("HL7_WATCH_FOLDER") == null || dotenv.get("EMPRESA_ID") == null) {
                dotenv = Dotenv.configure()
                        .directory(System.getProperty("user.home") + File.separator + ".lis-watcher")
                        .filename(ENV_FILENAME)
                        .ignoreIfMissing()
                        .load();
            }
        } catch (Exception e) {
            logger.warn("Erro ao carregar .env: {}", e.getMessage());
        }

        String watchPath = dotenv.get("HL7_WATCH_FOLDER");
        String empresaIdStr = dotenv.get("EMPRESA_ID");
        String serverUrl = dotenv.get("HL7_ENDPOINT_URL");

        // Se faltar algum valor, pede via Swing e salva no .env
        if (watchPath == null || empresaIdStr == null || serverUrl == null || Strings.isBlank(watchPath) || Strings.isBlank(empresaIdStr) || Strings.isBlank(serverUrl)) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Selecione a pasta a ser monitorada");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);

            int result = chooser.showOpenDialog(null);
            if (result != JFileChooser.APPROVE_OPTION) {
                logger.info("Nenhuma pasta selecionada. Encerrando.");
                return;
            }

            File selectedDirectory = chooser.getSelectedFile();
            watchPath = selectedDirectory.getAbsolutePath();

            empresaIdStr = JOptionPane.showInputDialog(null, "Digite o ID da empresa:");
            if (empresaIdStr == null || Strings.isBlank(empresaIdStr)) {
                logger.info("Nenhum ID informado. Encerrando.");
                return;
            }

            serverUrl = JOptionPane.showInputDialog(null, "Digite a URL do endpoint HL7:");
            if (serverUrl == null || Strings.isBlank(serverUrl)) {
                logger.info("Nenhuma URL informada. Encerrando.");
                return;
            }

            // Salva .env no user.home
            saveEnvToFile(System.getProperty("user.home") + File.separator + ".lis-watcher" + File.separator + ENV_FILENAME,
                    watchPath, empresaIdStr, serverUrl);
        }

        Path path = Paths.get(watchPath);
        long empresaId = Long.parseLong(empresaIdStr);

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            logger.info("Monitorando a pasta: {}", path);
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            while (true) {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path filePath = path.resolve((Path) event.context());

                        if (!filePath.toString().toLowerCase().endsWith(".hl7")) {
                            logger.warn("Ignorando arquivo não .hl7: {}", filePath);
                            continue;
                        }

                        logger.info("Novo arquivo detectado: {}", filePath);
                        processFile(filePath, serverUrl, empresaId, watchPath);
                    }
                }

                boolean valid = key.reset();
                if (!valid) break;
            }

        } catch (IOException | InterruptedException e) {
            logger.error("Erro no monitoramento da pasta", e);
        }
    }

    private static void saveEnvToFile(String filePath, String folder, String empresaId, String url) {
        try {
            File envFile = new File(filePath);
            File parentDir = envFile.getParentFile();

            if (!parentDir.exists()) {
                parentDir.mkdirs(); // cria ~/.lis-watcher se não existir
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(envFile))) {
                writer.println("HL7_WATCH_FOLDER=" + folder);
                writer.println("EMPRESA_ID=" + empresaId);
                writer.println("HL7_ENDPOINT_URL=" + url);
                logger.info(".env salvo em {}", filePath);
            }
        } catch (IOException e) {
            logger.error("Erro ao salvar .env: {}", e.getMessage());
        }
    }

    private static void processFile(Path filePath, String serverUrl, Long empresaId, String watchPath) {
        Path baseDir = filePath.resolve(watchPath);
        Path processingDir = baseDir.resolve("processamento");
        Path errorDir = baseDir.resolve("erro");

        try {
            // Garante que as pastas de processamento e erro existem
            Files.createDirectories(processingDir);
            Files.createDirectories(errorDir);

            // Move o arquivo para a pasta "processamento"
            Path processingFile = processingDir.resolve(filePath.getFileName());
            Files.move(filePath, processingFile, StandardCopyOption.REPLACE_EXISTING);

            // Lê e prepara o conteúdo do HL7
            byte[] bytes = Files.readAllBytes(processingFile);
            String content = new String(bytes, StandardCharsets.UTF_8)
                    .replace("\n", "\r")
                    .replace("\r\r", "\r");

            // Monta o JSON
            JSONObject json = new JSONObject();
            json.put("hl7", content);
            json.put("empresaId", empresaId);

            // Envia a requisição HTTP
            HttpSender.sendToServer(serverUrl, json.toString());

            // Sucesso: remove o arquivo da pasta de processamento
            Files.deleteIfExists(processingFile);
            logger.info("Arquivo processado e removido com sucesso: {}", processingFile);

        } catch (Exception e) {
            logger.error("Erro ao processar o arquivo: {}", filePath, e);

            // Em caso de erro: move o arquivo original para a pasta de erro
            try {
                Path errorFile = errorDir.resolve(filePath.getFileName());
                // Se já tiver sido movido para 'processamento', pega de lá
                Path currentFile = processingDir.resolve(filePath.getFileName());
                if (Files.exists(currentFile)) {
                    Files.move(currentFile, errorFile, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.move(filePath, errorFile, StandardCopyOption.REPLACE_EXISTING);
                }
                logger.warn("Arquivo movido para a pasta de erro: {}", errorFile);
            } catch (IOException ex) {
                logger.error("Falha ao mover o arquivo para a pasta de erro", ex);
            }
        }
    }
}