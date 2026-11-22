package love.atorixa.oplatinamefix;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Settings screen. Built entirely in code: no layout resources.
 * Reachable from the Xposed manager next to the module entry.
 */
public class SettingsActivity extends Activity {

    private static final String TARGET_PACKAGE = "by.lwo.oplati";

    private EditText oldFirst;
    private EditText newFirst;
    private EditText oldLast;
    private EditText newLast;
    private EditText oldInitials;
    private EditText newInitials;
    private EditText extra;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Map<String, String> cfg = ConfigStore.read(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);

        root.addView(title("Oplati Name Fix"));
        root.addView(hint("Rewrites on-screen text only. Account, server and app data stay untouched."));

        oldFirst = field(root, "Name the app shows now", get(cfg, Config.K_OLD_FIRST, ""));
        newFirst = field(root, "Name to display", get(cfg, Config.K_NEW_FIRST, Config.DEF_NEW_FIRST));

        oldLast = field(root, "Surname the app shows now", get(cfg, Config.K_OLD_LAST, ""));
        newLast = field(root, "Surname to display", get(cfg, Config.K_NEW_LAST, Config.DEF_NEW_LAST));

        oldInitials = field(root, "Avatar initials now", get(cfg, Config.K_OLD_INITIALS, ""));
        newInitials = field(root, "Initials to display", get(cfg, Config.K_NEW_INITIALS, Config.DEF_NEW_INITIALS));

        root.addView(hint("Extra replacements, one per line"));
        extra = new EditText(this);
        extra.setText(get(cfg, Config.K_EXTRA, ""));
        extra.setHint("old=new");
        extra.setGravity(Gravity.TOP | Gravity.START);
        extra.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        extra.setMinLines(3);
        extra.setMaxLines(8);
        root.addView(extra);

        Button save = new Button(this);
        save.setText("Save");
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
        root.addView(save);

        Button reset = new Button(this);
        reset.setText("Reset");
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConfigStore.write(SettingsActivity.this, new LinkedHashMap<String, String>());
                recreate();
            }
        });
        root.addView(reset);

        Button open = new Button(this);
        open.setText("Open Oplati");
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
                Intent i = getPackageManager().getLaunchIntentForPackage(TARGET_PACKAGE);
                if (i != null) {
                    startActivity(i);
                } else {
                    Toast.makeText(SettingsActivity.this, "Oplati is not installed", Toast.LENGTH_SHORT).show();
                }
            }
        });
        root.addView(open);

        root.addView(hint("Reopen Oplati for the change to appear on screens that are already open."));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void save() {
        Map<String, String> cfg = new LinkedHashMap<>();
        cfg.put(Config.K_OLD_FIRST, text(oldFirst));
        cfg.put(Config.K_NEW_FIRST, text(newFirst));
        cfg.put(Config.K_OLD_LAST, text(oldLast));
        cfg.put(Config.K_NEW_LAST, text(newLast));
        cfg.put(Config.K_OLD_INITIALS, text(oldInitials));
        cfg.put(Config.K_NEW_INITIALS, text(newInitials));
        cfg.put(Config.K_EXTRA, text(extra));
        ConfigStore.write(this, cfg);
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }

    private static String get(Map<String, String> cfg, String key, String def) {
        String v = cfg.get(key);
        return v == null || v.isEmpty() ? def : v;
    }

    private static String text(EditText f) {
        return f.getText() == null ? "" : f.getText().toString().trim();
    }

    private EditText field(LinearLayout parent, String label, String value) {
        parent.addView(hint(label));
        EditText f = new EditText(this);
        f.setSingleLine(true);
        f.setText(value);
        parent.addView(f);
        return f;
    }

    private TextView title(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(22);
        t.setPadding(0, 0, 0, dp(12));
        return t;
    }

    private TextView hint(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(13);
        t.setPadding(0, dp(8), 0, dp(4));
        return t;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
