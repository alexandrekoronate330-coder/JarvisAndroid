package com.example.jarvis;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.content.SharedPreferences;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class AnimeActivity extends Activity {
    private SharedPreferences prefs;
    private EditText titleInput, genreInput, charactersInput, ideaInput, episodeOutput;
    private TextView status;
    private JSONObject projects = new JSONObject();
    private String currentTitle = "";
    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private EditText field(String hint, int lines) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(0xFFB8C7D0);
        e.setTextSize(16);
        e.setMinLines(lines);
        e.setGravity(Gravity.TOP);
        e.setPadding(dp(12), dp(8), dp(12), dp(8));
        e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF7DE3FF));
        return e;
    }

    private Button button(LinearLayout layout, String text, Runnable action) {
        Button b = new Button(this);
        b.setText(text);
        layout.addView(b);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private String val(EditText e) {
        return e.getText().toString().trim();
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        // Reutiliza a preferência da Groq, mas mantém os projetos de Anime separados.
        prefs = getSharedPreferences("jarvis_manga_projects", MODE_PRIVATE);
        try {
            projects = new JSONObject(prefs.getString("anime_projects_json", "{}"));
        } catch (Exception e) {
            projects = new JSONObject();
        }

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), dp(20), dp(18), dp(24));
        layout.setBackgroundColor(0xFF101820);
        scroll.addView(layout);

        TextView heading = new TextView(this);
        heading.setText("JARVIS ANIME");
        heading.setTextColor(0xFF7DE3FF);
        heading.setTextSize(28);
        heading.setGravity(Gravity.CENTER);
        layout.addView(heading);

        TextView intro = new TextView(this);
        intro.setText("Crie uma história pelo título e pela ideia. Gere episódios e continue do ponto onde parou.");
        intro.setTextColor(Color.WHITE);
        intro.setTextSize(16);
        intro.setPadding(0, dp(12), 0, dp(12));
        layout.addView(intro);

        titleInput = field("Título do anime", 1);
        genreInput = field("Gênero (fantasia, ação, isekai...)", 1);
        charactersInput = field("Personagens principais", 2);
        ideaInput = field("Ideia da história e acontecimentos", 4);
        layout.addView(titleInput);
        layout.addView(genreInput);
        layout.addView(charactersInput);
        layout.addView(ideaInput);

        button(layout, "CRIAR / ABRIR ANIME", this::createOrOpen);
        button(layout, "SALVAR HISTÓRIA", this::saveProject);
        button(layout, "GERAR EPISÓDIO COM IA", this::generateEpisode);
        button(layout, "VER / EDITAR EPISÓDIOS", this::showEpisodes);
        button(layout, "CONFIGURAR CHAVE DA IA", this::configureKey);

        status = new TextView(this);
        status.setTextColor(0xFF7DE3FF);
        status.setTextSize(15);
        status.setPadding(0, dp(12), 0, dp(12));
        layout.addView(status);

        TextView outLabel = new TextView(this);
        outLabel.setText("ROTEIRO DO EPISÓDIO");
        outLabel.setTextColor(0xFF7DE3FF);
        outLabel.setTextSize(18);
        layout.addView(outLabel);

        episodeOutput = field("O episódio gerado aparecerá aqui. Também podes editar o texto.", 12);
        episodeOutput.setMinHeight(dp(250));
        layout.addView(episodeOutput);
        button(layout, "SALVAR EDIÇÕES DO EPISÓDIO", this::saveEditedEpisode);

        setContentView(scroll);
    }

    private void persist() {
        prefs.edit().putString("anime_projects_json", projects.toString()).apply();
    }

    private JSONObject currentProject() {
        if (currentTitle.isEmpty()) return null;
        return projects.optJSONObject(currentTitle);
    }

    private void createOrOpen() {
        String title = val(titleInput);
        if (title.isEmpty()) {
            titleInput.setError("Escreve o título do anime");
            return;
        }

        JSONObject p = projects.optJSONObject(title);
        if (p == null) {
            p = new JSONObject();
            try {
                p.put("title", title);
                p.put("genre", val(genreInput));
                p.put("characters", val(charactersInput));
                p.put("idea", val(ideaInput));
                p.put("episodes", new JSONArray());
                projects.put(title, p);
            } catch (Exception ignored) {}
        } else {
            genreInput.setText(p.optString("genre", ""));
            charactersInput.setText(p.optString("characters", ""));
            ideaInput.setText(p.optString("idea", ""));
        }
        currentTitle = title;
        persist();
        status.setText("Anime aberto: " + title + " | Episódios salvos: "
            + p.optJSONArray("episodes").length());
        Toast.makeText(this, "Projeto salvo neste celular", Toast.LENGTH_SHORT).show();
    }

    private void saveProject() {
        if (currentTitle.isEmpty()) {
            createOrOpen();
            if (currentTitle.isEmpty()) return;
        }
        JSONObject p = currentProject();
        if (p == null) return;
        try {
            p.put("genre", val(genreInput));
            p.put("characters", val(charactersInput));
            p.put("idea", val(ideaInput));
            persist();
            status.setText("História salva: " + currentTitle);
            Toast.makeText(this, "História salva", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            status.setText("Erro ao salvar: " + e.getMessage());
        }
    }

    private void configureKey() {
        EditText keyInput = field("Chave da Groq (gsk_...)", 1);
        keyInput.setSingleLine(true);
        keyInput.setText(prefs.getString("groq_api_key", ""));
        new AlertDialog.Builder(this)
            .setTitle("Conectar IA de histórias")
            .setMessage("A chave é guardada nas preferências locais do aplicativo.")
            .setView(keyInput)
            .setPositiveButton("SALVAR", (d, w) -> {
                String key = val(keyInput);
                if (key.isEmpty()) {
                    status.setText("Nenhuma chave informada.");
                } else {
                    prefs.edit().putString("groq_api_key", key).apply();
                    status.setText("Chave da IA salva.");
                }
            })
            .setNegativeButton("CANCELAR", null)
            .show();
    }

    private void generateEpisode() {
        if (currentTitle.isEmpty()) {
            Toast.makeText(this, "Cria ou abre um anime primeiro", Toast.LENGTH_LONG).show();
            return;
        }
        saveProject();
        String apiKey = prefs.getString("groq_api_key", "").trim();
        if (apiKey.isEmpty()) {
            configureKey();
            status.setText("Configura a chave da Groq para gerar episódios.");
            return;
        }

        JSONObject p = currentProject();
        if (p == null) return;
        JSONArray episodes = p.optJSONArray("episodes");
        if (episodes == null) episodes = new JSONArray();
        final JSONArray savedEpisodes = episodes;
        final int number = episodes.length() + 1;
        StringBuilder previous = new StringBuilder();
        int start = Math.max(0, episodes.length() - 3);
        for (int i = start; i < episodes.length(); i++) {
            JSONObject ep = episodes.optJSONObject(i);
            if (ep != null) {
                previous.append("\n\nEPISÓDIO ").append(ep.optInt("number"))
                    .append(": ").append(ep.optString("text"));
            }
        }

        String prompt = "Crie o episódio " + number + " do anime \"" + currentTitle + "\".\n"
            + "Gênero: " + p.optString("genre") + "\n"
            + "Personagens: " + p.optString("characters") + "\n"
            + "Ideia e história oficial: " + p.optString("idea") + "\n"
            + "Episódios anteriores para manter continuidade: " + previous + "\n\n"
            + "Escreva em português brasileiro. Produza um roteiro de anime completo, "
            + "com título, cenas numeradas, descrição visual das ações, diálogos naturais, "
            + "emoções e sons. Preserve nomes, poderes, relações e acontecimentos anteriores. "
            + "Não reinicie a história nem contradiga os episódios anteriores. "
            + "Se for o primeiro episódio, apresente o mundo e os protagonistas. "
            + "Termine com um gancho forte para o próximo episódio. "
            + "Não afirme que criou um vídeo: gere apenas o roteiro.";

        status.setText("A IA está escrevendo o episódio " + number + "... aguarda.");
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(120000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                JSONObject body = new JSONObject();
                body.put("model", "openai/gpt-oss-20b");
                body.put("temperature", 0.85);
                body.put("max_tokens", 4500);
                JSONArray messages = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                msg.put("content", prompt);
                messages.put(msg);
                body.put("messages", messages);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                InputStream stream = code >= 200 && code < 300
                    ? conn.getInputStream() : conn.getErrorStream();
                String response = readAll(stream);
                if (code < 200 || code >= 300) {
                    throw new Exception("Erro da Groq (" + code + "): " + response);
                }

                JSONObject json = new JSONObject(response);
                String text = json.getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("message").getString("content");

                JSONObject ep = new JSONObject();
                ep.put("number", number);
                ep.put("text", text);
                savedEpisodes.put(ep);
                p.put("episodes", savedEpisodes);
                persist();

                runOnUiThread(() -> {
                    episodeOutput.setText(text);
                    status.setText("Episódio " + number + " gerado e salvo!");
                });
            } catch (Exception e) {
                String error = e.getMessage() == null ? "Erro desconhecido" : e.getMessage();
                runOnUiThread(() -> status.setText("Não foi possível gerar: " + error));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private void showEpisodes() {
        JSONObject p = currentProject();
        if (p == null) {
            Toast.makeText(this, "Abre um anime primeiro", Toast.LENGTH_SHORT).show();
            return;
        }
        JSONArray eps = p.optJSONArray("episodes");
        if (eps == null || eps.length() == 0) {
            status.setText("Ainda não existem episódios salvos.");
            return;
        }
        String[] names = new String[eps.length()];
        for (int i = 0; i < eps.length(); i++) {
            JSONObject ep = eps.optJSONObject(i);
            names[i] = "Episódio " + (ep == null ? i + 1 : ep.optInt("number", i + 1));
        }
        new AlertDialog.Builder(this)
            .setTitle("Episódios de " + currentTitle)
            .setItems(names, (dialog, which) -> {
                JSONObject ep = eps.optJSONObject(which);
                if (ep != null) {
                    episodeOutput.setText(ep.optString("text", ""));
                    status.setText(names[which] + " aberto para leitura e edição.");
                }
            })
            .setNegativeButton("FECHAR", null)
            .show();
    }

    private void saveEditedEpisode() {
        JSONObject p = currentProject();
        if (p == null) {
            Toast.makeText(this, "Abre um anime primeiro", Toast.LENGTH_SHORT).show();
            return;
        }
        JSONArray eps = p.optJSONArray("episodes");
        if (eps == null || eps.length() == 0) {
            status.setText("Gera o primeiro episódio antes de salvar edições.");
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Salvar edição")
            .setMessage("Salvar o texto atual no episódio mais recente?")
            .setPositiveButton("SALVAR", (d, w) -> {
                try {
                    JSONObject last = eps.getJSONObject(eps.length() - 1);
                    last.put("text", val(episodeOutput));
                    p.put("episodes", eps);
                    persist();
                    status.setText("Edição salva no episódio " + last.optInt("number") + ".");
                } catch (Exception e) {
                    status.setText("Erro ao salvar a edição.");
                }
            })
            .setNegativeButton("CANCELAR", null)
            .show();
    }
}
