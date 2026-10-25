package com.example.jarvis;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MangaActivity extends Activity {
    private EditText titleInput, genreInput, characterInput, ideaInput;
    private TextView result;

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private EditText field(String hint, int minLines) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(0xFFB8C7D0);
        e.setTextSize(16);
        e.setPadding(dp(12), dp(8), dp(12), dp(8));
        e.setMinLines(minLines);
        e.setGravity(Gravity.TOP);
        e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF7DE3FF));
        return e;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(24), dp(20), dp(24));
        layout.setBackgroundColor(0xFF101820);
        scroll.addView(layout);

        TextView heading = new TextView(this);
        heading.setText("JARVIS MANGÁ");
        heading.setTextSize(28);
        heading.setTextColor(0xFF7DE3FF);
        heading.setGravity(Gravity.CENTER);
        layout.addView(heading, new LinearLayout.LayoutParams(-1, -2));

        TextView intro = new TextView(this);
        intro.setText("Prepare sua história de anime colorido");
        intro.setTextColor(Color.WHITE);
        intro.setTextSize(16);
        intro.setPadding(0, dp(12), 0, dp(12));
        layout.addView(intro);

        titleInput = field("Título do mangá", 1);
        genreInput = field("Gênero: fantasia, ação, isekai...", 1);
        characterInput = field("Personagens e aparência", 3);
        ideaInput = field("Conte a ideia da história", 4);

        layout.addView(titleInput);
        layout.addView(genreInput);
        layout.addView(characterInput);
        layout.addView(ideaInput);

        Button create = new Button(this);
        create.setText("Preparar projeto");
        layout.addView(create);

        result = new TextView(this);
        result.setTextColor(Color.WHITE);
        result.setTextSize(16);
        result.setPadding(0, dp(16), 0, dp(16));
        layout.addView(result);

        create.setOnClickListener(v -> {
            String title = titleInput.getText().toString().trim();
            String genre = genreInput.getText().toString().trim();
            String characters = characterInput.getText().toString().trim();
            String idea = ideaInput.getText().toString().trim();

            if (title.isEmpty() || idea.isEmpty()) {
                result.setText("Preencha o título e a ideia da história.");
                return;
            }

            String draft = "PROJETO DE MANGÁ\\n\\n"
                + "Título: " + title + "\\n"
                + "Gênero: " + (genre.isEmpty() ? "Aventura e fantasia" : genre) + "\\n"
                + "Estilo visual: anime colorido\\n\\n"
                + "PERSONAGENS\\n"
                + (characters.isEmpty() ? "Definir os personagens.\\n" : characters + "\\n")
                + "\\nIDEIA ORIGINAL\\n" + idea
                + "\\n\\nPRÓXIMA ETAPA\\n"
                + "Conectar a IA online para desenvolver capítulos, cenas "
                + "e gerar ilustrações coloridas. Este rascunho ainda não usa IA.";
            result.setText(draft);
        });

        Button back = new Button(this);
        back.setText("Voltar ao Jarvis");
        layout.addView(back);
        back.setOnClickListener(v -> finish());

        setContentView(scroll);
    }
}
