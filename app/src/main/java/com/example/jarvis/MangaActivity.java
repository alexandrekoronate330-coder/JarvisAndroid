package com.example.jarvis;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MangaActivity extends Activity {
    private EditText titleInput, genreInput, characterInput;
    private EditText ideaInput, editCharacterInput;
    private EditText chapterTitleInput, chapterTextInput;
    private TextView result;
    private LinearLayout layout, projectsContainer;
    private SharedPreferences prefs;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private JSONObject projects = new JSONObject();
    private String currentTitle = "";
    private float chapterTextSize = 16f;

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private EditText field(String hint, int lines) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(0xFFB8C7D0);
        e.setTextSize(16);
        e.setPadding(dp(12), dp(8), dp(12), dp(8));
        e.setMinLines(lines);
        e.setGravity(Gravity.TOP);
        e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF7DE3FF));
        return e;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        return b;
    }

    private String value(EditText e) {
        return e.getText().toString().trim();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("jarvis_manga_projects", MODE_PRIVATE);
        loadProjectsFromStorage();

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int r = tts.setLanguage(new Locale("pt", "BR"));
                ttsReady = r != TextToSpeech.LANG_MISSING_DATA
                    && r != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });

        ScrollView scroll = new ScrollView(this);
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(24), dp(20), dp(24));
        layout.setBackgroundColor(0xFF101820);
        scroll.addView(layout);

        TextView heading = new TextView(this);
        heading.setText("JARVIS MANGÁ");
        heading.setTextSize(28);
        heading.setTextColor(0xFF7DE3FF);
        heading.setGravity(Gravity.CENTER);
        layout.addView(heading);

        TextView intro = new TextView(this);
        intro.setText("Crie mangás, edite títulos e capítulos e continue suas histórias.");
        intro.setTextColor(Color.WHITE);
        intro.setTextSize(16);
        intro.setPadding(0, dp(12), 0, dp(12));
        layout.addView(intro);

        TextView savedHeading = new TextView(this);
        savedHeading.setText("MANGÁS SALVOS");
        savedHeading.setTextColor(0xFF7DE3FF);
        savedHeading.setTextSize(18);
        layout.addView(savedHeading);

        projectsContainer = new LinearLayout(this);
        projectsContainer.setOrientation(LinearLayout.VERTICAL);
        layout.addView(projectsContainer);
        refreshProjectList();

        titleInput = field("Título do mangá", 1);
        genreInput = field("Gênero e estilo visual", 1);
        characterInput = field("Personagens e aparência atual", 3);
        ideaInput = field("Ideia e resumo da história", 4);
        editCharacterInput = field("Descreva como quer mudar o personagem", 3);
        chapterTitleInput = field("Título do capítulo", 1);
        chapterTextInput = field("Escreva ou edite o capítulo completo", 8);

        layout.addView(titleInput);
        layout.addView(genreInput);
        layout.addView(characterInput);
        layout.addView(ideaInput);

        Button create = button("Criar novo / abrir pelo título");
        layout.addView(create);
        create.setOnClickListener(v -> openOrCreateProject());

        Button rename = button("✏️ Salvar mudança de título");
        layout.addView(rename);
        rename.setOnClickListener(v -> renameCurrentProject());

        addSectionTitle("PERSONAGENS");
        layout.addView(editCharacterInput);

        Button updateCharacter = button("Salvar descrição do personagem");
        layout.addView(updateCharacter);
        updateCharacter.setOnClickListener(v -> updateCharacter());

        addSectionTitle("CAPÍTULOS");
        layout.addView(chapterTitleInput);
        layout.addView(chapterTextInput);

        Button saveChapter = button("Salvar capítulo novo");
        layout.addView(saveChapter);
        saveChapter.setOnClickListener(v -> saveChapter());

        Button newChapter = button("Preparar próximo capítulo");
        layout.addView(newChapter);
        newChapter.setOnClickListener(v -> prepareNextChapter());

        Button aiConfig = button("🔑 Configurar IA para criar capítulos");
        layout.addView(aiConfig);
        aiConfig.setOnClickListener(v -> configureAiKey());

        Button generate = button("✨ Gerar capítulo com IA");
        layout.addView(generate);
        generate.setOnClickListener(v -> generateAiChapter());

        Button showChapters = button("Ver / editar capítulos salvos");
        layout.addView(showChapters);
        showChapters.setOnClickListener(v -> showChapters());

        addSectionTitle("LEITURA E PUBLICAÇÃO");
        TextView sizeLabel = new TextView(this);
        sizeLabel.setText("Tamanho do texto: ajuste para ler sem dar zoom");
        sizeLabel.setTextColor(Color.WHITE);
        sizeLabel.setTextSize(15);
        layout.addView(sizeLabel);
        LinearLayout sizeRow = new LinearLayout(this);
        sizeRow.setOrientation(LinearLayout.HORIZONTAL);
        Button smallerText = button("A− Menor");
        Button normalText = button("A Normal");
        Button largerText = button("A+ Maior");
        sizeRow.addView(smallerText, new LinearLayout.LayoutParams(0, -2, 1));
        sizeRow.addView(normalText, new LinearLayout.LayoutParams(0, -2, 1));
        sizeRow.addView(largerText, new LinearLayout.LayoutParams(0, -2, 1));
        layout.addView(sizeRow);
        smallerText.setOnClickListener(v -> adjustChapterTextSize(-2f));
        normalText.setOnClickListener(v -> setChapterTextSize(16f));
        largerText.setOnClickListener(v -> adjustChapterTextSize(2f));

        Button shareChapter = button("📤 Exportar / compartilhar capítulo");
        layout.addView(shareChapter);
        shareChapter.setOnClickListener(v -> shareCurrentChapter());

        Button saveProject = button("Salvar dados do mangá");
        layout.addView(saveProject);
        saveProject.setOnClickListener(v -> saveProjectData());

        Button narrate = button("▶ Ativar narração");
        layout.addView(narrate);
        narrate.setOnClickListener(v -> startNarration());

        Button stop = button("■ Parar narração");
        layout.addView(stop);
        stop.setOnClickListener(v -> {
            if (tts != null) tts.stop();
        });

        result = new TextView(this);
        result.setTextColor(Color.WHITE);
        result.setTextSize(chapterTextSize);
        result.setPadding(0, dp(16), 0, dp(16));
        layout.addView(result);

        Button back = button("Voltar ao Jarvis");
        layout.addView(back);
        back.setOnClickListener(v -> finish());

        setContentView(scroll);
    }


    private void addSectionTitle(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(0xFF7DE3FF);
        t.setTextSize(19);
        t.setPadding(0, dp(20), 0, dp(6));
        layout.addView(t);
    }

    private void loadProjectsFromStorage() {
        try {
            projects = new JSONObject(prefs.getString("projects_json", "{}"));
        } catch (Exception e) {
            projects = new JSONObject();
        }
    }

    private void persistProjects() {
        prefs.edit().putString("projects_json", projects.toString()).apply();
        refreshProjectList();
    }

    private JSONObject currentProject() {
        if (currentTitle.isEmpty()) return null;
        return projects.optJSONObject(currentTitle);
    }

    private JSONArray chapterArray(JSONObject p) {
        JSONArray chapters = p.optJSONArray("chapters");
        if (chapters == null) {
            chapters = new JSONArray();
            String oldText = p.optString("chapter_text", "");
            if (!oldText.isEmpty()) {
                try {
                    JSONObject old = new JSONObject();
                    old.put("number", p.optInt("chapter", 1));
                    old.put("title", "Capítulo anterior");
                    old.put("text", oldText);
                    chapters.put(old);
                } catch (Exception ignored) { }
            }
            try { p.put("chapters", chapters); } catch (Exception ignored) { }
        }
        return chapters;
    }

    private void refreshProjectList() {
        if (projectsContainer == null) return;
        projectsContainer.removeAllViews();
        JSONArray names = projects.names();
        if (names == null || names.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("Nenhum mangá salvo ainda.");
            empty.setTextColor(0xFFB8C7D0);
            projectsContainer.addView(empty);
            return;
        }
        for (int i = 0; i < names.length(); i++) {
            String name = names.optString(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            Button open = button("📖 " + name);
            Button delete = button("🗑️");
            row.addView(open, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(delete, new LinearLayout.LayoutParams(-2, -2));
            projectsContainer.addView(row);
            open.setOnClickListener(v -> loadProject(name));
            delete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Apagar mangá?")
                .setMessage("Deseja apagar \"" + name + "\" e todos os seus capítulos? Esta ação não pode ser desfeita.")
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Apagar", (dialog, which) -> {
                    projects.remove(name);
                    if (name.equals(currentTitle)) {
                        currentTitle = "";
                        titleInput.setText("");
                        genreInput.setText("");
                        characterInput.setText("");
                        ideaInput.setText("");
                        editCharacterInput.setText("");
                        chapterTitleInput.setText("");
                        chapterTextInput.setText("");
                        result.setText("Mangá apagado.");
                    }
                    persistProjects();
                    Toast.makeText(this, "Mangá apagado.", Toast.LENGTH_SHORT).show();
                }).show());
        }
    }

    private void openOrCreateProject() {
        String title = value(titleInput);
        if (title.isEmpty()) {
            showMessage("Digite o título do mangá.");
            return;
        }
        if (projects.has(title)) {
            loadProject(title);
            Toast.makeText(this, "História aberta!", Toast.LENGTH_SHORT).show();
            return;
        }
        String idea = value(ideaInput);
        if (idea.isEmpty()) {
            showMessage("Escreva a ideia da história para criar um mangá novo.");
            return;
        }
        try {
            JSONObject p = new JSONObject();
            p.put("title", title);
            p.put("genre", value(genreInput).isEmpty() ? "Aventura e fantasia" : value(genreInput));
            p.put("characters", value(characterInput));
            p.put("idea", idea);
            p.put("chapter", 0);
            p.put("chapters", new JSONArray());
            p.put("character_versions", new JSONArray());
            projects.put(title, p);
            currentTitle = title;
            persistProjects();
            showProject(p);
            Toast.makeText(this, "Novo mangá salvo!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            showMessage("Não foi possível criar o projeto.");
        }
    }

    private void loadProject(String title) {
        JSONObject p = projects.optJSONObject(title);
        if (p == null) return;
        currentTitle = title;
        titleInput.setText(p.optString("title", title));
        genreInput.setText(p.optString("genre", ""));
        characterInput.setText(p.optString("characters", ""));
        ideaInput.setText(p.optString("idea", ""));
        editCharacterInput.setText("");
        chapterTitleInput.setText("");
        chapterTextInput.setText("");
        chapterArray(p);
        persistProjects();
        showProject(p);
    }

    private void showProject(JSONObject p) {
        JSONArray chapters = chapterArray(p);
        result.setText("PROJETO ABERTO\n\n" +
            "Título: " + p.optString("title", "") + "\n" +
            "Gênero/estilo: " + p.optString("genre", "") + "\n" +
            "Capítulos salvos: " + chapters.length() + "\n\n" +
            "PERSONAGENS\n" + p.optString("characters", "Não definidos.") +
            "\n\nIDEIA DA HISTÓRIA\n" + p.optString("idea", "") +
            "\n\nUse 'Ver / editar capítulos salvos' para consultar e alterar os capítulos.");
    }

    private void renameCurrentProject() {
        JSONObject p = currentProject();
        if (p == null) {
            showMessage("Abra um mangá antes de mudar o título.");
            return;
        }
        String newTitle = value(titleInput);
        if (newTitle.isEmpty()) {
            showMessage("Digite o novo título.");
            return;
        }
        if (newTitle.equals(currentTitle)) {
            showMessage("O título não mudou.");
            return;
        }
        if (projects.has(newTitle)) {
            showMessage("Já existe um mangá com esse título. Escolha outro.");
            return;
        }
        try {
            p.put("title", newTitle);
            projects.remove(currentTitle);
            projects.put(newTitle, p);
            currentTitle = newTitle;
            persistProjects();
            showProject(p);
            Toast.makeText(this, "Título alterado sem perder os capítulos!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            showMessage("Não foi possível mudar o título.");
        }
    }

    private void updateCharacter() {
        JSONObject p = currentProject();
        if (p == null) {
            showMessage("Primeiro crie ou abra um mangá.");
            return;
        }
        String description = value(editCharacterInput);
        if (description.isEmpty()) {
            showMessage("Descreva as mudanças desejadas.");
            return;
        }
        try {
            JSONArray versions = p.optJSONArray("character_versions");
            if (versions == null) versions = new JSONArray();
            JSONObject version = new JSONObject();
            version.put("description", description);
            version.put("date", System.currentTimeMillis());
            versions.put(version);
            p.put("character_versions", versions);
            String updated = value(characterInput);
            if (!updated.isEmpty()) updated += "\n\n";
            updated += "Alteração solicitada: " + description;
            p.put("characters", updated);
            characterInput.setText(updated);
            editCharacterInput.setText("");
            persistProjects();
            showProject(p);
            Toast.makeText(this, "Descrição salva. A imagem ainda não foi regenerada.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            showMessage("Erro ao salvar a descrição.");
        }
    }

    private void saveChapter() {
        JSONObject p = currentProject();
        if (p == null) {
            showMessage("Primeiro crie ou abra um mangá.");
            return;
        }
        String text = value(chapterTextInput);
        if (text.isEmpty()) {
            showMessage("Escreva o capítulo antes de salvar.");
            return;
        }
        try {
            JSONArray chapters = chapterArray(p);
            String title = value(chapterTitleInput);
            if (title.isEmpty()) title = "Capítulo " + (chapters.length() + 1);
            JSONObject chapter = new JSONObject();
            chapter.put("number", chapters.length() + 1);
            chapter.put("title", title);
            chapter.put("text", text);
            chapter.put("savedAt", System.currentTimeMillis());
            chapters.put(chapter);
            p.put("chapters", chapters);
            p.put("chapter", chapters.length());
            p.put("chapter_text", text);
            persistProjects();
            chapterTitleInput.setText("");
            chapterTextInput.setText("");
            showProject(p);
            Toast.makeText(this, "Capítulo salvo!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            showMessage("Não foi possível salvar o capítulo.");
        }
    }

    private void prepareNextChapter() {
        JSONObject p = currentProject();
        if (p == null) {
            showMessage("Primeiro crie ou abra um mangá.");
            return;
        }
        int next = chapterArray(p).length() + 1;
        chapterTitleInput.setText("Capítulo " + next);
        chapterTextInput.setText("");
        showMessage("Toque em 'Gerar capítulo com IA' para criar o capítulo " + next + " usando a história-base salva.");
    }

    private void configureAiKey() {
        EditText keyInput = field("Chave de API da Groq", 1);
        keyInput.setSingleLine(true);
        keyInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyInput.setText(prefs.getString("groq_api_key", ""));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(8), dp(16), dp(8));
        TextView help = new TextView(this);
        help.setText("Cole sua chave de API da Groq. Você pode criar/gerenciar a chave em console.groq.com. O serviço tem limites e regras próprias; não é garantido que todo uso seja gratuito.");
        help.setTextColor(Color.WHITE);
        help.setTextSize(14);
        box.addView(help);
        box.addView(keyInput);

        new AlertDialog.Builder(this)
                .setTitle("Conectar IA de histórias")
                .setView(box)
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Apagar chave", (d, w) -> {
                    prefs.edit().remove("groq_api_key").apply();
                    showMessage("Chave apagada deste aparelho.");
                })
                .setPositiveButton("Salvar chave", (d, w) -> {
                    String key = keyInput.getText().toString().trim();
                    if (key.isEmpty()) {
                        showMessage("Cole uma chave válida.");
                    } else {
                        prefs.edit().putString("groq_api_key", key).apply();
                        showMessage("Chave salva neste aparelho.");
                    }
                }).show();
    }

    private void generateAiChapter() {
        JSONObject project = currentProject();
        if (project == null) {
            showMessage("Abra ou crie um mangá primeiro.");
            return;
        }
        String base = project.optString("idea", "").trim();
        if (base.isEmpty()) {
            showMessage("A história-base salva está vazia. Preencha e salve os dados do mangá uma vez.");
            return;
        }
        String apiKey = prefs.getString("groq_api_key", "").trim();
        if (apiKey.isEmpty()) {
            configureAiKey();
            return;
        }

        JSONArray chapters = chapterArray(project);
        int number = chapters.length() + 1;
        String prompt = buildChapterPrompt(project, chapters, number);
        chapterTitleInput.setText("Capítulo " + number);
        chapterTextInput.setText("A IA está escrevendo o capítulo " + number + "...");
        showMessage("Gerando capítulo " + number + " com IA...");

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL("https://api.groq.com/openai/v1/chat/completions");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(90000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                JSONObject request = new JSONObject();
                request.put("model", "llama-3.3-70b-versatile");
                request.put("temperature", 0.85);
                request.put("max_tokens", 4500);
                JSONArray messages = new JSONArray();
                JSONObject system = new JSONObject();
                system.put("role", "system");
                system.put("content", "Você é roteirista profissional de mangá e manhwa. Escreva em português brasileiro. Desenvolva capítulos completos com cenas, ações visuais e diálogos naturais identificados pelo nome dos personagens. Preserve a continuidade. Nunca peça para o usuário repetir a história-base. Termine com um gancho narrativo. Retorne apenas o capítulo pronto.");
                messages.put(system);
                JSONObject user = new JSONObject();
                user.put("role", "user");
                user.put("content", prompt);
                messages.put(user);
                request.put("messages", messages);

                byte[] bytes = request.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream out = connection.getOutputStream()) { out.write(bytes); }
                int status = connection.getResponseCode();
                InputStream stream = (status >= 200 && status < 300)
                        ? connection.getInputStream() : connection.getErrorStream();
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                }
                if (status < 200 || status >= 300) {
                    String error = "Erro HTTP " + status;
                    try {
                        JSONObject errJson = new JSONObject(response.toString());
                        JSONObject err = errJson.optJSONObject("error");
                        if (err != null) error = err.optString("message", error);
                    } catch (Exception ignored) { }
                    final String message = error;
                    runOnUiThread(() -> showMessage("A IA recusou a solicitação: " + message));
                    return;
                }
                JSONObject json = new JSONObject(response.toString());
                JSONArray choices = json.optJSONArray("choices");
                JSONObject choice = choices == null ? null : choices.optJSONObject(0);
                JSONObject msg = choice == null ? null : choice.optJSONObject("message");
                String generated = msg == null ? "" : msg.optString("content", "").trim();
                if (generated.isEmpty()) {
                    runOnUiThread(() -> showMessage("A IA retornou texto vazio. Tente novamente."));
                    return;
                }
                String generatedTitle = extractChapterTitle(generated, number);
                runOnUiThread(() -> {
                    chapterTitleInput.setText(generatedTitle);
                    chapterTextInput.setText(generated);
                    showMessage("Capítulo criado. Toque em 'Salvar capítulo novo' para guardar e depois gere o próximo.");
                });
            } catch (Exception e) {
                String reason = e.getMessage() == null ? "falha de conexão" : e.getMessage();
                runOnUiThread(() -> showMessage("Não consegui conectar à IA: " + reason + ". Confira a internet, a chave e a permissão INTERNET do aplicativo."));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private String buildChapterPrompt(JSONObject project, JSONArray chapters, int number) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Escreva o capítulo ").append(number).append(" completo deste mangá. ");
        prompt.append("Se não houver capítulo salvo, desenvolva a história-base como capítulo 1. ");
        prompt.append("Se houver capítulos salvos, continue depois do último, sem reiniciar a trama nem repetir cenas. ");
        prompt.append("Inclua título, cenas, descrições visuais úteis para os quadros, ações e falas identificadas pelo nome dos personagens. ");
        prompt.append("Faça um capítulo substancial e termine com um gancho.\n\n");
        prompt.append("TÍTULO: ").append(project.optString("title", currentTitle)).append("\n");
        prompt.append("GÊNERO: ").append(project.optString("genre", "Aventura")).append("\n");
        prompt.append("HISTÓRIA-BASE DO USUÁRIO: ").append(project.optString("idea", "")).append("\n");
        prompt.append("PERSONAGENS E APARÊNCIA: ").append(project.optString("characters", "Ainda não definidos")).append("\n\n");
        prompt.append("CAPÍTULOS ANTERIORES PARA CONTINUIDADE:\n");
        int start = Math.max(0, chapters.length() - 3);
        if (chapters.length() == 0) {
            prompt.append("Nenhum capítulo salvo ainda. Este é o capítulo 1.\n");
        } else {
            for (int i = start; i < chapters.length(); i++) {
                JSONObject c = chapters.optJSONObject(i);
                if (c != null) {
                    prompt.append("\n--- ").append(c.optString("title", "Capítulo " + (i + 1))).append(" ---\n");
                    String text = c.optString("text", "");
                    if (text.length() > 6500) text = text.substring(0, 6500);
                    prompt.append(text).append("\n");
                }
            }
        }
        prompt.append("\nAgora escreva somente o capítulo ").append(number).append(" completo em português brasileiro.");
        return prompt.toString();
    }

    private String extractChapterTitle(String text, int number) {
        for (String line : text.split("\n")) {
            String candidate = line.trim().replace("#", "").trim();
            String lower = candidate.toLowerCase(Locale.ROOT);
            if (!candidate.isEmpty() && candidate.length() <= 100
                    && (lower.contains("capítulo") || lower.startsWith("episódio"))) return candidate;
        }
        return "Capítulo " + number;
    }

    private void showChapters() {
        JSONObject p = currentProject();
        if (p == null) {
            showMessage("Primeiro crie ou abra um mangá.");
            return;
        }
        JSONArray chapters = chapterArray(p);
        if (chapters.length() == 0) {
            showMessage("Este mangá ainda não tem capítulos salvos.");
            return;
        }
        String[] labels = new String[chapters.length()];
        for (int i = 0; i < chapters.length(); i++) {
            JSONObject c = chapters.optJSONObject(i);
            labels[i] = "Capítulo " + (c == null ? i + 1 : c.optInt("number", i + 1)) +
                ": " + (c == null ? "Sem título" : c.optString("title", "Sem título"));
        }
        new AlertDialog.Builder(this)
            .setTitle("Escolha um capítulo para editar")
            .setItems(labels, (dialog, which) -> editSavedChapter(which))
            .setNegativeButton("Fechar", null)
            .show();
    }

    private void editSavedChapter(int index) {
        JSONObject p = currentProject();
        if (p == null) return;
        JSONArray chapters = chapterArray(p);
        JSONObject chapter = chapters.optJSONObject(index);
        if (chapter == null) {
            showMessage("Não consegui abrir esse capítulo.");
            return;
        }
        EditText editTitle = field("Título do capítulo", 1);
        editTitle.setText(chapter.optString("title", ""));
        EditText editText = field("Texto completo do capítulo", 10);
        editText.setText(chapter.optString("text", ""));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(8), dp(16), dp(8));
        box.addView(editTitle);
        box.addView(editText);
        ScrollView scroller = new ScrollView(this);
        scroller.addView(box);
        new AlertDialog.Builder(this)
            .setTitle("Editar capítulo " + chapter.optInt("number", index + 1))
            .setView(scroller)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar alterações", (dialog, which) -> {
                String newTitle = editTitle.getText().toString().trim();
                String newText = editText.getText().toString().trim();
                if (newText.isEmpty()) {
                    Toast.makeText(this, "O texto não pode ficar vazio.", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    chapter.put("title", newTitle.isEmpty() ? "Capítulo " + chapter.optInt("number", index + 1) : newTitle);
                    chapter.put("text", newText);
                    chapter.put("savedAt", System.currentTimeMillis());
                    p.put("chapters", chapters);
                    p.put("chapter_text", newText);
                    persistProjects();
                    showProject(p);
                    Toast.makeText(this, "Capítulo atualizado!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    showMessage("Erro ao atualizar capítulo.");
                }
            }).show();
    }

    private void adjustChapterTextSize(float delta) {
        setChapterTextSize(chapterTextSize + delta);
    }

    private void setChapterTextSize(float size) {
        chapterTextSize = Math.max(12f, Math.min(30f, size));
        if (result != null) result.setTextSize(chapterTextSize);
        Toast.makeText(this, "Tamanho do texto: " + (int) chapterTextSize, Toast.LENGTH_SHORT).show();
    }

    private void shareCurrentChapter() {
        JSONObject p = currentProject();
        if (p == null) {
            showMessage("Abra um mangá primeiro.");
            return;
        }
        JSONArray chapters = chapterArray(p);
        if (chapters.length() == 0) {
            showMessage("Salve pelo menos um capítulo antes de exportar.");
            return;
        }
        JSONObject chapter = chapters.optJSONObject(chapters.length() - 1);
        if (chapter == null) {
            showMessage("Não consegui ler o último capítulo.");
            return;
        }
        String exportText = p.optString("title", currentTitle) + "\n" +
            chapter.optString("title", "Capítulo") + "\n\n" +
            chapter.optString("text", "");
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, p.optString("title", currentTitle) + " - " + chapter.optString("title", "Capítulo"));
        send.putExtra(Intent.EXTRA_TEXT, exportText);
        try {
            startActivity(Intent.createChooser(send, "Compartilhar capítulo"));
        } catch (Exception e) {
            showMessage("Não foi possível abrir o menu de compartilhamento.");
        }
    }

    private void saveProjectData() {
        JSONObject p = currentProject();
        if (p == null) {
            showMessage("Primeiro crie ou abra um mangá.");
            return;
        }
        try {
            p.put("genre", value(genreInput));
            p.put("characters", value(characterInput));
            p.put("idea", value(ideaInput));
            persistProjects();
            showProject(p);
            Toast.makeText(this, "Dados do mangá salvos!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            showMessage("Erro ao salvar o projeto.");
        }
    }

    private void startNarration() {
        if (result == null || result.getText().toString().trim().isEmpty()) {
            showMessage("Abra um mangá ou capítulo antes de narrar.");
            return;
        }
        if (!ttsReady || tts == null) {
            showMessage("A voz ainda está carregando ou não está disponível.");
            return;
        }
        tts.speak(result.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, "jarvis_manga_narration");
    }

    private void showMessage(String message) {
        if (result != null) {
            result.setText(message);
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
