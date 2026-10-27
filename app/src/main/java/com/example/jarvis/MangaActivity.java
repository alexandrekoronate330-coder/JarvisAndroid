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
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

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

        Button generate = button("✨ Criar rascunho do próximo capítulo");
        layout.addView(generate);
        generate.setOnClickListener(v -> generateLocalDraft());

        Button showChapters = button("Ver / editar capítulos salvos");
        layout.addView(showChapters);
        showChapters.setOnClickListener(v -> showChapters());

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
        result.setTextSize(16);
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
        showMessage("Escreva o capítulo " + next + " e toque em Salvar capítulo.");
    }

    private void generateLocalDraft() {
        JSONObject p = currentProject();
        if (p == null) {
            showMessage("Primeiro crie ou abra um mangá.");
            return;
        }
        JSONArray chapters = chapterArray(p);
        if (chapters.length() == 0) {
            showMessage("Salve primeiro o capítulo 1. Depois posso usar ele como base.");
            return;
        }
        JSONObject last = chapters.optJSONObject(chapters.length() - 1);
        if (last == null) {
            showMessage("Não consegui ler o último capítulo.");
            return;
        }
        int number = chapters.length() + 1;
        String lastTitle = last.optString("title", "Capítulo anterior");
        String idea = p.optString("idea", "uma aventura inesperada");
        String characters = p.optString("characters", "o protagonista");
        chapterTitleInput.setText("Capítulo " + number + " - Novos caminhos");
        String draft =
            "CAPÍTULO " + number + " - NOVOS CAMINHOS\n\n" +
            "Continuação de \"" + lastTitle + "\"\n\n" +
            "[CENA 1: A CONSEQUÊNCIA]\n\n" +
            "Os acontecimentos anteriores mudaram o rumo da jornada. Agora, cada escolha teria consequências.\n\n" +
            "NARRADOR: A história estava longe de terminar. Um novo desafio começava a surgir no horizonte.\n\n" +
            "[CENA 2: UMA DECISÃO]\n\n" +
            "PERSONAGEM: \"Não podemos ignorar o que aconteceu. Precisamos descobrir a verdade.\"\n\n" +
            "O grupo se reuniu para decidir o próximo passo. Entre dúvidas e esperanças, uma pista inesperada apareceu.\n\n" +
            "[CENA 3: A PISTA]\n\n" +
            characters + " percebeu que havia algo conectado à ideia central: " + idea + ".\n\n" +
            "PERSONAGEM: \"Isso muda tudo. Estamos diante de algo muito maior.\"\n\n" +
            "[CENA 4: GANCHO FINAL]\n\n" +
            "Antes que alguém pudesse responder, um acontecimento inesperado interrompeu a conversa.\n\n" +
            "NARRADOR: O próximo passo poderia revelar a verdade... ou colocar tudo a perder.\n\n" +
            "[FIM DO RASCUNHO]\n\n" +
            "IMPORTANTE: este rascunho é gerado localmente por regras, não por uma IA avançada. Revise para manter a continuidade exata.";
        chapterTextInput.setText(draft);
        showMessage("Rascunho criado. Revise e edite antes de salvar.");
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
