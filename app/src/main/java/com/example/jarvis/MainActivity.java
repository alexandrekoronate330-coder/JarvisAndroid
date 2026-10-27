package com.example.jarvis;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView status;
    private TextView log;
    private Button startButton, stopButton;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean started = false;
    private SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("jarvis_memory", MODE_PRIVATE);
        buildUi();

        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("pt", "BR"));
            }
        });

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO}, 10);
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            writeLog("O reconhecimento de fala não está disponível.");
        }
    }

    private void buildUi() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(28, 24, 28, 24);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setBackgroundColor(0xFF101820);

        TextView title = new TextView(this);
        title.setText("JARVIS");
        title.setTextSize(32);
        title.setTextColor(0xFF7DE3FF);
        title.setGravity(Gravity.CENTER);
        layout.addView(title,
                new LinearLayout.LayoutParams(-1, -2));

        // IMAGEM DO JARVIS
        ImageView avatar = new ImageView(this);
        avatar.setImageResource(R.drawable.jarvis_avatar);
        avatar.setScaleType(ImageView.ScaleType.FIT_CENTER);

        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(240, 240);
        imageParams.setMargins(0, 12, 0, 12);
        layout.addView(avatar, imageParams);

        status = new TextView(this);
        status.setText("Pronto para falar");
        status.setTextSize(17);
        status.setTextColor(0xFFFFFFFF);
        status.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams sp =
                new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, 8, 0, 18);
        layout.addView(status, sp);

        startButton = new Button(this);
        startButton.setText("Iniciar escuta");
        layout.addView(startButton,
                new LinearLayout.LayoutParams(-1, -2));

        stopButton = new Button(this);
        stopButton.setText("Parar escuta");
        stopButton.setEnabled(false);
        layout.addView(stopButton,
                new LinearLayout.LayoutParams(-1, -2));

        Button mangaButton = new Button(this);
        mangaButton.setText("Criar mangá");
        layout.addView(mangaButton,
                new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(this);
        log = new TextView(this);
        log.setTextColor(0xFFE6F1F5);
        log.setTextSize(15);
        log.setPadding(8, 18, 8, 18);
        scroll.addView(log);

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(-1, 0, 1);
        lp.setMargins(0, 10, 0, 0);
        layout.addView(scroll, lp);

        setContentView(layout);

        startButton.setOnClickListener(v -> startListening());
        stopButton.setOnClickListener(v -> stopListening());

        mangaButton.setOnClickListener(v ->
                startActivity(new Intent(
                        MainActivity.this, MangaActivity.class)));
    }

    private void startListening() {
        if (started) return;

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO}, 10);
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            writeLog("Reconhecimento de fala indisponível.");
            return;
        }

        started = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        status.setText("Preparando microfone...");
        writeLog("Iniciando escuta.");

        handler.postDelayed(this::listenForCommand, 200);
    }

    private void listenForCommand() {
        if (!started) return;

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        speechRecognizer =
                SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizer.setRecognitionListener(
                new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                status.setText("Ouvindo comando...");
            }

            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                status.setText("Processando...");
            }

            @Override
            public void onError(int error) {
                if (started) {
                    writeLog("Não entendi. Tente novamente.");
                    finishListening();
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches =
                        results.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION);

                if (matches != null && !matches.isEmpty()) {
                    handleCommand(matches.get(0));
                } else {
                    finishListening();
                }
            }

            @Override public void onPartialResults(Bundle results) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        Intent intent =
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

        speechRecognizer.startListening(intent);
    }

    private void handleCommand(String raw) {
        String command = raw.toLowerCase(new Locale("pt", "BR"));
        writeLog("Você disse: " + raw);

        if (command.contains("hora")) {
            String hora = new SimpleDateFormat(
                    "HH:mm", new Locale("pt", "BR"))
                    .format(new Date());
            speak("Agora são " + hora);

        } else if (command.contains("abrir tik tok")
                || command.contains("abrir tiktok")
                || command.contains("abre tik tok")
                || command.contains("abre tiktok")) {

            Intent launch = getPackageManager()
                    .getLaunchIntentForPackage(
                            "com.zhiliaoapp.musically");

            if (launch == null) {
                launch = getPackageManager()
                        .getLaunchIntentForPackage(
                                "com.ss.android.ugc.trill");
            }

            if (launch != null) {
                startActivity(launch);
                speak("Abrindo TikTok.");
            } else {
                speak("Não encontrei o TikTok instalado.");
            }

        } else if (command.startsWith("lembre ")
                || command.startsWith("lembre que ")
                || command.startsWith("anote ")
                || command.startsWith("anota ")) {

            String note = raw.replaceFirst(
                    "(?i)^(lembre que|lembre|anote|anota)\\s+",
                    "").trim();

            if (!note.isEmpty()) {
                String old = prefs.getString("notes", "");
                prefs.edit()
                        .putString("notes", old + "\n• " + note)
                        .apply();
                speak("Anotação salva.");
            } else {
                speak("Diga o que devo lembrar.");
            }

        } else if (command.contains("minhas anotacoes")
                || command.contains("minhas anotações")
                || command.contains("o que eu pedi para lembrar")) {

            String notes = prefs.getString("notes", "");
            speak(notes.isEmpty()
                    ? "Você não tem anotações salvas."
                    : "Suas anotações estão na tela.");
            writeLog("Anotações:" + notes);

        } else if (command.contains("parar escuta")
                || command.contains("desligar jarvis")
                || command.equals("sair")) {

            speak("Parando a escuta.");
            stopListening();
            return;

        } else {
            speak("Ainda não conheço esse comando.");
        }

        finishListening();
    }

    private void finishListening() {
        started = false;

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        status.setText("Pronto. Aperte Iniciar escuta.");
    }

    private void speak(String text) {
        writeLog("JARVIS: " + text);

        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH,
                    null, "jarvis_tts");
        }
    }

    private void writeLog(String text) {
        if (log != null) {
            log.append(text + "\n");
        }
    }

    private void stopListening() {
        started = false;

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        if (startButton != null) startButton.setEnabled(true);
        if (stopButton != null) stopButton.setEnabled(false);
        if (status != null) status.setText("Escuta parada");
    }

    @Override
    protected void onDestroy() {
        stopListening();

        if (tts != null) {
            tts.shutdown();
        }

        super.onDestroy();
    }
}
