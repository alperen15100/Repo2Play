
package com.ecrinlabs.repo2play;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.util.Base64;

import org.json.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

public class MainActivity extends Activity {
    private static final String ENGINE_REPO = "alperen15100/Repo2Play";
    private static final String ENGINE_REF = "v12-release";
    private static final String WORKFLOW = "repo2play.yml";
    private static final String PRIVACY_URL = "https://alperen15100.github.io/repo2play-legal/privacy.html";
    private static final String TERMS_URL = "https://alperen15100.github.io/repo2play-legal/terms.html";
    private static final String SUPPORT_URL = "https://alperen15100.github.io/repo2play-legal/support.html";

    private final int BG=Color.rgb(10,12,16);
    private final int SURFACE=Color.rgb(18,21,27);
    private final int SURFACE2=Color.rgb(25,29,37);
    private final int TXT=Color.rgb(244,241,234);
    private final int MUT=Color.rgb(150,156,166);
    private final int ACCENT=Color.rgb(178,151,96);
    private final int GOOD=Color.rgb(128,181,138);
    private final int BAD=Color.rgb(202,116,116);

    private EditText token,target,branch;
    private Button newBtn,updateBtn,actionBtn,connectBtn,importBtn,privacyBtn;
    private TextView status,history,accountLabel,vaultLabel;
    private String mode="NEW";
    private SecureStore secure;
    private HistoryStore historyStore;
    private long currentRunId=0;
    private String currentArtifactUrl="", currentArtifactName="", currentRepo="";
    private static final int PICK_JKS=4101;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        secure=new SecureStore(this);
        historyStore=new HistoryStore(this);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildUi();
        refreshHistory();
    }

    private void buildUi(){
        ScrollView sv=new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackgroundColor(BG);
        LinearLayout page=col();
        page.setPadding(d(22),d(24),d(22),d(42));
        sv.addView(page);

        // Header
        LinearLayout hero=new LinearLayout(this);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout brand=col();
        TextView title=t("Repo2Play",32,TXT,true);
        brand.addView(title);
        TextView by=t("ECRIN LABS",11,ACCENT,true);
        by.setLetterSpacing(.16f);
        by.setPadding(0,d(5),0,0);
        brand.addView(by);
        TextView tagline=t("Mobile Android Release Studio",14,MUT,false);
        tagline.setPadding(0,d(8),0,0);
        brand.addView(tagline);
        hero.addView(brand,new LinearLayout.LayoutParams(0,-2,1));

        TextView mark=t("R2",18,ACCENT,true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(stroked(SURFACE,ACCENT,18,1));
        hero.addView(mark,new LinearLayout.LayoutParams(d(62),d(62)));
        page.addView(hero);

        TextView intro=t("Build, sign and package Android releases from a GitHub repository — directly from your phone.",15,TXT,false);
        intro.setLineSpacing(d(3),1f);
        intro.setPadding(0,d(24),0,d(16));
        page.addView(intro);

        Button helpBtn=secondary("HOW TO USE • STEP BY STEP");
        page.addView(helpBtn,smallButtonParams());
        helpBtn.setOnClickListener(v->
            startActivity(new Intent(this,HelpActivity.class))
        );

        // GitHub card
        LinearLayout auth=card();
        page.addView(auth);
        auth.addView(section("GITHUB"));
        accountLabel=t("Not connected",13,MUT,false);
        accountLabel.setPadding(0,d(4),0,0);
        auth.addView(accountLabel);

        token=input("Personal access token");
        token.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        token.setText(secure.get("github_token"));
        LinearLayout.LayoutParams tp=field(); tp.setMargins(0,d(13),0,0);
        auth.addView(token,tp);

        connectBtn=secondary("CONNECT SECURELY");
        auth.addView(connectBtn,buttonParams());
        connectBtn.setOnClickListener(v->connect());

        // Release card
        LinearLayout release=card();
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);
        rp.setMargins(0,d(14),0,0);
        page.addView(release,rp);

        release.addView(section("NEW RELEASE"));
        TextView helper=t("Paste the GitHub project link or owner/repository.",12,MUT,false);
        helper.setPadding(0,d(4),0,d(14));
        release.addView(helper);

        release.addView(label("REPOSITORY"));
        target=input("https://github.com/owner/repository");
        target.setText(getPreferences(MODE_PRIVATE).getString("last_repo",""));
        release.addView(target,field());

        release.addView(space(12));
        release.addView(label("BRANCH"));
        branch=input("main");
        branch.setText(getPreferences(MODE_PRIVATE).getString("last_branch","main"));
        release.addView(branch,field());

        release.addView(space(18));
        release.addView(label("BUILD MODE"));
        LinearLayout modes=new LinearLayout(this);
        newBtn=modeButton("NEW",true);
        updateBtn=modeButton("UPDATE",false);
        modes.addView(newBtn,new LinearLayout.LayoutParams(0,d(50),1));
        LinearLayout.LayoutParams up=new LinearLayout.LayoutParams(0,d(50),1);
        up.setMargins(d(10),0,0,0);
        modes.addView(updateBtn,up);
        release.addView(modes);
        newBtn.setOnClickListener(v->setMode("NEW"));
        updateBtn.setOnClickListener(v->setMode("UPDATE"));

        vaultLabel=t("Signing Vault • NEW creates and secures a project signing key.",12,MUT,false);
        vaultLabel.setPadding(0,d(14),0,0);
        release.addView(vaultLabel);

        importBtn=secondary("IMPORT ORIGINAL JKS");
        release.addView(importBtn,smallButtonParams());
        importBtn.setOnClickListener(v->pickKey());

        Button installEngineBtn=secondary("INSTALL BUILD ENGINE");
        release.addView(installEngineBtn,smallButtonParams());
        installEngineBtn.setOnClickListener(v->installBuildEngine());

        TextView engineInfo=t(
            "Install once for this repository. Builds then run in your own GitHub Actions.",
            11,MUT,false
        );
        engineInfo.setPadding(0,d(6),0,0);
        release.addView(engineInfo);

        actionBtn=primary("BUILD RELEASE");
        release.addView(actionBtn,buttonParams());
        actionBtn.setOnClickListener(v->dispatch());

        // Status card
        LinearLayout st=card();
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);
        sp.setMargins(0,d(14),0,0);
        page.addView(st,sp);
        st.addView(section("BUILD STATUS"));
        status=t("Ready",14,TXT,false);
        status.setPadding(0,d(8),0,0);
        status.setTextIsSelectable(true);
        status.setLineSpacing(d(3),1f);
        st.addView(status);

        // History card
        LinearLayout hist=card();
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);
        hp.setMargins(0,d(14),0,0);
        page.addView(hist,hp);
        hist.addView(section("RECENT BUILDS"));
        history=t("",13,TXT,false);
        history.setPadding(0,d(8),0,0);
        history.setLineSpacing(d(4),1f);
        hist.addView(history);

        // Privacy card/footer
        LinearLayout privacy=card();
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,-2);
        pp.setMargins(0,d(14),0,0);
        page.addView(privacy,pp);
        privacy.addView(section("PRIVACY & SECURITY"));
        TextView ptxt=t("GitHub token and project signing keys are encrypted on this device. Build requests are sent directly to GitHub over HTTPS.",12,MUT,false);
        ptxt.setPadding(0,d(6),0,0);
        privacy.addView(ptxt);
        privacyBtn=secondary("PRIVACY POLICY");
        privacy.addView(privacyBtn,smallButtonParams());
        privacyBtn.setOnClickListener(v->{
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL))); }
            catch(Exception ignored){}
        });

        Button termsBtn=secondary("TERMS OF USE");
        privacy.addView(termsBtn,smallButtonParams());
        termsBtn.setOnClickListener(v->{
            try {
                startActivity(
                    new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(TERMS_URL)
                    )
                );
            } catch(Exception ignored){}
        });

        Button supportBtn=secondary("SUPPORT");
        privacy.addView(supportBtn,smallButtonParams());
        supportBtn.setOnClickListener(v->{
            try {
                startActivity(
                    new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(SUPPORT_URL)
                    )
                );
            } catch(Exception ignored){}
        });

        Button clearBtn=secondary(
            "CLEAR CREDENTIALS & SIGNING VAULT"
        );

        privacy.addView(
            clearBtn,
            smallButtonParams()
        );

        clearBtn.setOnClickListener(v->{

            new AlertDialog.Builder(this)

                .setTitle(
                    "Clear local security data?"
                )

                .setMessage(
                    "This removes the saved GitHub token " +
                    "and all signing keys stored in the " +
                    "Repo2Play Signing Vault. " +
                    "Keep your original JKS backups before continuing."
                )

                .setNegativeButton(
                    "Cancel",
                    null
                )

                .setPositiveButton(
                    "Clear",
                    (dialog,which)->{

                        secure.clearAll();

                        token.setText("");

                        accountLabel.setText(
                            "Not connected"
                        );

                        accountLabel.setTextColor(
                            MUT
                        );

                        vaultLabel.setText(
                            "Signing Vault • No locally stored signing credentials."
                        );

                        setStatus(
                            "Local GitHub credentials and Signing Vault data cleared."
                        );
                    }
                )

                .show();
        });

        TextView foot=t("Repo2Play v13.1 • by Ecrin Labs",11,MUT,false);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0,d(22),0,0);
        page.addView(foot);

        setContentView(sv);
    }

    private void connect(){
        String tok=token.getText().toString().trim();
        if(tok.length()<10){ token.setError("GitHub token required"); return; }
        setStatus("Connecting securely to GitHub…");
        new Thread(()->{
            try{
                JSONObject me=get("https://api.github.com/user",tok);
                secure.put("github_token",tok);
                String login=me.optString("login","GitHub user");
                runOnUiThread(()->{
                    accountLabel.setText("Connected • "+login);
                    accountLabel.setTextColor(GOOD);
                    setStatus("✓ GitHub connected\nCredentials encrypted with Android Keystore.");
                });
            }catch(Exception e){ showError(e); }
        }).start();
    }



    private void installBuildEngine(){
        hideKeyboard();

        String tok=secure.get("github_token");
        if(tok.isEmpty()) tok=token.getText().toString().trim();

        String repo=normalizeRepo(target.getText().toString());
        String br=branch.getText().toString().trim();

        if(tok.length()<10){
            token.setError("Connect GitHub first");
            return;
        }

        if(repo==null){
            target.setError(
                "Paste a GitHub repo link or owner/repository"
            );
            return;
        }

        if(br.isEmpty()){
            branch.setError("Branch required");
            return;
        }

        final String finalTok=tok;
        final String finalRepo=repo;
        final String finalBranch=br;

        setStatus(
            "Installing Repo2Play Build Engine...\\n\\n"+
            finalRepo+
            "\\nBranch: "+finalBranch
        );

        new Thread(()->{
            try{

                putRepoFile(
                    finalTok,
                    finalRepo,
                    finalBranch,
                    ".github/workflows/repo2play-build.yml",
                    "bmFtZTogUmVwbzJQbGF5IEJ1aWxkCgpvbjoKICB3b3JrZmxvd19kaXNwYXRjaDoKICAgIGlucHV0czoKICAgICAgYnVpbGRfbW9kZToKICAgICAgICBkZXNjcmlwdGlvbjogIk5FVyBvciBVUERBVEUiCiAgICAgICAgcmVxdWlyZWQ6IHRydWUKICAgICAgICBkZWZhdWx0OiAiTkVXIgogICAgICAgIHR5cGU6IGNob2ljZQogICAgICAgIG9wdGlvbnM6CiAgICAgICAgICAtIE5FVwogICAgICAgICAgLSBVUERBVEUKCiAgICAgIGtleXN0b3JlX2Jhc2U2NDoKICAgICAgICBkZXNjcmlwdGlvbjogIkV4aXN0aW5nIFJlcG8yUGxheSBzaWduaW5nIGtleSBmb3IgVVBEQVRFIgogICAgICAgIHJlcXVpcmVkOiBmYWxzZQogICAgICAgIGRlZmF1bHQ6ICIiCiAgICAgICAgdHlwZTogc3RyaW5nCgpwZXJtaXNzaW9uczoKICBjb250ZW50czogcmVhZAoKam9iczoKICBidWlsZDoKICAgIG5hbWU6IEFuYWx5emUgQnVpbGQgU2lnbiBQYWNrYWdlCiAgICBydW5zLW9uOiB1YnVudHUtbGF0ZXN0CiAgICB0aW1lb3V0LW1pbnV0ZXM6IDQ1CgogICAgc3RlcHM6CiAgICAgIC0gbmFtZTogQ2hlY2tvdXQgcHJvamVjdAogICAgICAgIHVzZXM6IGFjdGlvbnMvY2hlY2tvdXRAdjQKCiAgICAgIC0gbmFtZTogU2V0dXAgSmF2YSAxNwogICAgICAgIHVzZXM6IGFjdGlvbnMvc2V0dXAtamF2YUB2NQogICAgICAgIHdpdGg6CiAgICAgICAgICBkaXN0cmlidXRpb246IHRlbXVyaW4KICAgICAgICAgIGphdmEtdmVyc2lvbjogIjE3IgoKICAgICAgLSBuYW1lOiBQcmVwYXJlIFJlcG8yUGxheSBlbmdpbmUKICAgICAgICBzaGVsbDogYmFzaAogICAgICAgIHJ1bjogfAogICAgICAgICAgc2V0IC1ldW8gcGlwZWZhaWwKICAgICAgICAgIG1rZGlyIC1wIC5yZXBvMnBsYXkvc2NyaXB0cwogICAgICAgICAgcHJpbnRmICclcycgJ0l5RXZkWE55TDJKcGJpOWxibllnWW1GemFBcHpaWFFnTFhWdklIQnBjR1ZtWVdsc0NncFVRVkpIUlZROUlpUjdNVG8vZEdGeVoyVjBJSEJoZEdnZ2NtVnhkV2x5WldSOUlncFBWVlJRVlZROUlpUjdNam8vYjNWMGNIVjBJSEJoZEdnZ2NtVnhkV2x5WldSOUlncEZUa2RKVGtVOUlpUW9ZMlFnSWlRb1pHbHlibUZ0WlNBaUpIdENRVk5JWDFOUFZWSkRSVnN3WFgwaUtTOHVMaUlnSmlZZ2NIZGtLU0lLQ25KdElDMXlaaUFpSkU5VlZGQlZWQ0lLYld0a2FYSWdMWEFnSWlSUFZWUlFWVlFpQ2dwU1JWQlBVbFE5SWlSUFZWUlFWVlF2UWxWSlRFUXRVa1ZRVDFKVUxuUjRkQ0lLZXdvZ0lHVmphRzhnSWxKRlVFOHlVRXhCV1NCV01URWdVRkpQUkZWRFZFbFBUaUJUVkVGSFJTQXhJZ29nSUdWamFHOGdJajA5UFQwOVBUMDlQVDA5UFQwOVBUMDlQVDA5UFQwOVBUMDlQVDBpQ2lBZ1pXTm9ieUFpVW1Wd2IzTnBkRzl5ZVRvZ0pIdFVRVkpIUlZSZlVrVlFUMU5KVkU5U1dUb3RkVzVyYm05M2JuMGlDaUFnWldOb2J5QWlRbkpoYm1Ob09pQWtlMVJCVWtkRlZGOUNVa0ZPUTBnNkxYVnVhMjV2ZDI1OUlnb2dJR1ZqYUc4Z0lrMXZaR1U2SUNSN1FsVkpURVJmVFU5RVJUb3RUa1ZYZlNJS0lDQmxZMmh2SUNKVGRHRnlkR1ZrT2lBa0tHUmhkR1VnTFhVZ0t5Y2xXUzBsYlMwbFpGUWxTRG9sVFRvbFUxb25LU0lLSUNCbFkyaHZDbjBnUGlBaUpGSkZVRTlTVkNJS0NtWmhhV3hmY21Wd2IzSjBLQ2tnZXdvZ0lHeHZZMkZzSUcxelp6MGlKREVpQ2lBZ2V3b2dJQ0FnWldOb2J3b2dJQ0FnWldOb2J5QWlSa2xPUVV3Z1VrVlRWVXhVT2lCQ1RFOURTMFZFSWdvZ0lDQWdaV05vYnlBaVVtVmhjMjl1T2lBa2JYTm5JZ29nSUgwZ1BqNGdJaVJTUlZCUFVsUWlDaUFnWldOb2J5QWlKRzF6WnlJZ1BpQWlKRTlWVkZCVlZDOUZVbEpQVWk1MGVIUWlDaUFnWlhocGRDQXhDbjBLQ2tSRlZFVkRWRjlQVlZROUlpUlBWVlJRVlZRdlpHVjBaV04wTG1WdWRpSUtJaVJGVGtkSlRrVXZjMk55YVhCMGN5OWtaWFJsWTNRdGNISnZhbVZqZEM1emFDSWdJaVJVUVZKSFJWUWlJQ0lrUkVWVVJVTlVYMDlWVkNJZ1BqNGdJaVJTUlZCUFVsUWlJREkrSmpFZ2ZId2dabUZwYkY5eVpYQnZjblFnSWtGdVpISnZhV1FnWVhCd2JHbGpZWFJwYjI0Z2NISnZhbVZqZENCamIzVnNaQ0J1YjNRZ1ltVWdaR1YwWldOMFpXUXVJZ3B6YjNWeVkyVWdJaVJFUlZSRlExUmZUMVZVSWdvS2NIbDBhRzl1TXlBaUpFVk9SMGxPUlM5elkzSnBjSFJ6TDNabGNuTnBiMjR1Y0hraUlDSWtVRkpQU2tWRFZGOUVTVklpSUNJa1FWQlFYMDFQUkZWTVJTSWdJaVI3UWxWSlRFUmZUVTlFUlRvdFRrVlhmU0lnSWlSUFZWUlFWVlF2VmtWU1UwbFBUaTFKVGtaUExtcHpiMjRpSUQ0K0lDSWtVa1ZRVDFKVUlpQXlQaVl4SUh4OElHWmhhV3hmY21Wd2IzSjBJQ0pXWlhKemFXOXVJSEJ5WlhCaGNtRjBhVzl1SUdaaGFXeGxaQzRpQ2dwSFVrRkVURVZmVDFWVVBTSWtUMVZVVUZWVUwyZHlZV1JzWlM1bGJuWWlDaUlrUlU1SFNVNUZMM05qY21sd2RITXZjbVZ6YjJ4MlpTMW5jbUZrYkdVdWMyZ2lJQ0lrVUZKUFNrVkRWRjlFU1ZJaUlDSWtSMUpCUkV4RlgwOVZWQ0lnUGo0Z0lpUlNSVkJQVWxRaUlESStKakVnZkh3Z1ptRnBiRjl5WlhCdmNuUWdJa052YlhCaGRHbGliR1VnUjNKaFpHeGxJR052ZFd4a0lHNXZkQ0JpWlNCd2NtVndZWEpsWkM0aUNuTnZkWEpqWlNBaUpFZFNRVVJNUlY5UFZWUWlDZ29pSkVWT1IwbE9SUzl6WTNKcGNIUnpMMkoxYVd4a0xuTm9JaUFpSkZCU1QwcEZRMVJmUkVsU0lpQWlKRWRTUVVSTVJWOURUVVFpSUNJa1FWQlFYMDFQUkZWTVJTSWdJaVJQVlZSUVZWUWlJRDQrSUNJa1VrVlFUMUpVSWlBeVBpWXhJSHg4SUdaaGFXeGZjbVZ3YjNKMElDSkJibVJ5YjJsa0lISmxiR1ZoYzJVZ1luVnBiR1FnWm1GcGJHVmtMaUlLQ2lJa1JVNUhTVTVGTDNOamNtbHdkSE12YzJsbmJpNXphQ0lnSWlSUFZWUlFWVlFpSUQ0K0lDSWtVa1ZRVDFKVUlpQXlQaVl4SUh4OElHWmhhV3hmY21Wd2IzSjBJQ0pUYVdkdWFXNW5JR1poYVd4bFpDNGlDZ29pSkVWT1IwbE9SUzl6WTNKcGNIUnpMMlJ2WTNSdmNpNXphQ0lnSWlSUVVrOUtSVU5VWDBSSlVpSWdJaVJQVlZSUVZWUWlJRDQrSUNJa1VrVlFUMUpVSWlBeVBpWXhJSHg4SUhSeWRXVUtJaVJGVGtkSlRrVXZjMk55YVhCMGN5OXdZV05yWVdkbExuTm9JaUFpSkU5VlZGQlZWQ0lnUGo0Z0lpUlNSVkJQVWxRaUlESStKakVnZkh3Z1ptRnBiRjl5WlhCdmNuUWdJa1pwYm1Gc0lIQmhZMnRoWjJVZ2NISmxjR0Z5WVhScGIyNGdabUZwYkdWa0xpSUtDbnNLSUNCbFkyaHZDaUFnWldOb2J5QWlSa2xPUVV3Z1VrVlRWVXhVT2lCVFZVTkRSVk5USWdvZ0lHVmphRzhnSWtacGJtbHphR1ZrT2lBa0tHUmhkR1VnTFhVZ0t5Y2xXUzBsYlMwbFpGUWxTRG9sVFRvbFUxb25LU0lLZlNBK1BpQWlKRkpGVUU5U1ZDSUtDbVZqYUc4Z0lsSmxjRzh5VUd4aGVTQmpiMjF3YkdWMFpXUWdjM1ZqWTJWemMyWjFiR3g1TGlJSycgfCBiYXNlNjQgLS1kZWNvZGUgPiAucmVwbzJwbGF5L3NjcmlwdHMvcnVuLnNoCiAgICAgICAgICBwcmludGYgJyVzJyAnSXlFdmRYTnlMMkpwYmk5bGJuWWdZbUZ6YUFwelpYUWdMV1YxYnlCd2FYQmxabUZwYkFwVVFWSkhSVlE5SWlSN01Uby9mU0lLVDFWVVBTSWtlekk2UDMwaUNncFRSVlJVU1U1SFV6MGlKQ2htYVc1a0lDSWtWRUZTUjBWVUlpQXRkSGx3WlNCbUlGd29JQzF1WVcxbElITmxkSFJwYm1kekxtZHlZV1JzWlNBdGJ5QXRibUZ0WlNCelpYUjBhVzVuY3k1bmNtRmtiR1V1YTNSeklGd3BJQ0VnTFhCaGRHZ2dKeW92WW5WcGJHUXZLaWNnZkNCb1pXRmtJQzB4SUh4OElIUnlkV1VwSWdwYklDMXVJQ0lrVTBWVVZFbE9SMU1pSUYwZ2ZId2dleUJsWTJodklDSk9ieUJ6WlhSMGFXNW5jeTVuY21Ga2JHVXZjMlYwZEdsdVozTXVaM0poWkd4bExtdDBjeUk3SUdWNGFYUWdNVHNnZlFwUVVrOUtSVU5VWDBSSlVqMGlKQ2hrYVhKdVlXMWxJQ0lrVTBWVVZFbE9SMU1pS1NJS0NrRlFVRjlDVlVsTVJEMGlJZ3AzYUdsc1pTQkpSbE05SUhKbFlXUWdMWElnWmpzZ1pHOEtJQ0JwWmlCbmNtVndJQzFGY1NBblkyOXRYQzVoYm1SeWIybGtYQzVoY0hCc2FXTmhkR2x2Ym54cFpGdGJPbk53WVdObE9sMWRLbHdvUDF0Yk9uTndZV05sT2wxZEtsc2lKMXduSjExamIyMWNMbUZ1WkhKdmFXUmNMbUZ3Y0d4cFkyRjBhVzl1ZkdGd2NHeDVJSEJzZFdkcGJqcGJXenB6Y0dGalpUcGRYU3BiSWlkY0p5ZGRZMjl0WEM1aGJtUnliMmxrWEM1aGNIQnNhV05oZEdsdmJpY2dJaVJtSWpzZ2RHaGxiZ29nSUNBZ1FWQlFYMEpWU1V4RVBTSWtaaUk3SUdKeVpXRnJDaUFnWm1rS1pHOXVaU0E4SUR3b1ptbHVaQ0FpSkZCU1QwcEZRMVJmUkVsU0lpQXRkSGx3WlNCbUlGd29JQzF1WVcxbElHSjFhV3hrTG1keVlXUnNaU0F0YnlBdGJtRnRaU0JpZFdsc1pDNW5jbUZrYkdVdWEzUnpJRndwSUNFZ0xYQmhkR2dnSnlvdlluVnBiR1F2S2ljcENncGJJQzF1SUNJa1FWQlFYMEpWU1V4RUlpQmRJSHg4SUhzZ1pXTm9ieUFpVG04Z1kyOXRMbUZ1WkhKdmFXUXVZWEJ3YkdsallYUnBiMjRnYlc5a2RXeGxJR1p2ZFc1a0lqc2daWGhwZENBeE95QjlDazFQUkZWTVJWOUVTVkk5SWlRb1pHbHlibUZ0WlNBaUpFRlFVRjlDVlVsTVJDSXBJZ3BTUlV3OUlpUjdUVTlFVlV4RlgwUkpVaU1pSkZCU1QwcEZRMVJmUkVsU0lpOTlJZ3BwWmlCYklDSWtUVTlFVlV4RlgwUkpVaUlnUFNBaUpGQlNUMHBGUTFSZlJFbFNJaUJkT3lCMGFHVnVDaUFnUVZCUVgwMVBSRlZNUlQwaUlncGxiSE5sQ2lBZ1FWQlFYMDFQUkZWTVJUMGlKSHRTUlV3dkwxd3ZMenA5SWdwbWFRb0tld29nSUhCeWFXNTBaaUFuVUZKUFNrVkRWRjlFU1ZJOUpYRmNiaWNnSWlSUVVrOUtSVU5VWDBSSlVpSUtJQ0J3Y21sdWRHWWdKMEZRVUY5TlQwUlZURVU5SlhGY2JpY2dJaVJCVUZCZlRVOUVWVXhGSWdwOUlENGdJaVJQVlZRaUNncGxZMmh2SUNKUVFWTlRJRUZ1WkhKdmFXUWdjSEp2YW1WamREb2dKRkJTVDBwRlExUmZSRWxTSWdwbFkyaHZJQ0pRUVZOVElFRndjR3hwWTJGMGFXOXVJRzF2WkhWc1pUb2dKSHRCVUZCZlRVOUVWVXhGT2kxeWIyOTBmU0lLJyB8IGJhc2U2NCAtLWRlY29kZSA+IC5yZXBvMnBsYXkvc2NyaXB0cy9kZXRlY3QtcHJvamVjdC5zaAogICAgICAgICAgcHJpbnRmICclcycgJ0l5RXZkWE55TDJKcGJpOWxibllnWW1GemFBcHpaWFFnTFdWMWJ5QndhWEJsWm1GcGJBcFFVazlLUlVOVVBTSWtlekU2UDMwaUNrOVZWRDBpSkhzeU9qOTlJZ29LWTJRZ0lpUlFVazlLUlVOVUlnb0taRzkzYm14dllXUmZaM0poWkd4bEtDa2dld29nSUd4dlkyRnNJSFpsY2owaUpERWlDaUFnYkc5allXd2daR2x5UFNJa1VsVk9Ua1ZTWDFSRlRWQXZjbVZ3YnpKd2JHRjVMV2R5WVdSc1pTMGtkbVZ5SWdvZ0lHbG1JRnNnSVNBdGVDQWlKR1JwY2k5bmNtRmtiR1V0SkhabGNpOWlhVzR2WjNKaFpHeGxJaUJkT3lCMGFHVnVDaUFnSUNCeWJTQXRjbVlnSWlSa2FYSWlPeUJ0YTJScGNpQXRjQ0FpSkdScGNpSUtJQ0FnSUdOMWNtd2dMV1p6VTB3Z0xTMXlaWFJ5ZVNBeklDSm9kSFJ3Y3pvdkwzTmxjblpwWTJWekxtZHlZV1JzWlM1dmNtY3ZaR2x6ZEhKcFluVjBhVzl1Y3k5bmNtRmtiR1V0Skh0MlpYSjlMV0pwYmk1NmFYQWlJQzF2SUNJa1pHbHlMMmR5WVdSc1pTNTZhWEFpQ2lBZ0lDQjFibnBwY0NBdGNTQWlKR1JwY2k5bmNtRmtiR1V1ZW1sd0lpQXRaQ0FpSkdScGNpSUtJQ0JtYVFvZ0lIQnlhVzUwWmlBbkpYTmNiaWNnSWlSa2FYSXZaM0poWkd4bExTUjJaWEl2WW1sdUwyZHlZV1JzWlNJS2ZRb0thV1lnV3lBdFppQm5jbUZrYkdWM0lGMDdJSFJvWlc0S0lDQmphRzF2WkNBcmVDQm5jbUZrYkdWM0NpQWdRMDFFUFNJa1VGSlBTa1ZEVkM5bmNtRmtiR1YzSWdvZ0lHVmphRzhnSWxCQlUxTWdSWGhwYzNScGJtY2dSM0poWkd4bElIZHlZWEJ3WlhJaUNtVnNhV1lnV3lBdFppQm5jbUZrYkdVdmQzSmhjSEJsY2k5bmNtRmtiR1V0ZDNKaGNIQmxjaTV3Y205d1pYSjBhV1Z6SUYwN0lIUm9aVzRLSUNCVlVrdzlJaVFvWjNKbGNDQW5YbVJwYzNSeWFXSjFkR2x2YmxWeWJEMG5JR2R5WVdSc1pTOTNjbUZ3Y0dWeUwyZHlZV1JzWlMxM2NtRndjR1Z5TG5CeWIzQmxjblJwWlhNZ2ZDQmpkWFFnTFdROUlDMW1NaTBnZkNCelpXUWdKM01qWEZ3Nkl6b2paeWNnZkh3Z2RISjFaU2tpQ2lBZ1ZrVlNQU0lrS0hCeWFXNTBaaUFuSlhNbklDSWtWVkpNSWlCOElITmxaQ0F0YmlBbmN5OHVLbWR5WVdSc1pTMWNLRnN3TFRsZFd6QXRPUzVkS2x3cExTNHFMMXd4TDNBbktTSUtJQ0JiSUMxdUlDSWtWa1ZTSWlCZElIeDhJR1Y0YVhRZ01Rb2dJR1ZqYUc4Z0lsSkZRMDlXUlZKWklFMXBjM05wYm1jZ1ozSmhaR3hsZHpzZ2RYTnBibWNnUjNKaFpHeGxJQ1JXUlZJaUNpQWdRMDFFUFNJa0tHUnZkMjVzYjJGa1gyZHlZV1JzWlNBaUpGWkZVaUlwSWdwbGJITmxDaUFnSXlCSFpXNWxjbUZzSUdaaGJHeGlZV05ySUdadmNpQnRiMlJsY200Z1FXNWtjbTlwWkNCd2NtOXFaV04wY3pzZ1luVnBiR1FnWlhKeWIzSnpJR0Z5WlNCc1lYUmxjaUJ5WlhCdmNuUmxaQ0JqYkdWaGNteDVMZ29nSUZaRlVqMGlPQzQ1SWdvZ0lHVmphRzhnSWxKRlEwOVdSVkpaSUU1dklIZHlZWEJ3WlhJZ2JXVjBZV1JoZEdFN0lIUnllV2x1WnlCSGNtRmtiR1VnSkZaRlVpSUtJQ0JEVFVROUlpUW9aRzkzYm14dllXUmZaM0poWkd4bElDSWtWa1ZTSWlraUNtWnBDZ29pSkVOTlJDSWdMUzEyWlhKemFXOXVDbkJ5YVc1MFppQW5SMUpCUkV4RlgwTk5SRDBsY1Z4dUp5QWlKRU5OUkNJZ1BpQWlKRTlWVkNJSycgfCBiYXNlNjQgLS1kZWNvZGUgPiAucmVwbzJwbGF5L3NjcmlwdHMvcmVzb2x2ZS1ncmFkbGUuc2gKICAgICAgICAgIHByaW50ZiAnJXMnICdJeUV2ZFhOeUwySnBiaTlsYm5ZZ1ltRnphQXB6WlhRZ0xXVjFieUJ3YVhCbFptRnBiQXBRVWs5S1JVTlVQU0lrZXpFNlAzMGlDa2RTUVVSTVJUMGlKSHN5T2o5OUlncE5UMFJWVEVVOUlpUjdNeTE5SWdwUFZWUlFWVlE5SWlSN05Eby9mU0lLQ21Oa0lDSWtVRkpQU2tWRFZDSUtVRkpGUmtsWVBTSWlDbHNnTFc0Z0lpUk5UMFJWVEVVaUlGMGdKaVlnVUZKRlJrbFlQU0k2Skh0TlQwUlZURVY5T2lJS0NtVmphRzhnSWtKMWFXeGthVzVuSUVGUVN5NHVMaUlLSWlSSFVrRkVURVVpSUNJa2UxQlNSVVpKV0gxaGMzTmxiV0pzWlZKbGJHVmhjMlVpSUMwdGJtOHRaR0ZsYlc5dUlDMHRjM1JoWTJ0MGNtRmpaUW9LWldOb2J5QWlRblZwYkdScGJtY2dRVUZDTGk0dUlnb2lKRWRTUVVSTVJTSWdJaVI3VUZKRlJrbFlmV0oxYm1Sc1pWSmxiR1ZoYzJVaUlDMHRibTh0WkdGbGJXOXVJQzB0YzNSaFkydDBjbUZqWlFvS1FWQkxQU0lrS0dacGJtUWdJaVJRVWs5S1JVTlVJaUF0ZEhsd1pTQm1JQzF1WVcxbElDY3FMbUZ3YXljZ0lTQXRjR0YwYUNBbktpOXBiblJsY20xbFpHbGhkR1Z6THlvbklId2daM0psY0NBbkwzSmxiR1ZoYzJVdkp5QjhJR2hsWVdRZ0xURWdmSHdnZEhKMVpTa2lDa0ZCUWowaUpDaG1hVzVrSUNJa1VGSlBTa1ZEVkNJZ0xYUjVjR1VnWmlBdGJtRnRaU0FuS2k1aFlXSW5JQ0VnTFhCaGRHZ2dKeW92YVc1MFpYSnRaV1JwWVhSbGN5OHFKeUI4SUdkeVpYQWdKeTl5Wld4bFlYTmxMeWNnZkNCb1pXRmtJQzB4SUh4OElIUnlkV1VwSWdvS1d5QXRiaUFpSkVGUVN5SWdYU0I4ZkNCN0lHVmphRzhnSWtGUVN5QnViM1FnWm05MWJtUWlPeUJsZUdsMElERTdJSDBLV3lBdGJpQWlKRUZCUWlJZ1hTQjhmQ0I3SUdWamFHOGdJa0ZCUWlCdWIzUWdabTkxYm1RaU95QmxlR2wwSURFN0lIMEtDbU53SUNJa1FWQkxJaUFpSkU5VlZGQlZWQzloY0hBdGNtVnNaV0Z6WlMxMWJuTnBaMjVsWkM1aGNHc2lDbU53SUNJa1FVRkNJaUFpSkU5VlZGQlZWQzloY0hBdGNtVnNaV0Z6WlMxMWJuTnBaMjVsWkM1aFlXSWlDZ3BsWTJodklDSlFRVk5USUVGUVN5QmlkV2xzWkNJS1pXTm9ieUFpVUVGVFV5QkJRVUlnWW5WcGJHUWlDZz09JyB8IGJhc2U2NCAtLWRlY29kZSA+IC5yZXBvMnBsYXkvc2NyaXB0cy9idWlsZC5zaAogICAgICAgICAgcHJpbnRmICclcycgJ0l5RXZkWE55TDJKcGJpOWxibllnWW1GemFBcHpaWFFnTFdWMWJ5QndhWEJsWm1GcGJBb0tUMVZVVUZWVVBTSWtlekU2UDMwaUNrMVBSRVU5SWlSN1FsVkpURVJmVFU5RVJUb3RUa1ZYZlNJS1MwVlpVMVJQVWtVOUlpUlBWVlJRVlZRdmNtVndiekp3YkdGNUxYVndiRzloWkM1cWEzTWlDa0ZNU1VGVFBTSWtlMU5KUjA1SlRrZGZTMFZaWDBGTVNVRlRPaTF5WlhCdk1uQnNZWGw5SWdwVFZFOVNSVkJCVTFNOUlpUjdVMGxIVGtsT1IxOVRWRTlTUlY5UVFWTlRWMDlTUkRvdFVtVndiekpRYkdGNU1USXpJWDBpQ2t0RldWQkJVMU05SWlSN1UwbEhUa2xPUjE5TFJWbGZVRUZUVTFkUFVrUTZMVkpsY0c4eVVHeGhlVEV5TXlGOUlnb0thV1lnV3lBaUpFMVBSRVVpSUQwZ0lsVlFSRUZVUlNJZ1hUc2dkR2hsYmdvZ0lGc2dMVzRnSWlSN1FWQlFYMHRGV1ZOVVQxSkZYMEpCVTBVMk5Eb3RmU0lnWFNCOGZDQjdJR1ZqYUc4Z0lsVlFSRUZVUlNCdGIyUmxJSEpsY1hWcGNtVnpJRUZRVUY5TFJWbFRWRTlTUlY5Q1FWTkZOalFpT3lCbGVHbDBJREU3SUgwS0lDQndjbWx1ZEdZZ0p5VnpKeUFpSkVGUVVGOUxSVmxUVkU5U1JWOUNRVk5GTmpRaUlId2dZbUZ6WlRZMElDMHRaR1ZqYjJSbElENGdJaVJMUlZsVFZFOVNSU0lLSUNCYklDMXpJQ0lrUzBWWlUxUlBVa1VpSUYwZ2ZId2dleUJsWTJodklDSkVaV052WkdWa0lHdGxlWE4wYjNKbElHbHpJR1Z0Y0hSNUlqc2daWGhwZENBeE95QjlDaUFnWldOb2J5QWlVRUZUVXlCRmVHbHpkR2x1WnlCclpYbHpkRzl5WlNCc2IyRmtaV1FnWm05eUlGVlFSRUZVUlNJS1pXeHpaUW9nSUd0bGVYUnZiMndnTFdkbGJtdGxlWEJoYVhJZ1hBb2dJQ0FnTFd0bGVYTjBiM0psSUNJa1MwVlpVMVJQVWtVaUlGd0tJQ0FnSUMxemRHOXlaWEJoYzNNZ0lpUlRWRTlTUlZCQlUxTWlJRndLSUNBZ0lDMXJaWGx3WVhOeklDSWtTMFZaVUVGVFV5SWdYQW9nSUNBZ0xXRnNhV0Z6SUNJa1FVeEpRVk1pSUZ3S0lDQWdJQzFyWlhsaGJHY2dVbE5CSUZ3S0lDQWdJQzFyWlhsemFYcGxJREl3TkRnZ1hBb2dJQ0FnTFhaaGJHbGthWFI1SURFd01EQXdJRndLSUNBZ0lDMWtibUZ0WlNBaVEwNDlVbVZ3YnpKUWJHRjVMQ0JQVlQxQmJtUnliMmxrTENCUFBWSmxjRzh5VUd4aGVTd2dURDFWYm10dWIzZHVMQ0JUVkQxVmJtdHViM2R1TENCRFBWVlRJaUJjQ2lBZ0lDQStMMlJsZGk5dWRXeHNJREkrSmpFS0lDQmxZMmh2SUNKUVFWTlRJRTVsZHlCclpYbHpkRzl5WlNCblpXNWxjbUYwWldRaUNtWnBDZ3BDVlVsTVJGOVVUMDlNVXowaUpDaG1hVzVrSUNJa1FVNUVVazlKUkY5SVQwMUZMMkoxYVd4a0xYUnZiMnh6SWlBdGJXbHVaR1Z3ZEdnZ01TQXRiV0Y0WkdWd2RHZ2dNU0F0ZEhsd1pTQmtJSHdnYzI5eWRDQXRWaUI4SUhSaGFXd2dMVEVwSWdwYklDMXVJQ0lrUWxWSlRFUmZWRTlQVEZNaUlGMGdmSHdnZXlCbFkyaHZJQ0pCYm1SeWIybGtJR0oxYVd4a0xYUnZiMnh6SUc1dmRDQm1iM1Z1WkNJN0lHVjRhWFFnTVRzZ2ZRb0tXa2xRUVV4SlIwNDlJaVJDVlVsTVJGOVVUMDlNVXk5NmFYQmhiR2xuYmlJS1FWQkxVMGxIVGtWU1BTSWtRbFZKVEVSZlZFOVBURk12WVhCcmMybG5ibVZ5SWdvS1d5QXRlQ0FpSkZwSlVFRk1TVWRPSWlCZElIeDhJSHNnWldOb2J5QWllbWx3WVd4cFoyNGdibTkwSUdadmRXNWtJanNnWlhocGRDQXhPeUI5Q2xzZ0xYZ2dJaVJCVUV0VFNVZE9SVklpSUYwZ2ZId2dleUJsWTJodklDSmhjR3R6YVdkdVpYSWdibTkwSUdadmRXNWtJanNnWlhocGRDQXhPeUI5Q2dvaUpGcEpVRUZNU1VkT0lpQXRaaUEwSUNJa1QxVlVVRlZVTDJGd2NDMXlaV3hsWVhObExYVnVjMmxuYm1Wa0xtRndheUlnSWlSUFZWUlFWVlF2WVhCd0xYSmxiR1ZoYzJVdFlXeHBaMjVsWkM1aGNHc2lDZ29pSkVGUVMxTkpSMDVGVWlJZ2MybG5iaUJjQ2lBZ0xTMXJjeUFpSkV0RldWTlVUMUpGSWlCY0NpQWdMUzFyY3kxclpYa3RZV3hwWVhNZ0lpUkJURWxCVXlJZ1hBb2dJQzB0YTNNdGNHRnpjeUFpY0dGemN6b2tVMVJQVWtWUVFWTlRJaUJjQ2lBZ0xTMXJaWGt0Y0dGemN5QWljR0Z6Y3pva1MwVlpVRUZUVXlJZ1hBb2dJQzB0YjNWMElDSWtUMVZVVUZWVUwyRndjQzF5Wld4bFlYTmxMWE5wWjI1bFpDNWhjR3NpSUZ3S0lDQWlKRTlWVkZCVlZDOWhjSEF0Y21Wc1pXRnpaUzFoYkdsbmJtVmtMbUZ3YXlJS0NpSWtRVkJMVTBsSFRrVlNJaUIyWlhKcFpua2dMUzEyWlhKaWIzTmxJQ0lrVDFWVVVGVlVMMkZ3Y0MxeVpXeGxZWE5sTFhOcFoyNWxaQzVoY0dzaUlENHZaR1YyTDI1MWJHd0taV05vYnlBaVVFRlRVeUJCVUVzZ2MybG5ibUYwZFhKbElGWkZVa2xHU1VWRUlnb0tZM0FnSWlSUFZWUlFWVlF2WVhCd0xYSmxiR1ZoYzJVdGRXNXphV2R1WldRdVlXRmlJaUFpSkU5VlZGQlZWQzloY0hBdGNtVnNaV0Z6WlMxemFXZHVaV1F1WVdGaUlnb0thbUZ5YzJsbmJtVnlJRndLSUNBdGMybG5ZV3huSUZOSVFUSTFObmRwZEdoU1UwRWdYQW9nSUMxa2FXZGxjM1JoYkdjZ1UwaEJMVEkxTmlCY0NpQWdMV3RsZVhOMGIzSmxJQ0lrUzBWWlUxUlBVa1VpSUZ3S0lDQXRjM1J2Y21Wd1lYTnpJQ0lrVTFSUFVrVlFRVk5USWlCY0NpQWdMV3RsZVhCaGMzTWdJaVJMUlZsUVFWTlRJaUJjQ2lBZ0lpUlBWVlJRVlZRdllYQndMWEpsYkdWaGMyVXRjMmxuYm1Wa0xtRmhZaUlnWEFvZ0lDSWtRVXhKUVZNaUlGd0tJQ0ErTDJSbGRpOXVkV3hzQ2dwcVlYSnphV2R1WlhJZ0xYWmxjbWxtZVNBaUpFOVZWRkJWVkM5aGNIQXRjbVZzWldGelpTMXphV2R1WldRdVlXRmlJaUErTDJSbGRpOXVkV3hzQ21WamFHOGdJbEJCVTFNZ1FVRkNJSE5wWjI1aGRIVnlaU0JXUlZKSlJrbEZSQ0lLQ25zS0lDQmxZMmh2SUNKU1JWQlBNbEJNUVZrZ1UwbEhUa2xPUnlCSlRrWlBJZ29nSUdWamFHOGdJajA5UFQwOVBUMDlQVDA5UFQwOVBUMDlQVDA5UFQwaUNpQWdaV05vYnlBaVRXOWtaVG9nSkUxUFJFVWlDaUFnWldOb2J5QWlRV3hwWVhNNklDUkJURWxCVXlJS0lDQnJaWGwwYjI5c0lDMXNhWE4wSUMxMklDMXJaWGx6ZEc5eVpTQWlKRXRGV1ZOVVQxSkZJaUF0YzNSdmNtVndZWE56SUNJa1UxUlBVa1ZRUVZOVElpQXRZV3hwWVhNZ0lpUkJURWxCVXlJZ1hBb2dJQ0FnZkNCbmNtVndJQzFGSUNkVFNFRXhPbnhUU0VFeU5UWTZKeUI4ZkNCMGNuVmxDbjBnUGlBaUpFOVZWRkJWVkM5VFNVZE9TVTVITFVsT1JrOHVkSGgwSWdvS2FXWWdXeUFpSkUxUFJFVWlJRDBnSWs1RlZ5SWdYVHNnZEdobGJnb2dJSHNLSUNBZ0lHVmphRzhnSWtsTlVFOVNWRUZPVkNJS0lDQWdJR1ZqYUc4Z0lqMDlQVDA5UFQwOVBTSUtJQ0FnSUdWamFHOGdJa3RsWlhBZ2NtVndiekp3YkdGNUxYVndiRzloWkM1cWEzTXNJR0ZzYVdGeklHRnVaQ0J3WVhOemQyOXlaSE1nYzJGbVpTNGlDaUFnSUNCbFkyaHZJQ0paYjNVZ2JtVmxaQ0IwYUdVZ2MyRnRaU0J6YVdkdWFXNW5JR2xrWlc1MGFYUjVJR1p2Y2lCbWRYUjFjbVVnZFhCa1lYUmxjeUJ2ZFhSemFXUmxJRkJzWVhrZ1FYQndJRk5wWjI1cGJtY2dkMjl5YTJac2IzZHpMaUlLSUNCOUlENCtJQ0lrVDFWVVVGVlVMMU5KUjA1SlRrY3RTVTVHVHk1MGVIUWlDbVpwQ2c9PScgfCBiYXNlNjQgLS1kZWNvZGUgPiAucmVwbzJwbGF5L3NjcmlwdHMvc2lnbi5zaAogICAgICAgICAgcHJpbnRmICclcycgJ0l5RXZkWE55TDJKcGJpOWxibllnWW1GemFBcHpaWFFnTFdWMWJ5QndhWEJsWm1GcGJBcFFVazlLUlVOVVBTSWtlekU2UDMwaUNrOVZWRkJWVkQwaUpIc3lPajk5SWdwU1BTSWtUMVZVVUZWVUwxQk1RVmt0VWtWUVQxSlVMblI0ZENJS0Nuc0tJQ0JsWTJodklDSlNSVkJQTWxCTVFWa2dWakV4SUZCTVFWa2dVMVJQVWtVZ1JFOURWRTlTSWdvZ0lHVmphRzhnSWowOVBUMDlQVDA5UFQwOVBUMDlQVDA5UFQwOVBUMDlQVDA5UFQwOVBUMGlDaUFnWldOb2J3b0tJQ0JwWmlCbWFXNWtJQ0lrVUZKUFNrVkRWQ0lnTFhSNWNHVWdaaUF0Ym1GdFpTQkJibVJ5YjJsa1RXRnVhV1psYzNRdWVHMXNJQ0VnTFhCaGRHZ2dKeW92WW5WcGJHUXZLaWNnZkNCbmNtVndJQzF4SUM0N0lIUm9aVzRLSUNBZ0lHVmphRzhnSWxCQlUxTWdRVzVrY205cFpFMWhibWxtWlhOMElHWnZkVzVrSWdvZ0lHVnNjMlVLSUNBZ0lHVmphRzhnSWxkQlVrNUpUa2NnUVc1a2NtOXBaRTFoYm1sbVpYTjBJRzV2ZENCbWIzVnVaQ0lLSUNCbWFRb0tJQ0JVUVZKSFJWUTlJaVFvWjNKbGNDQXRVbWh2UlNBbmRHRnlaMlYwVTJScktGWmxjbk5wYjI0cFAxdGJPbk53WVdObE9sMDlLQ2xkSzFzd0xUbGRLeWNnSWlSUVVrOUtSVU5VSWlBdExXbHVZMngxWkdVOUp5b3VaM0poWkd4bEp5QXRMV2x1WTJ4MVpHVTlKeW91WjNKaFpHeGxMbXQwY3ljZ01qNHZaR1YyTDI1MWJHd2dmQ0JuY21Wd0lDMXZSU0FuV3pBdE9WMHJKeUI4SUhOdmNuUWdMVzV5SUh3Z2FHVmhaQ0F0TVNCOGZDQjBjblZsS1NJS0lDQkRUMDFRU1V4RlBTSWtLR2R5WlhBZ0xWSm9iMFVnSjJOdmJYQnBiR1ZUWkdzb1ZtVnljMmx2YmlrL1cxczZjM0JoWTJVNlhUMG9LVjByV3pBdE9WMHJKeUFpSkZCU1QwcEZRMVFpSUMwdGFXNWpiSFZrWlQwbktpNW5jbUZrYkdVbklDMHRhVzVqYkhWa1pUMG5LaTVuY21Ga2JHVXVhM1J6SnlBeVBpOWtaWFl2Ym5Wc2JDQjhJR2R5WlhBZ0xXOUZJQ2RiTUMwNVhTc25JSHdnYzI5eWRDQXRibklnZkNCb1pXRmtJQzB4SUh4OElIUnlkV1VwSWdvS0lDQmxZMmh2SUNKRVpYUmxZM1JsWkNCMFlYSm5aWFJUWkdzNklDUjdWRUZTUjBWVU9pMTFibXR1YjNkdWZTSUtJQ0JsWTJodklDSkVaWFJsWTNSbFpDQmpiMjF3YVd4bFUyUnJPaUFrZTBOUFRWQkpURVU2TFhWdWEyNXZkMjU5SWdvS0lDQWpJRWR2YjJkc1pTQlFiR0Y1SUcxdlltbHNaU0J3YjJ4cFkza2dZbUZ6Wld4cGJtVWdabTl5SUc1bGR5QmhjSEJ6SUdGdVpDQjFjR1JoZEdWeklHWnliMjBnTWpBeU5pMHdPQzB6TVM0S0lDQnBaaUJiSUNJa2UxUkJVa2RGVkRvdE1IMGlJQzFuWlNBek5pQmRJREkrTDJSbGRpOXVkV3hzT3lCMGFHVnVDaUFnSUNCbFkyaHZJQ0pRUVZOVElIUmhjbWRsZEZOa2F5QnRaV1YwY3lCQlVFa2dNellnYlc5aWFXeGxJSE4xWW0xcGMzTnBiMjRnWW1GelpXeHBibVVpQ2lBZ1pXeHpaUW9nSUNBZ1pXTm9ieUFpVjBGU1RrbE9SeUIwWVhKblpYUlRaR3NnWkc5bGN5QnViM1FnYldWbGRDQkJVRWtnTXpZZ2JXOWlhV3hsSUhOMVltMXBjM05wYjI0Z1ltRnpaV3hwYm1VZ1pXWm1aV04wYVhabElESXdNall0TURndE16RWlDaUFnWm1rS0NpQWdhV1lnV3lBaUpIdERUMDFRU1V4Rk9pMHdmU0lnTFdkbElETTJJRjBnTWo0dlpHVjJMMjUxYkd3N0lIUm9aVzRLSUNBZ0lHVmphRzhnSWxCQlUxTWdZMjl0Y0dsc1pWTmtheUErUFNBek5pSUtJQ0JsYkhObENpQWdJQ0JsWTJodklDSlhRVkpPU1U1SElHTnZiWEJwYkdWVFpHc2dZbVZzYjNjZ016WWdiM0lnYm05MElHUmxkR1ZqZEdWa0lnb2dJR1pwQ2dvZ0lHbG1JR2R5WlhBZ0xWSWdMWEVnSjJGdVpISnZhV1E2WkdWaWRXZG5ZV0pzWlQwaWRISjFaU0luSUNJa1VGSlBTa1ZEVkNJZ0xTMXBibU5zZFdSbFBTZEJibVJ5YjJsa1RXRnVhV1psYzNRdWVHMXNKeUF5UGk5a1pYWXZiblZzYkRzZ2RHaGxiZ29nSUNBZ1pXTm9ieUFpVjBGU1RrbE9SeUJrWldKMVoyZGhZbXhsUFhSeWRXVWdaR1YwWldOMFpXUWlDaUFnWld4elpRb2dJQ0FnWldOb2J5QWlVRUZUVXlCdWJ5QmxlSEJzYVdOcGRDQmtaV0oxWjJkaFlteGxQWFJ5ZFdVaUNpQWdabWtLQ2lBZ2FXWWdaM0psY0NBdFVpQXRjU0FuWVc1a2NtOXBaRHAxYzJWelEyeGxZWEowWlhoMFZISmhabVpwWXowaWRISjFaU0luSUNJa1VGSlBTa1ZEVkNJZ0xTMXBibU5zZFdSbFBTZEJibVJ5YjJsa1RXRnVhV1psYzNRdWVHMXNKeUF5UGk5a1pYWXZiblZzYkRzZ2RHaGxiZ29nSUNBZ1pXTm9ieUFpVjBGU1RrbE9SeUJqYkdWaGNuUmxlSFFnZEhKaFptWnBZeUJsZUhCc2FXTnBkR3g1SUdWdVlXSnNaV1FpQ2lBZ1pXeHpaUW9nSUNBZ1pXTm9ieUFpVUVGVFV5QnVieUJsZUhCc2FXTnBkQ0JqYkdWaGNuUmxlSFFnZEhKaFptWnBZeUJsYm1GaWJHVnRaVzUwSWdvZ0lHWnBDZ29nSUVWWVVFOVNWRVZFUFNJa0tHZHlaWEFnTFZKb2J5QW5ZVzVrY205cFpEcGxlSEJ2Y25SbFpEMGlkSEoxWlNJbklDSWtVRkpQU2tWRFZDSWdMUzFwYm1Oc2RXUmxQU2RCYm1SeWIybGtUV0Z1YVdabGMzUXVlRzFzSnlBeVBpOWtaWFl2Ym5Wc2JDQjhJSGRqSUMxc0lId2dkSElnTFdRZ0p5QW5LU0lLSUNCbFkyaHZJQ0pKVGtaUElHVjRjRzl5ZEdWa1BYUnlkV1VnWTI5dGNHOXVaVzUwY3pvZ0pIdEZXRkJQVWxSRlJEb3RNSDBpQ2dvZ0lHbG1JRnNnTFdZZ0lpUlBWVlJRVlZRdllYQndMWEpsYkdWaGMyVXRjMmxuYm1Wa0xtRndheUlnWFRzZ2RHaGxiZ29nSUNBZ1pXTm9ieUFpVUVGVFV5QnphV2R1WldRZ1FWQkxJSEJ5WlhObGJuUWlDaUFnWld4elpRb2dJQ0FnWldOb2J5QWlRa3hQUTB0RlVpQnphV2R1WldRZ1FWQkxJRzFwYzNOcGJtY2lDaUFnWm1rS0NpQWdhV1lnV3lBdFppQWlKRTlWVkZCVlZDOWhjSEF0Y21Wc1pXRnpaUzF6YVdkdVpXUXVZV0ZpSWlCZE95QjBhR1Z1Q2lBZ0lDQmxZMmh2SUNKUVFWTlRJSE5wWjI1bFpDQkJRVUlnY0hKbGMyVnVkQ0lLSUNCbGJITmxDaUFnSUNCbFkyaHZJQ0pDVEU5RFMwVlNJSE5wWjI1bFpDQkJRVUlnYldsemMybHVaeUlLSUNCbWFRb0tJQ0JsWTJodkNpQWdaV05vYnlBaVVFOU1TVU5aSUU1UFZFVWlDaUFnWldOb2J5QWlVM1JoY25ScGJtY2dNakF5Tmkwd09DMHpNU3dnYm1WM0lHMXZZbWxzWlNCaGNIQnpJR0Z1WkNCaGNIQWdkWEJrWVhSbGN5QnpkV0p0YVhSMFpXUWdkRzhnUjI5dloyeGxJRkJzWVhrZ2JYVnpkQ0IwWVhKblpYUWdRVzVrY205cFpDQXhOaUF2SUVGUVNTQXpOaUJ2Y2lCb2FXZG9aWEl1SWdvZ0lHVmphRzhnSWxSb2FYTWdhWE1nWVNCMFpXTm9ibWxqWVd3Z2NISmxabXhwWjJoMExDQnViM1FnWVNCbmRXRnlZVzUwWldVZ2IyWWdSMjl2WjJ4bElGQnNZWGtnWVhCd2NtOTJZV3d1SWdwOUlENGdJaVJTSWdvS1kyRjBJQ0lrVWlJSycgfCBiYXNlNjQgLS1kZWNvZGUgPiAucmVwbzJwbGF5L3NjcmlwdHMvZG9jdG9yLnNoCiAgICAgICAgICBwcmludGYgJyVzJyAnSXlFdmRYTnlMMkpwYmk5bGJuWWdZbUZ6YUFwelpYUWdMV1YxYnlCd2FYQmxabUZwYkFwUFZWUlFWVlE5SWlSN01Uby9mU0lLQ2xzZ0xXWWdJaVJQVlZSUVZWUXZZWEJ3TFhKbGJHVmhjMlV0YzJsbmJtVmtMbUZ3YXlJZ1hTQjhmQ0I3SUdWamFHOGdJbE5wWjI1bFpDQkJVRXNnYldsemMybHVaeUk3SUdWNGFYUWdNVHNnZlFwYklDMW1JQ0lrVDFWVVVGVlVMMkZ3Y0MxeVpXeGxZWE5sTFhOcFoyNWxaQzVoWVdJaUlGMGdmSHdnZXlCbFkyaHZJQ0pUYVdkdVpXUWdRVUZDSUcxcGMzTnBibWNpT3lCbGVHbDBJREU3SUgwS1d5QXRaaUFpSkU5VlZGQlZWQzlUU1VkT1NVNUhMVWxPUms4dWRIaDBJaUJkSUh4OElIc2daV05vYnlBaVUybG5ibWx1WnlCeVpYQnZjblFnYldsemMybHVaeUk3SUdWNGFYUWdNVHNnZlFvS2NtMGdMV1lnSWlSUFZWUlFWVlF2WVhCd0xYSmxiR1ZoYzJVdGRXNXphV2R1WldRdVlYQnJJaUFpSkU5VlZGQlZWQzloY0hBdGNtVnNaV0Z6WlMxaGJHbG5ibVZrTG1Gd2F5SWdJaVJQVlZSUVZWUXZZWEJ3TFhKbGJHVmhjMlV0ZFc1emFXZHVaV1F1WVdGaUlpQWlKRTlWVkZCVlZDOWtaWFJsWTNRdVpXNTJJaUFpSkU5VlZGQlZWQzluY21Ga2JHVXVaVzUySWdvS0tBb2dJR05rSUNJa1QxVlVVRlZVSWdvZ0lITm9ZVEkxTm5OMWJTQmhjSEF0Y21Wc1pXRnpaUzF6YVdkdVpXUXVZWEJySUdGd2NDMXlaV3hsWVhObExYTnBaMjVsWkM1aFlXSWdQaUJUU0VFeU5UWlRWVTFUTG5SNGRBb3BDZ3BsWTJodklDSlFRVk5USUVacGJtRnNJSE5wWjI1bFpDQkJVRXN2UVVGQ0lIQmhZMnRoWjJVZ2NISmxjR0Z5WldRaUNnPT0nIHwgYmFzZTY0IC0tZGVjb2RlID4gLnJlcG8ycGxheS9zY3JpcHRzL3BhY2thZ2Uuc2gKICAgICAgICAgIHByaW50ZiAnJXMnICdJeUV2ZFhOeUwySnBiaTlsYm5ZZ2NIbDBhRzl1TXdwcGJYQnZjblFnY21Vc0lITjVjeXdnYW5OdmJncG1jbTl0SUhCaGRHaHNhV0lnYVcxd2IzSjBJRkJoZEdnS0NuQnliMnBsWTNRZ1BTQlFZWFJvS0hONWN5NWhjbWQyV3pGZEtTNXlaWE52YkhabEtDa0tiVzlrZFd4bElEMGdjM2x6TG1GeVozWmJNbDBnYVdZZ2JHVnVLSE41Y3k1aGNtZDJLU0ErSURJZ1pXeHpaU0FpSWdwdGIyUmxJRDBnS0hONWN5NWhjbWQyV3pOZElHbG1JR3hsYmloemVYTXVZWEpuZGlrZ1BpQXpJR1ZzYzJVZ0lrNUZWeUlwTG5Wd2NHVnlLQ2tLYjNWMElEMGdVR0YwYUNoemVYTXVZWEpuZGxzMFhTa3VjbVZ6YjJ4MlpTZ3BDZ3B0YjJSMWJHVmZaR2x5SUQwZ2NISnZhbVZqZENCcFppQnViM1FnYlc5a2RXeGxJR1ZzYzJVZ2NISnZhbVZqZEM1cWIybHVjR0YwYUNncWJXOWtkV3hsTG5Od2JHbDBLQ0k2SWlrcENtTmhibVJwWkdGMFpYTWdQU0JiYlc5a2RXeGxYMlJwY2lBdklDSmlkV2xzWkM1bmNtRmtiR1V1YTNSeklpd2diVzlrZFd4bFgyUnBjaUF2SUNKaWRXbHNaQzVuY21Ga2JHVWlYUXBpZFdsc1pGOW1hV3hsSUQwZ2JtVjRkQ2dvY0NCbWIzSWdjQ0JwYmlCallXNWthV1JoZEdWeklHbG1JSEF1WlhocGMzUnpLQ2twTENCT2IyNWxLUXBwWmlCdWIzUWdZblZwYkdSZlptbHNaVG9LSUNBZ0lIQnlhVzUwS0NKV1JWSlRTVTlPSUVWU1VrOVNPaUJoY0hBZ1luVnBiR1F1WjNKaFpHeGxLQzVyZEhNcElHNXZkQ0JtYjNWdVpDSXBDaUFnSUNCemVYTXVaWGhwZENneEtRb0tkR1Y0ZENBOUlHSjFhV3hrWDJacGJHVXVjbVZoWkY5MFpYaDBLR1Z1WTI5a2FXNW5QU0oxZEdZdE9DSXBDbTl5YVdjZ1BTQjBaWGgwQ2dwMlkxOXdZWFIwWlhKdWN5QTlJRnNLSUNBZ0lISW5LRnhpZG1WeWMybHZia052WkdWY2N5bzlYSE1xS1NoY1pDc3BKeXdLSUNBZ0lISW5LRnhpZG1WeWMybHZia052WkdWY2N5c3BLRnhrS3lrbkxBcGRDblp1WDNCaGRIUmxjbTV6SUQwZ1d3b2dJQ0FnY2ljb1hHSjJaWEp6YVc5dVRtRnRaVnh6S2oxY2N5cGJJbHduWFNrb1cxNGlYQ2RkS3lrb1d5SmNKMTBwSnl3S0lDQWdJSEluS0Z4aWRtVnljMmx2Yms1aGJXVmNjeXRiSWx3blhTa29XMTRpWENkZEt5a29XeUpjSjEwcEp5d0tYUW9LZG1WeWMybHZibDlqYjJSbElEMGdUbTl1WlFwbWIzSWdjR0YwSUdsdUlIWmpYM0JoZEhSbGNtNXpPZ29nSUNBZ2JTQTlJSEpsTG5ObFlYSmphQ2h3WVhRc0lIUmxlSFFwQ2lBZ0lDQnBaaUJ0T2dvZ0lDQWdJQ0FnSUhabGNuTnBiMjVmWTI5a1pTQTlJR2x1ZENodExtZHliM1Z3S0RJcEtRb2dJQ0FnSUNBZ0lIWmpYM0JoZENBOUlIQmhkQW9nSUNBZ0lDQWdJR0p5WldGckNncDJaWEp6YVc5dVgyNWhiV1VnUFNCT2IyNWxDbVp2Y2lCd1lYUWdhVzRnZG01ZmNHRjBkR1Z5Ym5NNkNpQWdJQ0J0SUQwZ2NtVXVjMlZoY21Ob0tIQmhkQ3dnZEdWNGRDa0tJQ0FnSUdsbUlHMDZDaUFnSUNBZ0lDQWdkbVZ5YzJsdmJsOXVZVzFsSUQwZ2JTNW5jbTkxY0NneUtRb2dJQ0FnSUNBZ0lIWnVYM0JoZENBOUlIQmhkQW9nSUNBZ0lDQWdJR0p5WldGckNncHlaWE4xYkhRZ1BTQjdDaUFnSUNBaVptbHNaU0k2SUhOMGNpaGlkV2xzWkY5bWFXeGxLU3dLSUNBZ0lDSnRiMlJsSWpvZ2JXOWtaU3dLSUNBZ0lDSnZiR1JmZG1WeWMybHZia052WkdVaU9pQjJaWEp6YVc5dVgyTnZaR1VzQ2lBZ0lDQWliMnhrWDNabGNuTnBiMjVPWVcxbElqb2dkbVZ5YzJsdmJsOXVZVzFsTEFvZ0lDQWdJbU5vWVc1blpXUWlPaUJHWVd4elpTd0tmUW9LYVdZZ2JXOWtaU0E5UFNBaVZWQkVRVlJGSWpvS0lDQWdJR2xtSUhabGNuTnBiMjVmWTI5a1pTQnBjeUJPYjI1bE9nb2dJQ0FnSUNBZ0lIQnlhVzUwS0NKV1JWSlRTVTlPSUVWU1VrOVNPaUJWVUVSQlZFVWdiVzlrWlNCeVpYRjFhWEpsY3lCa1pYUmxZM1JoWW14bElHNTFiV1Z5YVdNZ2RtVnljMmx2YmtOdlpHVWlLUW9nSUNBZ0lDQWdJSE41Y3k1bGVHbDBLREVwQ2dvZ0lDQWdibVYzWDJOdlpHVWdQU0IyWlhKemFXOXVYMk52WkdVZ0t5QXhDaUFnSUNCMFpYaDBJRDBnY21VdWMzVmlLSFpqWDNCaGRDd2diR0Z0WW1SaElHMDZJRzB1WjNKdmRYQW9NU2tnS3lCemRISW9ibVYzWDJOdlpHVXBMQ0IwWlhoMExDQmpiM1Z1ZEQweEtRb2dJQ0FnY21WemRXeDBXeUp1WlhkZmRtVnljMmx2YmtOdlpHVWlYU0E5SUc1bGQxOWpiMlJsQ2dvZ0lDQWdhV1lnZG1WeWMybHZibDl1WVcxbE9nb2dJQ0FnSUNBZ0lIQmhjblJ6SUQwZ2RtVnljMmx2Ymw5dVlXMWxMbk53YkdsMEtDSXVJaWtLSUNBZ0lDQWdJQ0JwWmlCaGJHd29jQzVwYzJScFoybDBLQ2tnWm05eUlIQWdhVzRnY0dGeWRITXBJR0Z1WkNCc1pXNG9jR0Z5ZEhNcElENDlJREk2Q2lBZ0lDQWdJQ0FnSUNBZ0lHNTFiWE1nUFNCc2FYTjBLRzFoY0NocGJuUXNJSEJoY25SektTa0tJQ0FnSUNBZ0lDQWdJQ0FnYm5WdGMxc3RNVjBnS3owZ01Rb2dJQ0FnSUNBZ0lDQWdJQ0J1WlhkZmJtRnRaU0E5SUNJdUlpNXFiMmx1S0cxaGNDaHpkSElzSUc1MWJYTXBLUW9nSUNBZ0lDQWdJR1ZzYzJVNkNpQWdJQ0FnSUNBZ0lDQWdJRzVsZDE5dVlXMWxJRDBnZG1WeWMybHZibDl1WVcxbElDc2dJaTR4SWdvZ0lDQWdJQ0FnSUhSbGVIUWdQU0J5WlM1emRXSW9kbTVmY0dGMExDQnNZVzFpWkdFZ2JUb2diUzVuY205MWNDZ3hLU0FySUc1bGQxOXVZVzFsSUNzZ2JTNW5jbTkxY0NnektTd2dkR1Y0ZEN3Z1kyOTFiblE5TVNrS0lDQWdJQ0FnSUNCeVpYTjFiSFJiSW01bGQxOTJaWEp6YVc5dVRtRnRaU0pkSUQwZ2JtVjNYMjVoYldVS0lDQWdJR1ZzYzJVNkNpQWdJQ0FnSUNBZ2NtVnpkV3gwV3lKdVpYZGZkbVZ5YzJsdmJrNWhiV1VpWFNBOUlFNXZibVVLQ2lBZ0lDQnBaaUIwWlhoMElDRTlJRzl5YVdjNkNpQWdJQ0FnSUNBZ1luVnBiR1JmWm1sc1pTNTNjbWwwWlY5MFpYaDBLSFJsZUhRc0lHVnVZMjlrYVc1blBTSjFkR1l0T0NJcENpQWdJQ0FnSUNBZ2NtVnpkV3gwV3lKamFHRnVaMlZrSWwwZ1BTQlVjblZsQ21Wc2MyVTZDaUFnSUNCeVpYTjFiSFJiSW01bGQxOTJaWEp6YVc5dVEyOWtaU0pkSUQwZ2RtVnljMmx2Ymw5amIyUmxDaUFnSUNCeVpYTjFiSFJiSW01bGQxOTJaWEp6YVc5dVRtRnRaU0pkSUQwZ2RtVnljMmx2Ymw5dVlXMWxDZ3B2ZFhRdWQzSnBkR1ZmZEdWNGRDaHFjMjl1TG1SMWJYQnpLSEpsYzNWc2RDd2dhVzVrWlc1MFBUSXBMQ0JsYm1OdlpHbHVaejBpZFhSbUxUZ2lLUXB3Y21sdWRDZ2lWa1ZTVTBsUFRpSXBDbkJ5YVc1MEtDSTlQVDA5UFQwOUlpa0tjSEpwYm5Rb1ppSkNkV2xzWkNCbWFXeGxPaUI3WW5WcGJHUmZabWxzWlgwaUtRcHdjbWx1ZENobUlrMXZaR1U2SUh0dGIyUmxmU0lwQ25CeWFXNTBLR1lpVDJ4a0lIWmxjbk5wYjI1RGIyUmxPaUI3ZG1WeWMybHZibDlqYjJSbGZTSXBDbkJ5YVc1MEtHWWlUbVYzSUhabGNuTnBiMjVEYjJSbE9pQjdjbVZ6ZFd4MExtZGxkQ2duYm1WM1gzWmxjbk5wYjI1RGIyUmxKeWw5SWlrS2NISnBiblFvWmlKUGJHUWdkbVZ5YzJsdmJrNWhiV1U2SUh0MlpYSnphVzl1WDI1aGJXVjlJaWtLY0hKcGJuUW9aaUpPWlhjZ2RtVnljMmx2Yms1aGJXVTZJSHR5WlhOMWJIUXVaMlYwS0NkdVpYZGZkbVZ5YzJsdmJrNWhiV1VuS1gwaUtRcHdjbWx1ZENobUlrTm9ZVzVuWldRNklIdHlaWE4xYkhSYkoyTm9ZVzVuWldRblhYMGlLUW89JyB8IGJhc2U2NCAtLWRlY29kZSA+IC5yZXBvMnBsYXkvc2NyaXB0cy92ZXJzaW9uLnB5CiAgICAgICAgICBjaG1vZCAreCAucmVwbzJwbGF5L3NjcmlwdHMvKi5zaAoKICAgICAgLSBuYW1lOiBSdW4gUmVwbzJQbGF5IGVuZ2luZQogICAgICAgIHNoZWxsOiBiYXNoCiAgICAgICAgZW52OgogICAgICAgICAgVEFSR0VUX1JFUE9TSVRPUlk6ICR7eyBnaXRodWIucmVwb3NpdG9yeSB9fQogICAgICAgICAgVEFSR0VUX0JSQU5DSDogJHt7IGdpdGh1Yi5yZWZfbmFtZSB9fQogICAgICAgICAgQlVJTERfTU9ERTogJHt7IGlucHV0cy5idWlsZF9tb2RlIH19CiAgICAgICAgICBBUFBfS0VZU1RPUkVfQkFTRTY0OiAke3sgaW5wdXRzLmtleXN0b3JlX2Jhc2U2NCB9fQogICAgICAgIHJ1bjogfAogICAgICAgICAgc2V0IC1ldW8gcGlwZWZhaWwKICAgICAgICAgIC5yZXBvMnBsYXkvc2NyaXB0cy9ydW4uc2ggICAgICAgICAgICAgIiRHSVRIVUJfV09SS1NQQUNFIiAgICAgICAgICAgICAiJEdJVEhVQl9XT1JLU1BBQ0Uvb3V0cHV0IgoKICAgICAgLSBuYW1lOiBSZXF1aXJlIFBsYXkgU3RvcmUgZGVsaXZlcmFibGVzCiAgICAgICAgaWY6IHN1Y2Nlc3MoKQogICAgICAgIHNoZWxsOiBiYXNoCiAgICAgICAgcnVuOiB8CiAgICAgICAgICBzZXQgLWV1byBwaXBlZmFpbAogICAgICAgICAgdGVzdCAtcyBvdXRwdXQvYXBwLXJlbGVhc2Utc2lnbmVkLmFwawogICAgICAgICAgdGVzdCAtcyBvdXRwdXQvYXBwLXJlbGVhc2Utc2lnbmVkLmFhYgogICAgICAgICAgdGVzdCAtcyBvdXRwdXQvcmVwbzJwbGF5LXVwbG9hZC5qa3MKCiAgICAgIC0gbmFtZTogVXBsb2FkIFJlcG8yUGxheSByZWxlYXNlCiAgICAgICAgaWY6IGFsd2F5cygpCiAgICAgICAgdXNlczogYWN0aW9ucy91cGxvYWQtYXJ0aWZhY3RAdjQKICAgICAgICB3aXRoOgogICAgICAgICAgbmFtZTogUmVwbzJQbGF5LSR7eyBpbnB1dHMuYnVpbGRfbW9kZSB9fS1SZXN1bHQKICAgICAgICAgIHBhdGg6IG91dHB1dC8KICAgICAgICAgIGlmLW5vLWZpbGVzLWZvdW5kOiBlcnJvcgogICAgICAgICAgcmV0ZW50aW9uLWRheXM6IDcK"
                );

                getPreferences(MODE_PRIVATE)
                    .edit()
                    .putBoolean(
                        "engine_installed_"+
                        finalRepo+"_"+
                        finalBranch,
                        true
                    )
                    .apply();

                runOnUiThread(()->{
                    setStatus(
                        "✓ BUILD ENGINE INSTALLED\\n\\n"+
                        finalRepo+
                        "\\nBranch: "+finalBranch+
                        "\\n\\nCustomer-owned GitHub Actions ready."
                    );

                    Toast.makeText(
                        this,
                        "Build Engine installed",
                        Toast.LENGTH_LONG
                    ).show();
                });

            }catch(Exception e){

                final String msg=
                    e.getMessage()==null
                        ? e.getClass().getSimpleName()
                        : e.getMessage();

                runOnUiThread(()->setStatus(
                    "BUILD ENGINE INSTALL FAILED\\n\\n"+
                    msg
                ));
            }
        }).start();
    }


    private void putRepoFile(
        String tok,
        String repo,
        String br,
        String path,
        String contentBase64
    ) throws Exception {

        String encodedPath=path
            .replace(" ","%20");

        String api=
            "https://api.github.com/repos/"+
            repo+
            "/contents/"+
            encodedPath;

        String sha="";

        HttpURLConnection c=null;

        try{
            URL checkUrl=new URL(
                api+
                "?ref="+
                URLEncoder.encode(br,"UTF-8")
            );

            c=(HttpURLConnection)
                checkUrl.openConnection();

            c.setRequestMethod("GET");
            c.setRequestProperty(
                "Authorization",
                "Bearer "+tok
            );
            c.setRequestProperty(
                "Accept",
                "application/vnd.github+json"
            );
            c.setRequestProperty(
                "X-GitHub-Api-Version",
                "2022-11-28"
            );
            c.setConnectTimeout(20000);
            c.setReadTimeout(20000);

            int code=c.getResponseCode();

            if(code==200){

                BufferedReader r=
                    new BufferedReader(
                        new InputStreamReader(
                            c.getInputStream()
                        )
                    );

                StringBuilder out=
                    new StringBuilder();

                String line;

                while((line=r.readLine())!=null){
                    out.append(line);
                }

                r.close();

                JSONObject existing=
                    new JSONObject(
                        out.toString()
                    );

                sha=existing.optString(
                    "sha",
                    ""
                );
            }

        }finally{
            if(c!=null)c.disconnect();
        }

        URL putUrl=new URL(api);

        c=(HttpURLConnection)
            putUrl.openConnection();

        try{
            c.setRequestMethod("PUT");
            c.setDoOutput(true);

            c.setRequestProperty(
                "Authorization",
                "Bearer "+tok
            );

            c.setRequestProperty(
                "Accept",
                "application/vnd.github+json"
            );

            c.setRequestProperty(
                "X-GitHub-Api-Version",
                "2022-11-28"
            );

            c.setRequestProperty(
                "Content-Type",
                "application/json"
            );

            c.setConnectTimeout(20000);
            c.setReadTimeout(20000);

            JSONObject body=
                new JSONObject();

            body.put(
                "message",
                "Install Repo2Play engine: "+
                path
            );

            body.put(
                "content",
                contentBase64
            );

            body.put(
                "branch",
                br
            );

            if(!sha.isEmpty()){
                body.put(
                    "sha",
                    sha
                );
            }

            byte[] bytes=
                body.toString()
                .getBytes(
                    StandardCharsets.UTF_8
                );

            OutputStream os=
                c.getOutputStream();

            os.write(bytes);
            os.flush();
            os.close();

            int code=
                c.getResponseCode();

            if(code!=200 && code!=201){

                String message=
                    "GitHub HTTP "+
                    code+
                    " while installing "+
                    path;

                InputStream err=
                    c.getErrorStream();

                if(err!=null){

                    BufferedReader r=
                        new BufferedReader(
                            new InputStreamReader(
                                err
                            )
                        );

                    StringBuilder out=
                        new StringBuilder();

                    String line;

                    while((line=r.readLine())!=null){
                        out.append(line);
                    }

                    r.close();

                    try{
                        JSONObject j=
                            new JSONObject(
                                out.toString()
                            );

                        String gm=
                            j.optString(
                                "message",
                                ""
                            );

                        if(!gm.isEmpty()){
                            message+="\n"+gm;
                        }

                    }catch(Exception ignored){}
                }

                throw new Exception(
                    message
                );
            }

        }finally{
            if(c!=null)c.disconnect();
        }
    }


    private void dispatch(){
        hideKeyboard();
        String tok=secure.get("github_token");
        if(tok.isEmpty()) tok=token.getText().toString().trim();

        String repo=normalizeRepo(target.getText().toString());
        String br=branch.getText().toString().trim();

        if(tok.length()<10){ token.setError("Connect GitHub first"); return; }
        if(repo==null){ target.setError("Paste a GitHub repo link or owner/repository"); return; }
        if(br.isEmpty()){ branch.setError("Branch required"); return; }

        String keystore="";
        if("UPDATE".equals(mode)){
            keystore=secure.get(vaultName(repo));
            if(keystore.isEmpty()){
                setStatus("UPDATE BLOCKED\n\nNo original signing key is stored for this project.\nImport the JKS from the original NEW release first.");
                return;
            }
        }

        currentRepo=repo;
        getPreferences(MODE_PRIVATE).edit()
                .putString("last_repo",repo)
                .putString("last_branch",br)
                .apply();

        actionBtn.setEnabled(false);
        actionBtn.setText("STARTING…");
        setStatus("Preparing "+mode+" build…");

        final String finalTok=tok;
        final String finalKey=keystore;

        new Thread(()->{
            try{
                JSONObject body=new JSONObject();
                body.put("ref",br);
                JSONObject in=new JSONObject();
                in.put("build_mode",mode);
                in.put("keystore_base64",finalKey);
                body.put("inputs",in);

                // Important: same API version behavior used by the previously working V11 Android client.
                JSONObject res=post(
                        "https://api.github.com/repos/"+repo+"/actions/workflows/repo2play-build.yml/dispatches",
                        finalTok,
                        body.toString()
                );

                long id=res.optLong("workflow_run_id",0);
                if(id==0){
                    // Compatibility fallback: find the newest manually-dispatched matching workflow run.
                    id=findNewestWorkflowRun(finalTok,repo,br);
                }
                if(id==0) throw new Exception("GitHub started the workflow, but Repo2Play could not identify the run.");

                currentRunId=id;
                long runId=id;
                runOnUiThread(()->{
                    actionBtn.setText("BUILDING…");
                    setStatus("Run #"+runId+"\nQueued on GitHub Actions…");
                });
                poll(finalTok,id,repo);
            }catch(Exception e){
                showError(e);
                runOnUiThread(this::resetAction);
            }
        }).start();
    }

    private long findNewestWorkflowRun(String tok,String repo,String br)throws Exception{
        Thread.sleep(1500);
        JSONObject j=get("https://api.github.com/repos/"+repo+
                "/actions/workflows/repo2play-build.yml/runs?event=workflow_dispatch&branch="+URLEncoder.encode(br,"UTF-8")+"&per_page=5",tok);
        JSONArray a=j.optJSONArray("workflow_runs");
        if(a==null||a.length()==0)return 0;
        for(int i=0;i<a.length();i++){
            JSONObject r=a.getJSONObject(i);
            if("workflow_dispatch".equals(r.optString("event"))) return r.optLong("id",0);
        }
        return 0;
    }

    private void poll(String tok,long id,String repo)throws Exception{
        String url="https://api.github.com/repos/"+repo+"/actions/runs/"+id;
        while(true){
            JSONObject j=get(url,tok);
            String st=j.optString("status"), con=j.optString("conclusion");
            runOnUiThread(()->setStatus(
                    "Run #"+id+
                    "\nStatus: "+friendlyStatus(st)+
                    ("completed".equals(st) ? "\nResult: "+friendlyConclusion(con) : "\nGitHub Actions is building your release.")
            ));

            if("completed".equals(st)){
                if(!"success".equals(con)){
                    historyStore.add(repo,mode,"FAILED",id);
                    runOnUiThread(this::refreshHistory);
                    throw new Exception("Build failed on GitHub Actions. Run #"+id);
                }
                break;
            }
            Thread.sleep(5000);
        }

        JSONObject a=get("https://api.github.com/repos/"+repo+"/actions/runs/"+id+"/artifacts",tok);
        JSONArray arr=a.optJSONArray("artifacts");
        if(arr==null||arr.length()==0)
            throw new Exception("Build completed, but no release package was produced.");

        JSONObject artifact=arr.getJSONObject(0);
        currentArtifactName=artifact.optString("name","Repo2Play-Result");
        long aid=artifact.getLong("id");
        currentArtifactUrl="https://api.github.com/repos/"+repo+"/actions/artifacts/"+aid+"/zip";

        historyStore.add(repo,mode,"SUCCESS",id);
        runOnUiThread(()->{
            refreshHistory();
            setStatus("✓ RELEASE READY\n\n"+currentArtifactName+
                    "\n\nSigned APK\nPlay Store AAB\nSigning key backup\nBuild reports");
            actionBtn.setText("DOWNLOAD RELEASE");
            actionBtn.setEnabled(true);
            actionBtn.setOnClickListener(v->downloadResult());
        });
    }

    private void downloadResult(){
        String tok=secure.get("github_token");
        if(currentArtifactUrl.isEmpty()||tok.isEmpty())return;

        actionBtn.setEnabled(false);
        actionBtn.setText("DOWNLOADING…");
        setStatus("Downloading release package…");

        new Thread(()->{
            try{
                HttpURLConnection first=req(currentArtifactUrl,tok);
                first.setInstanceFollowRedirects(false);
                int code=first.getResponseCode();
                String loc=first.getHeaderField("Location");
                first.disconnect();
                if(code/100!=3||loc==null)
                    throw new Exception("GitHub artifact download could not start.");

                HttpURLConnection dl=(HttpURLConnection)new URL(loc).openConnection();
                dl.setConnectTimeout(20000);
                dl.setReadTimeout(60000);
                File tmp=new File(getCacheDir(),"release-"+System.currentTimeMillis()+".zip");

                try(InputStream in=dl.getInputStream();OutputStream out=new FileOutputStream(tmp)){
                    byte[] buf=new byte[8192];
                    int n;
                    while((n=in.read(buf))>0) out.write(buf,0,n);
                }
                dl.disconnect();

                if("NEW".equals(mode)){
                    String key=extractKeystoreBase64(tmp);
                    if(!key.isEmpty()) secure.put(vaultName(currentRepo),key);
                }

                saveToDownloads(tmp,currentArtifactName+".zip");

                runOnUiThread(()->{
                    vaultLabel.setText("Signing Vault • Original key secured for "+currentRepo);
                    setStatus("✓ DOWNLOAD COMPLETE\n\nDownloads/"+currentArtifactName+".zip\n\nKeep the ZIP backup safe for future updates.");
                    resetAction();
                });
            }catch(Exception e){
                showError(e);
                runOnUiThread(this::resetAction);
            }
        }).start();
    }

    private void pickKey(){
        String repo=normalizeRepo(target.getText().toString());
        if(repo==null){ target.setError("Enter the target repository first"); return; }
        currentRepo=repo;
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("*/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i,PICK_JKS);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==PICK_JKS&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri uri=data.getData();
            try(InputStream in=getContentResolver().openInputStream(uri);
                ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] b=new byte[8192];
                int n;
                while((n=in.read(b))>0) out.write(b,0,n);
                String enc=Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);
                secure.put(vaultName(currentRepo),enc);
                vaultLabel.setText("Signing Vault • Original key imported for "+currentRepo);
                setStatus("✓ Signing key imported securely");
            }catch(Exception e){ showError(e); }
        }
    }

    private String extractKeystoreBase64(File zip)throws Exception{
        try(ZipInputStream zin=new ZipInputStream(new FileInputStream(zip))){
            ZipEntry e;
            while((e=zin.getNextEntry())!=null){
                if(!e.isDirectory()&&e.getName().endsWith(".jks")){
                    ByteArrayOutputStream out=new ByteArrayOutputStream();
                    byte[] b=new byte[4096];
                    int n;
                    while((n=zin.read(b))>0) out.write(b,0,n);
                    return Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);
                }
            }
        }
        return "";
    }

    private void saveToDownloads(File src,String name)throws Exception{
        if(Build.VERSION.SDK_INT>=29){
            ContentValues cv=new ContentValues();
            cv.put(MediaStore.Downloads.DISPLAY_NAME,name);
            cv.put(MediaStore.Downloads.MIME_TYPE,"application/zip");
            cv.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS);
            Uri u=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cv);
            if(u==null) throw new IOException("Cannot create Downloads file.");
            try(InputStream in=new FileInputStream(src);OutputStream out=getContentResolver().openOutputStream(u)){
                byte[] b=new byte[8192];
                int n;
                while((n=in.read(b))>0) out.write(b,0,n);
            }
        } else {
            File dir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File dst=new File(dir,name);
            try(InputStream in=new FileInputStream(src);OutputStream out=new FileOutputStream(dst)){
                byte[] b=new byte[8192];
                int n;
                while((n=in.read(b))>0) out.write(b,0,n);
            }
        }
    }

    private String vaultName(String repo){
        return "jks_"+repo.toLowerCase(Locale.US).replace('/','_');
    }

    private void refreshHistory(){
        history.setText(historyStore.render());
    }

    private void resetAction(){
        actionBtn.setText("BUILD RELEASE");
        actionBtn.setEnabled(true);
        actionBtn.setOnClickListener(v->dispatch());
    }

    private void setMode(String m){
        mode=m;
        boolean isNew="NEW".equals(m);
        newBtn.setTextColor(isNew?BG:TXT);
        newBtn.setBackground(stroked(isNew?ACCENT:SURFACE2,isNew?ACCENT:SURFACE2,13,0));
        updateBtn.setTextColor(!isNew?BG:TXT);
        updateBtn.setBackground(stroked(!isNew?ACCENT:SURFACE2,!isNew?ACCENT:SURFACE2,13,0));

        String repo=normalizeRepo(target.getText().toString());
        if("UPDATE".equals(m) && repo!=null && !secure.get(vaultName(repo)).isEmpty())
            vaultLabel.setText("Signing Vault • Matching project key found.");
        else if("UPDATE".equals(m))
            vaultLabel.setText("Signing Vault • UPDATE requires the original project JKS.");
        else
            vaultLabel.setText("Signing Vault • NEW creates and secures a project signing key.");
    }

    private String friendlyStatus(String s){
        if("queued".equals(s))return "Queued";
        if("in_progress".equals(s))return "Building";
        if("completed".equals(s))return "Completed";
        return s;
    }

    private String friendlyConclusion(String s){
        return "success".equals(s) ? "Success" : s;
    }

    private void showError(Exception e){
        String m=e.getMessage()==null?"Unknown error":e.getMessage();

        if(m.contains("401"))
            m="GitHub rejected the token. Reconnect using a valid Personal Access Token.";
        else if(m.contains("403"))
            m="GitHub permission denied. The token needs permission to run and read Actions in Repo2Play.";
        else if(m.contains("404"))
            m="Repository or workflow not found, or the token cannot access it.";
        else if(m.contains("422"))
            m="GitHub rejected the build request. Check the repository, branch and workflow inputs.";

        final String msg=m;
        runOnUiThread(()->{
            status.setTextColor(BAD);
            setStatus("BUILD ERROR\n\n"+msg+"\n\nRun #"+(currentRunId==0?"—":currentRunId));
        });
    }

    private JSONObject get(String u,String tok)throws Exception{
        HttpURLConnection c=req(u,tok);
        return parse(c);
    }

    private JSONObject post(String u,String tok,String body)throws Exception{
        HttpURLConnection c=req(u,tok);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type","application/json");
        try(OutputStream o=c.getOutputStream()){
            o.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return parse(c);
    }

    private HttpURLConnection req(String u,String tok)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setRequestProperty("Accept","application/vnd.github+json");
        c.setRequestProperty("Authorization","Bearer "+tok);
        c.setRequestProperty("X-GitHub-Api-Version","2026-03-10");
        c.setRequestProperty("User-Agent","Repo2Play-Android/12");
        return c;
    }

    private JSONObject parse(HttpURLConnection c)throws Exception{
        int code=c.getResponseCode();
        InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();
        String x=read(in);
        c.disconnect();
        if(code<200||code>=300)
            throw new Exception("GitHub HTTP "+code+(x.isEmpty()?"":"\n"+x));
        return x.trim().isEmpty()?new JSONObject():new JSONObject(x);
    }

    private String read(InputStream in)throws Exception{
        if(in==null)return "";
        BufferedReader b=new BufferedReader(new InputStreamReader(in));
        StringBuilder s=new StringBuilder();
        String l;
        while((l=b.readLine())!=null) s.append(l);
        return s.toString();
    }

    private String normalizeRepo(String x){
        if(x==null)return null;
        x=x.trim()
                .replaceFirst("^https?://github\\.com/","")
                .replaceFirst("\\?.*$","")
                .replaceFirst("#.*$","")
                .replaceFirst("\\.git$","")
                .replaceAll("/+$","");
        return x.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")?x:null;
    }

    private void hideKeyboard(){
        try{
            ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(target.getWindowToken(),0);
        }catch(Exception ignored){}
    }

    private void setStatus(String x){
        status.setTextColor(TXT);
        status.setText(x);
    }

    // UI helpers
    private LinearLayout card(){
        LinearLayout l=col();
        l.setPadding(d(18),d(18),d(18),d(18));
        l.setBackground(stroked(SURFACE,Color.rgb(32,36,44),20,1));
        return l;
    }
    private LinearLayout col(){
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }
    private TextView section(String s){
        TextView v=t(s,12,ACCENT,true);
        v.setLetterSpacing(.10f);
        return v;
    }
    private TextView label(String s){
        TextView v=t(s,11,MUT,true);
        v.setPadding(0,0,0,d(7));
        return v;
    }
    private TextView t(String s,int sp,int c,boolean bold){
        TextView v=new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(c);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return v;
    }
    private EditText input(String h){
        EditText e=new EditText(this);
        e.setHint(h);
        e.setHintTextColor(Color.rgb(92,98,109));
        e.setTextColor(TXT);
        e.setTextSize(15);
        e.setSingleLine(true);
        e.setPadding(d(14),0,d(14),0);
        e.setBackground(stroked(SURFACE2,Color.rgb(38,43,53),14,1));
        return e;
    }
    private Button primary(String s){
        Button b=new Button(this);
        b.setText(s);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setTextColor(BG);
        b.setBackground(stroked(ACCENT,ACCENT,14,0));
        return b;
    }
    private Button secondary(String s){
        Button b=new Button(this);
        b.setText(s);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setTextColor(TXT);
        b.setBackground(stroked(SURFACE2,Color.rgb(42,47,57),14,1));
        return b;
    }
    private Button modeButton(String s,boolean on){
        return on?primary(s):secondary(s);
    }
    private LinearLayout.LayoutParams field(){
        return new LinearLayout.LayoutParams(-1,d(54));
    }
    private LinearLayout.LayoutParams buttonParams(){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,d(56));
        p.setMargins(0,d(14),0,0);
        return p;
    }
    private LinearLayout.LayoutParams smallButtonParams(){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,d(48));
        p.setMargins(0,d(10),0,0);
        return p;
    }
    private View space(int h){
        View v=new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1,d(h)));
        return v;
    }
    private GradientDrawable stroked(int fill,int stroke,int radius,int width){
        GradientDrawable g=new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(d(radius));
        if(width>0)g.setStroke(d(width),stroke);
        return g;
    }
    private int d(int x){
        return(int)(x*getResources().getDisplayMetrics().density+.5f);
    }
}
