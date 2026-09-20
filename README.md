# Meizu MYVU App

Aplicativo Android completo para controlar óculos de realidade aumentada **Meizu MYVU / StarV Air2**.

## Funcionalidades Implementadas

### 🤖 Assistente IA
- **Claude** (Anthropic) - Modelo Haiku para respostas rápidas
- **OpenAI** - GPT-4o mini para conversação
- **Groq** - Llama 3 para respostas ultra-rápidas
- **Local** - Compatível com LM Studio, Ollama, etc.
- Modo contínuo de conversação
- Histórico de conversas

### 🗺️ Navegação GPS
- Rotas com **Google Directions API**
- Turn-by-turn nos óculos
- Busca de locais próximos
- Modos: carro, bicicleta, a pé

### 🌐 Tradutor em Tempo Real
- **11 idiomas** suportados
- Modo conversação (dois falantes)
- Tradução de documentos
- Integrado com LLM para tradução natural

### 🏠 Casa Inteligente
- **Home Assistant** - Integração completa
- **Tuya** - Suporte básico
- Controle de luzes, interruptores, clima
- Monitoramento em tempo real

### ❤️ Monitoramento de Saúde
- Contador de passos
- Frequência cardíaca (se disponível)
- Cálculo de calorias e distância
- Alertas de batimentos altos/baixos

### 📜 Teleprompter
- Importação de arquivos TXT
- Navegação por páginas
- Velocidade ajustável
- Busca de texto

### 🎵 Controle de Música
- Play/pause, próxima/anterior
- Espelhamento da música atual nos óculos
- Informações da faixa em tempo real

## Estrutura do Projeto

```
app/src/main/java/com/yourapp/myvu/
├── ai/
│   ├── LlmProviders.kt      # Claude, OpenAI, Groq, Local
│   └── AiAssistant.kt        # Assistente de voz
├── navigation/
│   └── MyvuNavigation.kt     # Navegação GPS
├── translation/
│   └── MyvuTranslator.kt     # Tradutor em tempo real
├── health/
│   └── MyvuHealthMonitor.kt  # Monitoramento de saúde
├── music/
│   └── MyvuMusicManager.kt   # Controle de música
├── smart_home/
│   └── MyvuSmartHome.kt      # Casa inteligente
├── teleprompter/
│   └── TeleprompterManager.kt # Teleprompter
├── service/
│   └── MyvuService.kt        # Serviço Bluetooth
├── ui/
│   ├── HomeFragment.kt       # Tela principal
│   ├── NavigationFragment.kt # Navegação
│   ├── AssistantFragment.kt  # Assistente IA
│   └── NotificationsFragment.kt # Notificações
├── utils/
│   └── ConfigManager.kt      # Configurações
├── MainActivity.kt           # Activity principal
└── MyvuApp.kt                # Application
```

## Configuração

### 1. Chaves de API Necessárias

| Serviço | Onde obter | Variável |
|---------|------------|----------|
| Claude | console.anthropic.com | `CLAUDE_API_KEY` |
| OpenAI | platform.openai.com | `OPENAI_API_KEY` |
| Groq | console.groq.com | `GROQ_API_KEY` |
| Google Maps | console.cloud.google.com | `GOOGLE_MAPS_KEY` |
| Home Assistant | Configurações > Segurança | `HOME_ASSISTANT_TOKEN` |

### 2. Configurar no App

Acesse **Configurações** no app e preencha:

```kotlin
// Exemplo de configuração
configManager.saveConfig(AppConfig(
    llmProvider = "claude",
    llmApiKey = "sk-ant-...",
    googleMapsKey = "AIza...",
    homeAssistantUrl = "http://192.168.1.100:8123",
    homeAssistantToken = "eyJ..."
))
```

### 3. Compilar e Instalar

```bash
# No Codespace ou Android Studio
./gradlew assembleDebug

# Instalar via ADB
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Comandos de Voz

| Comando | Ação |
|---------|------|
| "Olá" | Ativa assistente |
| "Navegar para [local]" | Inicia navegação |
| "Traduzir [texto]" | Traduz texto |
| "Próxima música" | Avança faixa |
| "Pausar" | Pausa reprodução |
| "Status" | Mostra resumo |

## Permissões Necessárias

```xml
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
```

## Compatibilidade

| Dispositivo | Status |
|-------------|--------|
| Meizu MYVU (StarV Air 1) | ✅ Testado |
| StarV Air2 | ✅ Testado |
| StarV View | ⚠️ Não testado |
| Outros AR | ❌ Não suportado |

## Links Úteis

- [SDK Original](https://github.com/Panny777/Meizu-Myvu-SDK)
- [Protocolo](https://github.com/Panny777/Meizu-Myvu-SDK/blob/main/PROTOCOL.md)
- [Meizu Developer](https://open.flyme.cn/)

## Licença

MIT License
