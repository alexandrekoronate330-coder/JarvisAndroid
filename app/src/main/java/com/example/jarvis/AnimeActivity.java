package com.example.jarvis;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AnimeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(28, 28, 28, 28);
        layout.setBackgroundColor(0xFF101820);

        TextView title = new TextView(this);
        title.setText("JARVIS ANIME");
        title.setTextSize(28);
        title.setTextColor(0xFF7DE3FF);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView info = new TextView(this);
        info.setText("\\nPrimeira etapa concluída!\\n\\nAqui vamos criar histórias e episódios contínuos.\\n\\nA geração de vídeos será implementada depois.");
        info.setTextSize(17);
        info.setTextColor(Color.WHITE);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 24, 0, 24);
        layout.addView(info);

        setContentView(layout);
    }
}
