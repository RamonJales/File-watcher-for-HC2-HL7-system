# 🩺 HL7 File Monitor

Este programa foi desenvolvido para monitorar arquivos HL7 gerados por máquinas de captura híbrida da QIAGEN HC2 — um equipamento utilizado para análise molecular e diagnósticos. Sempre que um novo arquivo .hl7 é criado na pasta monitorada, o programa lê o conteúdo completo do arquivo e envia uma requisição HTTP para um endpoint específico, que é responsável por processar essa mensagem HL7 e integrar os dados ao sistema de informação hospitalar ou laboratório.

---

## 🧩 Contextualização e propósito

O HL7 é um padrão amplamente utilizado para troca eletrônica de informações médicas entre sistemas hospitalares e laboratoriais. A QIAGEN HC2 gera arquivos HL7 contendo resultados de exames que precisam ser integrados automaticamente a sistemas centrais.

Este programa automatiza essa etapa, monitorando a pasta onde os arquivos são salvos e enviando as mensagens para o endpoint configurado, evitando a necessidade de intervenção manual e garantindo agilidade e confiabilidade na integração dos dados.

---


## ✨ Funcionalidades

- ✅ Interface gráfica (Swing) para seleção da pasta e configuração inicial
- ✅ Envio automático do conteúdo `.hl7` para um servidor via JSON
- ✅ Armazena variáveis de ambiente em um `.env` persistente
- ✅ Compatível com **Windows** e **Linux**
- ✅ Executável único via `shadowJar` (não precisa instalar dependências Java manualmente)

---

## 🧠 Como funciona

### 🟢 Primeira execução

Ao rodar o `.jar` pela primeira vez:

1. O programa abre uma **interface gráfica** pedindo:
    - A **pasta a ser monitorada**
    - O **ID da empresa**
    - A **URL do endpoint HL7**

2. Esses dados são salvos automaticamente em um arquivo chamado `.env`, que será carregado nas próximas execuções.

---

🔄 Ciclo de vida do arquivo .hl7

    📨 Cai em entrada/

    📥 É movido para processamento/

    🔄 É lido, transformado em JSON e enviado via HTTP

    ✅ Se sucesso → é deletado

    ❌ Se erro → é movido para erro/

---

### 📦 Onde o `.env` é salvo?

O programa tenta carregar o `.env` dos seguintes locais, **nesta ordem**:

1. **Diretório onde o `.jar` está**
2. **Diretório `home` do usuário** (`C:\Users\SeuNome` no Windows ou `/home/seunome` no Linux)

---

### 📁 Exemplo de `.env`

```env
HL7_WATCH_FOLDER=C:\Users\Public\QIAGEN\HC2 System Software\data\lis
EMPRESA_ID=12345
HL7_ENDPOINT_URL=http://localhost:8080/iPathos/site/receiveHL7
```

---

## ▶️ Como executar

### 💻 Windows

1. Clique duas vezes no arquivo `iniciar-monitoramento.bat` (fornecido junto do `.jar`)
2. OU abra o terminal (cmd), vá até a pasta e execute:

```bat
java -jar hl7-monitor.jar
```

> Se for a primeira vez, será aberta a janela para configuração.

---

### 🐧 Linux

1. Abra o terminal
2. Vá até a pasta onde está o `hl7-monitor.jar`
3. Execute:

```bash
java -jar hl7-monitor.jar
```

---

## 🔁 Executar automaticamente ao iniciar o computador

### Windows

1. Pressione `Win + R`, digite:

```
shell:startup
```

2. Copie um **atalho do arquivo `iniciar-monitoramento.bat`** para esta pasta.

> Pronto! O programa será iniciado toda vez que o Windows for ligado.

### Linux (usando `crontab`)

1. No terminal, execute:

```bash
crontab -e
```

2. Adicione a linha:

```bash
@reboot java -jar /caminho/para/hl7-monitor.jar
```

---

## 🛠️ Requisitos

- Java 8 ou superior instalado (`java -version` no terminal)
- Acesso à internet ou rede para enviar os arquivos

---

## 📦 Empacotamento com Shadow Jar

Este projeto usa o plugin `shadowJar`, o que significa que o `.jar` gerado já contém todas as dependências (incluindo `dotenv-java` e `log4j`).

Para gerar o `.jar` completo:

```bash
./gradlew shadowJar
```

O arquivo estará em:

```
build/libs/hl7-monitor.jar
```

---

## Contato
Para dúvidas ou sugestões, entre em contato:
- **GitHub**: [Ramon Jales](https://github.com/RamonJales/)
- **E-mail**: ramonjales123@gmail.com
