# Jarvis Android (projeto inicial)

Este é um projeto Android Studio com:
- Detecção da palavra de ativação **Jarvis** usando Porcupine.
- Resposta falada em português.
- Reconhecimento de comandos após a chamada.
- Comandos simples: horas, abrir TikTok, salvar e mostrar anotações.
- Botões para iniciar e parar a escuta.

## Requisitos
- Android Studio com Android SDK 35 e JDK suportado pelo Android Gradle Plugin.
- Conta Picovoice e AccessKey (a criação de chave é gratuita, sujeita aos termos da Picovoice).
- Microfone permitido no Android.

## Configuração
1. Abra o projeto no Android Studio.
2. Abra `app/src/main/java/com/example/jarvis/MainActivity.java`.
3. Substitua `COLE_SUA_ACCESS_KEY_AQUI` pela sua AccessKey privada obtida em https://console.picovoice.ai/.
4. Sincronize o Gradle e execute em seu Android.
5. Permita acesso ao microfone.

## Importante
- Este arquivo é o **código-fonte**, não um APK instalado.
- O detector Porcupine requer AccessKey e usa a palavra integrada `JARVIS`. Consulte a documentação oficial.
- O reconhecimento de comandos usa o reconhecedor de fala do Android; disponibilidade offline depende do aparelho e dos pacotes de idioma instalados.
- Esta primeira versão não roda como serviço permanente em segundo plano. Mantenha o app aberto enquanto testa.
- A versão inicial não importa automaticamente seu script Termux. Ela recria alguns comandos básicos dentro do app.
- Nunca compartilhe sua AccessKey.
