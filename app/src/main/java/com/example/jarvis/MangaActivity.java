
package com.example.jarvis;

import android.app.Activity;
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
    private EditText titleInput, genreInput, characterInput, ideaInput, editCharacterInput;
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
        e.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(0xFF7DE3FF)
        );
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
        intro.setText("Crie vários mangás e continue cada história pelo título.");
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
        genreInput = field("Gênero: fantasia, ação, isekai...", 1);
        characterInput = field("Personagens e aparência", 3);
        ideaInput = field("Ideia da história", 4);
        editCharacterInput = field(
            "Descreva como quer mudar o personagem", 3
        );

        layout.addView(titleInput);
        layout.addView(genreInput);
        layout.addView(characterInput);
        layout.addView(ideaInput);
        layout.addView(editCharacterInput);

        Button openOrCreate = button("Criar novo / abrir pelo título");
        layout.addView(openOrCreate);
        openOrCreate.setOnClickListener(v -> openOrCreateProject());

        Button editCharacter = button("Salvar mudanças do personagem");
        layout.addView(editCharacter);
        editCharacter.setOnClickListener(v -> applyCharacterDescription());

        Button save = button("Salvar capítulo e progresso");
        layout.addView(save);
        save.setOnClickListener(v -> saveProgress());

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

    private void loadProjectsFromStorage() {
        try {
            projects = new JSONObject(
                prefs.getString("projects_json", "{}")
            );
        } catch (Exception e) {
            projects = new JSONObject();
        }
    }

    private void persistProjects() {
        prefs.edit().putString("projects_json", projects.toString()).apply();
        refreshProjectList();
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
            Button b = button("📖 " + name);
            projectsContainer.addView(b);
            b.setOnClickListener(v -> loadProject(name));
        }
    }

    private void openOrCreateProject() {
        String title = value(titleInput);

        if (title.isEmpty()) {
            result.setText("Digite o título do mangá.");
            return;
        }

        if (projects.has(title)) {
            loadProject(title);
            Toast.makeText(
                this, "História aberta!", Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String idea = value(ideaInput);
        if (idea.isEmpty()) {
            result.setText("Para criar um mangá novo, escreva a ideia da história.");
            return;
        }

        try {
            JSONObject p = new JSONObject();
            p.put("title", title);
            p.put("genre", value(genreInput).isEmpty()
                ? "Aventura e fantasia" : value(genreInput));
            p.put("characters", value(characterInput));
            p.put("idea", idea);
            p.put("chapter", 1);
            p.put("chapter_text", "CAPÍTULO 1\n\n" + idea);
            p.put("character_versions", new JSONArray());

            projects.put(title, p);
            currentTitle = title;
            persistProjects();
            showProject(p);

            Toast.makeText(
                this, "Novo mangá salvo!", Toast.LENGTH_SHORT
            ).show();
        } catch (Exception e) {
            result.setText("Não foi possível salvar o projeto.");
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

        showProject(p);
    }

    private void showProject(JSONObject p) {
        result.setText(
            "PROJETO SALVO\n\n" +
            "Título: " + p.optString("title", "") + "\n" +
            "Gênero: " + p.optString("genre", "") + "\n" +
            "Capítulo atual: " + p.optInt("chapter", 1) + "\n\n" +
            "PERSONAGENS\n" + p.optString("characters", "Não definidos.") +
            "\n\nIDEIA DA HISTÓRIA\n" + p.optString("idea", "") +
            "\n\nCAPÍTULOS SALVOS\n" + p.optString("chapter_text", "") +
            "\n\nO projeto está salvo neste celular. A geração automática " +
            "por IA e as ilustrações ainda precisam de integração com um serviço de IA."
        );
    }

    private void applyCharacterDescription() {
        if (currentTitle.isEmpty()) {
            result.setText("Primeiro crie ou abra um mangá.");
            return;
        }

        String description = value(editCharacterInput);
        if (description.isEmpty()) {
            result.setText("Descreva as mudanças que deseja.");
            return;
        }

        JSONObject p = projects.optJSONObject(currentTitle);
        if (p == null) return;

        String characters = p.optString("characters", "");
        if (!characters.isEmpty()) characters += "\n\n";
        characters += "Alteração solicitada: " + description;

        try {
            p.put("characters", characters);

            JSONArray versions = p.optJSONArray("character_versions");
            if (versions == null) versions = new JSONArray();
            versions.put(description);
            p.put("character_versions", versions);
        } catch (Exception ignored) {
        }

        characterInput.setText(characters);
        editCharacterInput.setText("");
        persistProjects();
        showProject(p);

        Toast.makeText(
            this, "Descrição do personagem salva!", Toast.LENGTH_SHORT
        ).show();
    }

    private void saveProgress() {
        if (currentTitle.isEmpty()) {
            result.setText("Primeiro crie ou abra um mangá.");
            return;
        }

        JSONObject p = projects.optJSONObject(currentTitle);
        if (p == null) return;

        String idea = value(ideaInput);
        String characters = value(characterInput);
        String genre = value(genreInput);

        try {
            p.put("genre", genre.isEmpty()
                ? "Aventura e fantasia" : genre);
            p.put("characters", characters);

            if (!idea.isEmpty()) p.put("idea", idea);

            // Salva o texto atual sem inventar um capítulo novo.
            p.put("chapter_text",
                "CAPÍTULO " + p.optInt("chapter", 1) + "\n\n" +
                p.optString("idea", ""));

            persistProjects();
            showProject(p);

            Toast.makeText(
                this, "Progresso salvo!", Toast.LENGTH_SHORT
            ).show();
        } catch (Exception e) {
            result.setText("Erro ao salvar o progresso.");
        }
    }

    private void startNarration() {
        if (result == null ||
            result.getText().toString().trim().isEmpty()) {
            Toast.makeText(
                this, "Abra um mangá antes de ativar a narração.",
                Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (!ttsReady || tts == null) {
            Toast.makeText(
                this, "A voz ainda está carregando ou não está disponível.",
                Toast.LENGTH_SHORT
            ).show();
            return;
        }

        tts.speak(
            result.getText().toString(),
            TextToSpeech.QUEUE_FLUSH,
            null,
            "jarvis_manga_narration"
        );
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
