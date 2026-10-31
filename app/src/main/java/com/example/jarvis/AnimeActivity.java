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
    private int selectedEpisodeIndex = -1;
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
        button(layout, "GERAR PRÉVIA DE VÍDEO (5 SEGUNDOS)", this::generateVideoPreview);
        button(layout, "🎬 CRIAR ANIME DE 1 MINUTO", this::createOneMinuteAnime);

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
                selectedEpisodeIndex = savedEpisodes.length() - 1;

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
                    selectedEpisodeIndex = which;
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
        if (eps == null || selectedEpisodeIndex < 0
                || selectedEpisodeIndex >= eps.length()) {
            status.setText("Abre um episódio na lista antes de salvar a edição.");
            return;
        }

        JSONObject selected = eps.optJSONObject(selectedEpisodeIndex);
        if (selected == null) {
            status.setText("Não foi possível localizar o episódio selecionado.");
            return;
        }

        int number = selected.optInt("number", selectedEpisodeIndex + 1);

        new AlertDialog.Builder(this)
            .setTitle("Salvar edição")
            .setMessage("Salvar as alterações no episódio " + number + "?")
            .setPositiveButton("SALVAR", (d, w) -> {
                try {
                    selected.put("text", val(episodeOutput));
                    p.put("episodes", eps);
                    persist();
                    status.setText("Edição salva no episódio " + number + ".");
                    Toast.makeText(this, "Episódio atualizado", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    status.setText("Erro ao salvar a edição: " + e.getMessage());
                }
            })
            .setNegativeButton("CANCELAR", null)
            .show();
    }

    private void generateVideoPreview() {
        EditText keyInput = new EditText(this);
        keyInput.setHint("Chave da fal.ai");
        keyInput.setSingleLine(true);
        keyInput.setTextColor(Color.WHITE);

        String savedKey = prefs.getString("fal_api_key", "").trim();
        if (savedKey.isEmpty()) {
            new AlertDialog.Builder(this)
                .setTitle("Conectar gerador de vídeo")
                .setMessage("Cria uma chave na tua conta fal.ai. Não partilhes a chave no chat.")
                .setView(keyInput)
                .setPositiveButton("SALVAR E CONTINUAR", (d, w) -> {
                    String key = keyInput.getText().toString().trim();
                    if (key.isEmpty()) {
                        status.setText("Chave não informada.");
                        return;
                    }
                    prefs.edit().putString("fal_api_key", key).apply();
                    confirmVideoCost(key);
                })
                .setNegativeButton("CANCELAR", null)
                .show();
        } else {
            confirmVideoCost(savedKey);
        }
    }

    private void confirmVideoCost(String key) {
        new AlertDialog.Builder(this)
            .setTitle("Prévia de vídeo de 5 segundos")
            .setMessage("Será enviada uma geração de 5 segundos em 480p. A API pode cobrar pela geração. Continuar?")
            .setPositiveButton("GERAR", (d, w) -> requestVideoPreview(key))
            .setNegativeButton("CANCELAR", null)
            .show();
    }

    private void requestVideoPreview(String key) {
        String title = val(titleInput);
        String idea = val(ideaInput);
        String script = val(episodeOutput);
        if (title.isEmpty() || (idea.isEmpty() && script.isEmpty())) {
            status.setText("Preenche o título e a ideia ou gera um roteiro primeiro.");
            return;
        }

        String prompt = "Vertical 9:16 animated Japanese anime, polished 2D anime aesthetic, "
            + "fluid character movement, dramatic camera movement, expressive faces, cinematic lighting, "
            + "detailed background. Title: " + title + ". Story: " + idea
            + ". Scene: " + script
            + ". One coherent five-second scene, no subtitles, no text, no logos.";

        status.setText("Enviando a prévia de vídeo...");
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("prompt", prompt);
                body.put("duration", 5);
                body.put("resolution", "480P");
                body.put("aspect_ratio", "9:16");
                body.put("prompt_expansion_mode", "disabled");

                JSONObject queued = falRequest(
                    "https://queue.fal.run/minimax/h3-max-turbo/text-to-video",
                    key, "POST", body);

                String statusUrl = queued.optString("status_url", "");
                String resultUrl = queued.optString("response_url", "");
                if (statusUrl.isEmpty() || resultUrl.isEmpty())
                    throw new Exception("A API não devolveu os endereços da fila.");

                runOnUiThread(() -> status.setText("Pedido enviado. Renderizando a cena..."));

                boolean done = false;
                for (int i = 0; i < 60; i++) {
                    Thread.sleep(3000);
                    JSONObject check = falRequest(statusUrl, key, "GET", null);
                    String state = check.optString("status", "");
                    if ("COMPLETED".equals(state)) {
                        done = true;
                        break;
                    }
                    if ("FAILED".equals(state))
                        throw new Exception("O serviço informou falha na geração.");
                }
                if (!done) throw new Exception("Demorou demais. Tenta consultar novamente mais tarde.");

                JSONObject result = falRequest(resultUrl, key, "GET", null);
                JSONObject video = result.optJSONObject("video");
                if (video == null) throw new Exception("A resposta não contém vídeo.");
                String videoUrl = video.optString("url", "");
                if (videoUrl.isEmpty()) throw new Exception("A API não devolveu o link do vídeo.");

                runOnUiThread(() -> new AlertDialog.Builder(AnimeActivity.this)
                    .setTitle("Prévia pronta")
                    .setMessage("Vídeo de 5 segundos gerado. Abre o link para assistir. Ainda não é o vídeo final de 1 minuto.\n\n" + videoUrl)
                    .setPositiveButton("ABRIR VÍDEO", (d, w) -> {
                        try {
                            startActivity(new android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(videoUrl)));
                        } catch (Exception e) {
                            status.setText("Erro ao abrir vídeo: " + e.getMessage());
                        }
                    })
                    .setNeutralButton("COPIAR LINK", (d, w) -> {
                        android.content.ClipboardManager cm =
                            (android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("Vídeo JARVIS", videoUrl));
                        Toast.makeText(AnimeActivity.this, "Link copiado", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("FECHAR", null)
                    .show());
            } catch (Exception e) {
                String msg = e.getMessage() == null ? "Erro desconhecido" : e.getMessage();
                runOnUiThread(() -> status.setText("Erro ao gerar vídeo: " + msg));
            }
        }).start();
    }

    private JSONObject falRequest(String address, String key, String method, JSONObject body)
            throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(address).openConnection();
        try {
            c.setRequestMethod(method);
            c.setConnectTimeout(20000);
            c.setReadTimeout(30000);
            c.setRequestProperty("Authorization", "Key " + key);
            c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            if (body != null) {
                c.setDoOutput(true);
                try (OutputStream os = c.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            int code = c.getResponseCode();
            String data = readAll(code >= 200 && code < 300
                ? c.getInputStream() : c.getErrorStream());
            if (code < 200 || code >= 300)
                throw new Exception("fal.ai (" + code + "): " + data);
            return new JSONObject(data);
        } finally {
            c.disconnect();
        }
    }



    private void createOneMinuteAnime() {
        String title = val(titleInput);
        String idea = val(ideaInput);
        String script = val(episodeOutput);
        String key = prefs.getString("fal_api_key", "").trim();

        if (title.isEmpty() || (idea.isEmpty() && script.isEmpty())) {
            Toast.makeText(this, "Preencha o título e a ideia ou gere um roteiro primeiro.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (key.isEmpty()) {
            generateVideoPreview();
            Toast.makeText(this, "Configure a chave fal.ai e depois toque novamente em CRIAR ANIME.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("Criar anime de 1 minuto")
            .setMessage("O JARVIS vai gerar 12 cenas de 5 segundos e tentar montar o vídeo final. "
                + "A geração usa créditos pagos da fal.ai e pode custar aproximadamente US$ 1,50 "
                + "ou mais, dependendo do modelo e das tentativas. Continuar?")
            .setNegativeButton("CANCELAR", null)
            .setPositiveButton("CRIAR", (d, w) -> {
                status.setText("Preparando 12 cenas do anime...");
                Toast.makeText(this,
                    "A geração automática completa ainda precisa da etapa de montagem e exportação.",
                    Toast.LENGTH_LONG).show();
            })
            .show();
    }

}
