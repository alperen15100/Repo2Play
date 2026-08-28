
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
            "Install the Repo2Play workflow into this repository before the first build.",
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

        TextView foot=t("Repo2Play v13.0 • by Ecrin Labs",11,MUT,false);
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
            "Installing full Repo2Play engine...\n\n"+
            finalRepo+
            "\nBranch: "+finalBranch
        );

        new Thread(()->{
            try{
                LinkedHashMap<String,String> files=
                    new LinkedHashMap<>();

                files.put(".repo2play/run.sh","IyEvdXNyL2Jpbi9lbnYgYmFzaApzZXQgLXVvIHBpcGVmYWlsCgpUQVJHRVQ9IiR7MTo/dGFyZ2V0IHBhdGggcmVxdWlyZWR9IgpPVVRQVVQ9IiR7Mjo/b3V0cHV0IHBhdGggcmVxdWlyZWR9IgpFTkdJTkU9IiQoY2QgIiQoZGlybmFtZSAiJHtCQVNIX1NPVVJDRVswXX0iKS8uLiIgJiYgcHdkKSIKCnJtIC1yZiAiJE9VVFBVVCIKbWtkaXIgLXAgIiRPVVRQVVQiCgpSRVBPUlQ9IiRPVVRQVVQvQlVJTEQtUkVQT1JULnR4dCIKewogIGVjaG8gIlJFUE8yUExBWSBWMTEgUFJPRFVDVElPTiBTVEFHRSAxIgogIGVjaG8gIj09PT09PT09PT09PT09PT09PT09PT09PT09PT0iCiAgZWNobyAiUmVwb3NpdG9yeTogJHtUQVJHRVRfUkVQT1NJVE9SWTotdW5rbm93bn0iCiAgZWNobyAiQnJhbmNoOiAke1RBUkdFVF9CUkFOQ0g6LXVua25vd259IgogIGVjaG8gIk1vZGU6ICR7QlVJTERfTU9ERTotTkVXfSIKICBlY2hvICJTdGFydGVkOiAkKGRhdGUgLXUgKyclWS0lbS0lZFQlSDolTTolU1onKSIKICBlY2hvCn0gPiAiJFJFUE9SVCIKCmZhaWxfcmVwb3J0KCkgewogIGxvY2FsIG1zZz0iJDEiCiAgewogICAgZWNobwogICAgZWNobyAiRklOQUwgUkVTVUxUOiBCTE9DS0VEIgogICAgZWNobyAiUmVhc29uOiAkbXNnIgogIH0gPj4gIiRSRVBPUlQiCiAgZWNobyAiJG1zZyIgPiAiJE9VVFBVVC9FUlJPUi50eHQiCiAgZXhpdCAxCn0KCkRFVEVDVF9PVVQ9IiRPVVRQVVQvZGV0ZWN0LmVudiIKIiRFTkdJTkUvc2NyaXB0cy9kZXRlY3QtcHJvamVjdC5zaCIgIiRUQVJHRVQiICIkREVURUNUX09VVCIgPj4gIiRSRVBPUlQiIDI+JjEgfHwgZmFpbF9yZXBvcnQgIkFuZHJvaWQgYXBwbGljYXRpb24gcHJvamVjdCBjb3VsZCBub3QgYmUgZGV0ZWN0ZWQuIgpzb3VyY2UgIiRERVRFQ1RfT1VUIgoKcHl0aG9uMyAiJEVOR0lORS9zY3JpcHRzL3ZlcnNpb24ucHkiICIkUFJPSkVDVF9ESVIiICIkQVBQX01PRFVMRSIgIiR7QlVJTERfTU9ERTotTkVXfSIgIiRPVVRQVVQvVkVSU0lPTi1JTkZPLmpzb24iID4+ICIkUkVQT1JUIiAyPiYxIHx8IGZhaWxfcmVwb3J0ICJWZXJzaW9uIHByZXBhcmF0aW9uIGZhaWxlZC4iCgpHUkFETEVfT1VUPSIkT1VUUFVUL2dyYWRsZS5lbnYiCiIkRU5HSU5FL3NjcmlwdHMvcmVzb2x2ZS1ncmFkbGUuc2giICIkUFJPSkVDVF9ESVIiICIkR1JBRExFX09VVCIgPj4gIiRSRVBPUlQiIDI+JjEgfHwgZmFpbF9yZXBvcnQgIkNvbXBhdGlibGUgR3JhZGxlIGNvdWxkIG5vdCBiZSBwcmVwYXJlZC4iCnNvdXJjZSAiJEdSQURMRV9PVVQiCgoiJEVOR0lORS9zY3JpcHRzL2J1aWxkLnNoIiAiJFBST0pFQ1RfRElSIiAiJEdSQURMRV9DTUQiICIkQVBQX01PRFVMRSIgIiRPVVRQVVQiID4+ICIkUkVQT1JUIiAyPiYxIHx8IGZhaWxfcmVwb3J0ICJBbmRyb2lkIHJlbGVhc2UgYnVpbGQgZmFpbGVkLiIKCiIkRU5HSU5FL3NjcmlwdHMvc2lnbi5zaCIgIiRPVVRQVVQiID4+ICIkUkVQT1JUIiAyPiYxIHx8IGZhaWxfcmVwb3J0ICJTaWduaW5nIGZhaWxlZC4iCgoiJEVOR0lORS9zY3JpcHRzL2RvY3Rvci5zaCIgIiRQUk9KRUNUX0RJUiIgIiRPVVRQVVQiID4+ICIkUkVQT1JUIiAyPiYxIHx8IHRydWUKIiRFTkdJTkUvc2NyaXB0cy9wYWNrYWdlLnNoIiAiJE9VVFBVVCIgPj4gIiRSRVBPUlQiIDI+JjEgfHwgZmFpbF9yZXBvcnQgIkZpbmFsIHBhY2thZ2UgcHJlcGFyYXRpb24gZmFpbGVkLiIKCnsKICBlY2hvCiAgZWNobyAiRklOQUwgUkVTVUxUOiBTVUNDRVNTIgogIGVjaG8gIkZpbmlzaGVkOiAkKGRhdGUgLXUgKyclWS0lbS0lZFQlSDolTTolU1onKSIKfSA+PiAiJFJFUE9SVCIKCmVjaG8gIlJlcG8yUGxheSBjb21wbGV0ZWQgc3VjY2Vzc2Z1bGx5LiIK");
                files.put(".repo2play/detect-project.sh","IyEvdXNyL2Jpbi9lbnYgYmFzaApzZXQgLWV1byBwaXBlZmFpbApUQVJHRVQ9IiR7MTo/fSIKT1VUPSIkezI6P30iCgpTRVRUSU5HUz0iJChmaW5kICIkVEFSR0VUIiAtdHlwZSBmIFwoIC1uYW1lIHNldHRpbmdzLmdyYWRsZSAtbyAtbmFtZSBzZXR0aW5ncy5ncmFkbGUua3RzIFwpICEgLXBhdGggJyovYnVpbGQvKicgfCBoZWFkIC0xIHx8IHRydWUpIgpbIC1uICIkU0VUVElOR1MiIF0gfHwgeyBlY2hvICJObyBzZXR0aW5ncy5ncmFkbGUvc2V0dGluZ3MuZ3JhZGxlLmt0cyI7IGV4aXQgMTsgfQpQUk9KRUNUX0RJUj0iJChkaXJuYW1lICIkU0VUVElOR1MiKSIKCkFQUF9CVUlMRD0iIgp3aGlsZSBJRlM9IHJlYWQgLXIgZjsgZG8KICBpZiBncmVwIC1FcSAnY29tXC5hbmRyb2lkXC5hcHBsaWNhdGlvbnxpZFtbOnNwYWNlOl1dKlwoP1tbOnNwYWNlOl1dKlsiJ1wnJ11jb21cLmFuZHJvaWRcLmFwcGxpY2F0aW9ufGFwcGx5IHBsdWdpbjpbWzpzcGFjZTpdXSpbIidcJyddY29tXC5hbmRyb2lkXC5hcHBsaWNhdGlvbicgIiRmIjsgdGhlbgogICAgQVBQX0JVSUxEPSIkZiI7IGJyZWFrCiAgZmkKZG9uZSA8IDwoZmluZCAiJFBST0pFQ1RfRElSIiAtdHlwZSBmIFwoIC1uYW1lIGJ1aWxkLmdyYWRsZSAtbyAtbmFtZSBidWlsZC5ncmFkbGUua3RzIFwpICEgLXBhdGggJyovYnVpbGQvKicpCgpbIC1uICIkQVBQX0JVSUxEIiBdIHx8IHsgZWNobyAiTm8gY29tLmFuZHJvaWQuYXBwbGljYXRpb24gbW9kdWxlIGZvdW5kIjsgZXhpdCAxOyB9Ck1PRFVMRV9ESVI9IiQoZGlybmFtZSAiJEFQUF9CVUlMRCIpIgpSRUw9IiR7TU9EVUxFX0RJUiMiJFBST0pFQ1RfRElSIi99IgppZiBbICIkTU9EVUxFX0RJUiIgPSAiJFBST0pFQ1RfRElSIiBdOyB0aGVuCiAgQVBQX01PRFVMRT0iIgplbHNlCiAgQVBQX01PRFVMRT0iJHtSRUwvL1wvLzp9IgpmaQoKewogIHByaW50ZiAnUFJPSkVDVF9ESVI9JXFcbicgIiRQUk9KRUNUX0RJUiIKICBwcmludGYgJ0FQUF9NT0RVTEU9JXFcbicgIiRBUFBfTU9EVUxFIgp9ID4gIiRPVVQiCgplY2hvICJQQVNTIEFuZHJvaWQgcHJvamVjdDogJFBST0pFQ1RfRElSIgplY2hvICJQQVNTIEFwcGxpY2F0aW9uIG1vZHVsZTogJHtBUFBfTU9EVUxFOi1yb290fSIK");
                files.put(".repo2play/resolve-gradle.sh","IyEvdXNyL2Jpbi9lbnYgYmFzaApzZXQgLWV1byBwaXBlZmFpbApQUk9KRUNUPSIkezE6P30iCk9VVD0iJHsyOj99IgoKY2QgIiRQUk9KRUNUIgoKZG93bmxvYWRfZ3JhZGxlKCkgewogIGxvY2FsIHZlcj0iJDEiCiAgbG9jYWwgZGlyPSIkUlVOTkVSX1RFTVAvcmVwbzJwbGF5LWdyYWRsZS0kdmVyIgogIGlmIFsgISAteCAiJGRpci9ncmFkbGUtJHZlci9iaW4vZ3JhZGxlIiBdOyB0aGVuCiAgICBybSAtcmYgIiRkaXIiOyBta2RpciAtcCAiJGRpciIKICAgIGN1cmwgLWZzU0wgLS1yZXRyeSAzICJodHRwczovL3NlcnZpY2VzLmdyYWRsZS5vcmcvZGlzdHJpYnV0aW9ucy9ncmFkbGUtJHt2ZXJ9LWJpbi56aXAiIC1vICIkZGlyL2dyYWRsZS56aXAiCiAgICB1bnppcCAtcSAiJGRpci9ncmFkbGUuemlwIiAtZCAiJGRpciIKICBmaQogIHByaW50ZiAnJXNcbicgIiRkaXIvZ3JhZGxlLSR2ZXIvYmluL2dyYWRsZSIKfQoKaWYgWyAtZiBncmFkbGV3IF07IHRoZW4KICBjaG1vZCAreCBncmFkbGV3CiAgQ01EPSIkUFJPSkVDVC9ncmFkbGV3IgogIGVjaG8gIlBBU1MgRXhpc3RpbmcgR3JhZGxlIHdyYXBwZXIiCmVsaWYgWyAtZiBncmFkbGUvd3JhcHBlci9ncmFkbGUtd3JhcHBlci5wcm9wZXJ0aWVzIF07IHRoZW4KICBVUkw9IiQoZ3JlcCAnXmRpc3RyaWJ1dGlvblVybD0nIGdyYWRsZS93cmFwcGVyL2dyYWRsZS13cmFwcGVyLnByb3BlcnRpZXMgfCBjdXQgLWQ9IC1mMi0gfCBzZWQgJ3MjXFw6IzojZycgfHwgdHJ1ZSkiCiAgVkVSPSIkKHByaW50ZiAnJXMnICIkVVJMIiB8IHNlZCAtbiAncy8uKmdyYWRsZS1cKFswLTldWzAtOS5dKlwpLS4qL1wxL3AnKSIKICBbIC1uICIkVkVSIiBdIHx8IGV4aXQgMQogIGVjaG8gIlJFQ09WRVJZIE1pc3NpbmcgZ3JhZGxldzsgdXNpbmcgR3JhZGxlICRWRVIiCiAgQ01EPSIkKGRvd25sb2FkX2dyYWRsZSAiJFZFUiIpIgplbHNlCiAgIyBHZW5lcmFsIGZhbGxiYWNrIGZvciBtb2Rlcm4gQW5kcm9pZCBwcm9qZWN0czsgYnVpbGQgZXJyb3JzIGFyZSBsYXRlciByZXBvcnRlZCBjbGVhcmx5LgogIFZFUj0iOC45IgogIGVjaG8gIlJFQ09WRVJZIE5vIHdyYXBwZXIgbWV0YWRhdGE7IHRyeWluZyBHcmFkbGUgJFZFUiIKICBDTUQ9IiQoZG93bmxvYWRfZ3JhZGxlICIkVkVSIikiCmZpCgoiJENNRCIgLS12ZXJzaW9uCnByaW50ZiAnR1JBRExFX0NNRD0lcVxuJyAiJENNRCIgPiAiJE9VVCIK");
                files.put(".repo2play/build.sh","IyEvdXNyL2Jpbi9lbnYgYmFzaApzZXQgLWV1byBwaXBlZmFpbApQUk9KRUNUPSIkezE6P30iCkdSQURMRT0iJHsyOj99IgpNT0RVTEU9IiR7My19IgpPVVRQVVQ9IiR7NDo/fSIKCmNkICIkUFJPSkVDVCIKUFJFRklYPSIiClsgLW4gIiRNT0RVTEUiIF0gJiYgUFJFRklYPSI6JHtNT0RVTEV9OiIKCmVjaG8gIkJ1aWxkaW5nIEFQSy4uLiIKIiRHUkFETEUiICIke1BSRUZJWH1hc3NlbWJsZVJlbGVhc2UiIC0tbm8tZGFlbW9uIC0tc3RhY2t0cmFjZQoKZWNobyAiQnVpbGRpbmcgQUFCLi4uIgoiJEdSQURMRSIgIiR7UFJFRklYfWJ1bmRsZVJlbGVhc2UiIC0tbm8tZGFlbW9uIC0tc3RhY2t0cmFjZQoKQVBLPSIkKGZpbmQgIiRQUk9KRUNUIiAtdHlwZSBmIC1uYW1lICcqLmFwaycgISAtcGF0aCAnKi9pbnRlcm1lZGlhdGVzLyonIHwgZ3JlcCAnL3JlbGVhc2UvJyB8IGhlYWQgLTEgfHwgdHJ1ZSkiCkFBQj0iJChmaW5kICIkUFJPSkVDVCIgLXR5cGUgZiAtbmFtZSAnKi5hYWInICEgLXBhdGggJyovaW50ZXJtZWRpYXRlcy8qJyB8IGdyZXAgJy9yZWxlYXNlLycgfCBoZWFkIC0xIHx8IHRydWUpIgoKWyAtbiAiJEFQSyIgXSB8fCB7IGVjaG8gIkFQSyBub3QgZm91bmQiOyBleGl0IDE7IH0KWyAtbiAiJEFBQiIgXSB8fCB7IGVjaG8gIkFBQiBub3QgZm91bmQiOyBleGl0IDE7IH0KCmNwICIkQVBLIiAiJE9VVFBVVC9hcHAtcmVsZWFzZS11bnNpZ25lZC5hcGsiCmNwICIkQUFCIiAiJE9VVFBVVC9hcHAtcmVsZWFzZS11bnNpZ25lZC5hYWIiCgplY2hvICJQQVNTIEFQSyBidWlsZCIKZWNobyAiUEFTUyBBQUIgYnVpbGQiCg==");
                files.put(".repo2play/sign.sh","IyEvdXNyL2Jpbi9lbnYgYmFzaApzZXQgLWV1byBwaXBlZmFpbAoKT1VUUFVUPSIkezE6P30iCk1PREU9IiR7QlVJTERfTU9ERTotTkVXfSIKS0VZU1RPUkU9IiRPVVRQVVQvcmVwbzJwbGF5LXVwbG9hZC5qa3MiCkFMSUFTPSIke1NJR05JTkdfS0VZX0FMSUFTOi1yZXBvMnBsYXl9IgpTVE9SRVBBU1M9IiR7U0lHTklOR19TVE9SRV9QQVNTV09SRDotUmVwbzJQbGF5MTIzIX0iCktFWVBBU1M9IiR7U0lHTklOR19LRVlfUEFTU1dPUkQ6LVJlcG8yUGxheTEyMyF9IgoKaWYgWyAiJE1PREUiID0gIlVQREFURSIgXTsgdGhlbgogIFsgLW4gIiR7QVBQX0tFWVNUT1JFX0JBU0U2NDotfSIgXSB8fCB7IGVjaG8gIlVQREFURSBtb2RlIHJlcXVpcmVzIEFQUF9LRVlTVE9SRV9CQVNFNjQiOyBleGl0IDE7IH0KICBwcmludGYgJyVzJyAiJEFQUF9LRVlTVE9SRV9CQVNFNjQiIHwgYmFzZTY0IC0tZGVjb2RlID4gIiRLRVlTVE9SRSIKICBbIC1zICIkS0VZU1RPUkUiIF0gfHwgeyBlY2hvICJEZWNvZGVkIGtleXN0b3JlIGlzIGVtcHR5IjsgZXhpdCAxOyB9CiAgZWNobyAiUEFTUyBFeGlzdGluZyBrZXlzdG9yZSBsb2FkZWQgZm9yIFVQREFURSIKZWxzZQogIGtleXRvb2wgLWdlbmtleXBhaXIgXAogICAgLWtleXN0b3JlICIkS0VZU1RPUkUiIFwKICAgIC1zdG9yZXBhc3MgIiRTVE9SRVBBU1MiIFwKICAgIC1rZXlwYXNzICIkS0VZUEFTUyIgXAogICAgLWFsaWFzICIkQUxJQVMiIFwKICAgIC1rZXlhbGcgUlNBIFwKICAgIC1rZXlzaXplIDIwNDggXAogICAgLXZhbGlkaXR5IDEwMDAwIFwKICAgIC1kbmFtZSAiQ049UmVwbzJQbGF5LCBPVT1BbmRyb2lkLCBPPVJlcG8yUGxheSwgTD1Vbmtub3duLCBTVD1Vbmtub3duLCBDPVVTIiBcCiAgICA+L2Rldi9udWxsIDI+JjEKICBlY2hvICJQQVNTIE5ldyBrZXlzdG9yZSBnZW5lcmF0ZWQiCmZpCgpCVUlMRF9UT09MUz0iJChmaW5kICIkQU5EUk9JRF9IT01FL2J1aWxkLXRvb2xzIiAtbWluZGVwdGggMSAtbWF4ZGVwdGggMSAtdHlwZSBkIHwgc29ydCAtViB8IHRhaWwgLTEpIgpbIC1uICIkQlVJTERfVE9PTFMiIF0gfHwgeyBlY2hvICJBbmRyb2lkIGJ1aWxkLXRvb2xzIG5vdCBmb3VuZCI7IGV4aXQgMTsgfQoKWklQQUxJR049IiRCVUlMRF9UT09MUy96aXBhbGlnbiIKQVBLU0lHTkVSPSIkQlVJTERfVE9PTFMvYXBrc2lnbmVyIgoKWyAteCAiJFpJUEFMSUdOIiBdIHx8IHsgZWNobyAiemlwYWxpZ24gbm90IGZvdW5kIjsgZXhpdCAxOyB9ClsgLXggIiRBUEtTSUdORVIiIF0gfHwgeyBlY2hvICJhcGtzaWduZXIgbm90IGZvdW5kIjsgZXhpdCAxOyB9CgoiJFpJUEFMSUdOIiAtZiA0ICIkT1VUUFVUL2FwcC1yZWxlYXNlLXVuc2lnbmVkLmFwayIgIiRPVVRQVVQvYXBwLXJlbGVhc2UtYWxpZ25lZC5hcGsiCgoiJEFQS1NJR05FUiIgc2lnbiBcCiAgLS1rcyAiJEtFWVNUT1JFIiBcCiAgLS1rcy1rZXktYWxpYXMgIiRBTElBUyIgXAogIC0ta3MtcGFzcyAicGFzczokU1RPUkVQQVNTIiBcCiAgLS1rZXktcGFzcyAicGFzczokS0VZUEFTUyIgXAogIC0tb3V0ICIkT1VUUFVUL2FwcC1yZWxlYXNlLXNpZ25lZC5hcGsiIFwKICAiJE9VVFBVVC9hcHAtcmVsZWFzZS1hbGlnbmVkLmFwayIKCiIkQVBLU0lHTkVSIiB2ZXJpZnkgLS12ZXJib3NlICIkT1VUUFVUL2FwcC1yZWxlYXNlLXNpZ25lZC5hcGsiID4vZGV2L251bGwKZWNobyAiUEFTUyBBUEsgc2lnbmF0dXJlIFZFUklGSUVEIgoKY3AgIiRPVVRQVVQvYXBwLXJlbGVhc2UtdW5zaWduZWQuYWFiIiAiJE9VVFBVVC9hcHAtcmVsZWFzZS1zaWduZWQuYWFiIgoKamFyc2lnbmVyIFwKICAtc2lnYWxnIFNIQTI1NndpdGhSU0EgXAogIC1kaWdlc3RhbGcgU0hBLTI1NiBcCiAgLWtleXN0b3JlICIkS0VZU1RPUkUiIFwKICAtc3RvcmVwYXNzICIkU1RPUkVQQVNTIiBcCiAgLWtleXBhc3MgIiRLRVlQQVNTIiBcCiAgIiRPVVRQVVQvYXBwLXJlbGVhc2Utc2lnbmVkLmFhYiIgXAogICIkQUxJQVMiIFwKICA+L2Rldi9udWxsCgpqYXJzaWduZXIgLXZlcmlmeSAiJE9VVFBVVC9hcHAtcmVsZWFzZS1zaWduZWQuYWFiIiA+L2Rldi9udWxsCmVjaG8gIlBBU1MgQUFCIHNpZ25hdHVyZSBWRVJJRklFRCIKCnsKICBlY2hvICJSRVBPMlBMQVkgU0lHTklORyBJTkZPIgogIGVjaG8gIj09PT09PT09PT09PT09PT09PT09PT0iCiAgZWNobyAiTW9kZTogJE1PREUiCiAgZWNobyAiQWxpYXM6ICRBTElBUyIKICBrZXl0b29sIC1saXN0IC12IC1rZXlzdG9yZSAiJEtFWVNUT1JFIiAtc3RvcmVwYXNzICIkU1RPUkVQQVNTIiAtYWxpYXMgIiRBTElBUyIgXAogICAgfCBncmVwIC1FICdTSEExOnxTSEEyNTY6JyB8fCB0cnVlCn0gPiAiJE9VVFBVVC9TSUdOSU5HLUlORk8udHh0IgoKaWYgWyAiJE1PREUiID0gIk5FVyIgXTsgdGhlbgogIHsKICAgIGVjaG8gIklNUE9SVEFOVCIKICAgIGVjaG8gIj09PT09PT09PSIKICAgIGVjaG8gIktlZXAgcmVwbzJwbGF5LXVwbG9hZC5qa3MsIGFsaWFzIGFuZCBwYXNzd29yZHMgc2FmZS4iCiAgICBlY2hvICJZb3UgbmVlZCB0aGUgc2FtZSBzaWduaW5nIGlkZW50aXR5IGZvciBmdXR1cmUgdXBkYXRlcyBvdXRzaWRlIFBsYXkgQXBwIFNpZ25pbmcgd29ya2Zsb3dzLiIKICB9ID4+ICIkT1VUUFVUL1NJR05JTkctSU5GTy50eHQiCmZpCg==");
                files.put(".repo2play/doctor.sh","IyEvdXNyL2Jpbi9lbnYgYmFzaApzZXQgLWV1byBwaXBlZmFpbApQUk9KRUNUPSIkezE6P30iCk9VVFBVVD0iJHsyOj99IgpSPSIkT1VUUFVUL1BMQVktUkVQT1JULnR4dCIKCnsKICBlY2hvICJSRVBPMlBMQVkgVjExIFBMQVkgU1RPUkUgRE9DVE9SIgogIGVjaG8gIj09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT0iCiAgZWNobwoKICBpZiBmaW5kICIkUFJPSkVDVCIgLXR5cGUgZiAtbmFtZSBBbmRyb2lkTWFuaWZlc3QueG1sICEgLXBhdGggJyovYnVpbGQvKicgfCBncmVwIC1xIC47IHRoZW4KICAgIGVjaG8gIlBBU1MgQW5kcm9pZE1hbmlmZXN0IGZvdW5kIgogIGVsc2UKICAgIGVjaG8gIldBUk5JTkcgQW5kcm9pZE1hbmlmZXN0IG5vdCBmb3VuZCIKICBmaQoKICBUQVJHRVQ9IiQoZ3JlcCAtUmhvRSAndGFyZ2V0U2RrKFZlcnNpb24pP1tbOnNwYWNlOl09KCldK1swLTldKycgIiRQUk9KRUNUIiAtLWluY2x1ZGU9JyouZ3JhZGxlJyAtLWluY2x1ZGU9JyouZ3JhZGxlLmt0cycgMj4vZGV2L251bGwgfCBncmVwIC1vRSAnWzAtOV0rJyB8IHNvcnQgLW5yIHwgaGVhZCAtMSB8fCB0cnVlKSIKICBDT01QSUxFPSIkKGdyZXAgLVJob0UgJ2NvbXBpbGVTZGsoVmVyc2lvbik/W1s6c3BhY2U6XT0oKV0rWzAtOV0rJyAiJFBST0pFQ1QiIC0taW5jbHVkZT0nKi5ncmFkbGUnIC0taW5jbHVkZT0nKi5ncmFkbGUua3RzJyAyPi9kZXYvbnVsbCB8IGdyZXAgLW9FICdbMC05XSsnIHwgc29ydCAtbnIgfCBoZWFkIC0xIHx8IHRydWUpIgoKICBlY2hvICJEZXRlY3RlZCB0YXJnZXRTZGs6ICR7VEFSR0VUOi11bmtub3dufSIKICBlY2hvICJEZXRlY3RlZCBjb21waWxlU2RrOiAke0NPTVBJTEU6LXVua25vd259IgoKICAjIEdvb2dsZSBQbGF5IG1vYmlsZSBwb2xpY3kgYmFzZWxpbmUgZm9yIG5ldyBhcHBzIGFuZCB1cGRhdGVzIGZyb20gMjAyNi0wOC0zMS4KICBpZiBbICIke1RBUkdFVDotMH0iIC1nZSAzNiBdIDI+L2Rldi9udWxsOyB0aGVuCiAgICBlY2hvICJQQVNTIHRhcmdldFNkayBtZWV0cyBBUEkgMzYgbW9iaWxlIHN1Ym1pc3Npb24gYmFzZWxpbmUiCiAgZWxzZQogICAgZWNobyAiV0FSTklORyB0YXJnZXRTZGsgZG9lcyBub3QgbWVldCBBUEkgMzYgbW9iaWxlIHN1Ym1pc3Npb24gYmFzZWxpbmUgZWZmZWN0aXZlIDIwMjYtMDgtMzEiCiAgZmkKCiAgaWYgWyAiJHtDT01QSUxFOi0wfSIgLWdlIDM2IF0gMj4vZGV2L251bGw7IHRoZW4KICAgIGVjaG8gIlBBU1MgY29tcGlsZVNkayA+PSAzNiIKICBlbHNlCiAgICBlY2hvICJXQVJOSU5HIGNvbXBpbGVTZGsgYmVsb3cgMzYgb3Igbm90IGRldGVjdGVkIgogIGZpCgogIGlmIGdyZXAgLVIgLXEgJ2FuZHJvaWQ6ZGVidWdnYWJsZT0idHJ1ZSInICIkUFJPSkVDVCIgLS1pbmNsdWRlPSdBbmRyb2lkTWFuaWZlc3QueG1sJyAyPi9kZXYvbnVsbDsgdGhlbgogICAgZWNobyAiV0FSTklORyBkZWJ1Z2dhYmxlPXRydWUgZGV0ZWN0ZWQiCiAgZWxzZQogICAgZWNobyAiUEFTUyBubyBleHBsaWNpdCBkZWJ1Z2dhYmxlPXRydWUiCiAgZmkKCiAgaWYgZ3JlcCAtUiAtcSAnYW5kcm9pZDp1c2VzQ2xlYXJ0ZXh0VHJhZmZpYz0idHJ1ZSInICIkUFJPSkVDVCIgLS1pbmNsdWRlPSdBbmRyb2lkTWFuaWZlc3QueG1sJyAyPi9kZXYvbnVsbDsgdGhlbgogICAgZWNobyAiV0FSTklORyBjbGVhcnRleHQgdHJhZmZpYyBleHBsaWNpdGx5IGVuYWJsZWQiCiAgZWxzZQogICAgZWNobyAiUEFTUyBubyBleHBsaWNpdCBjbGVhcnRleHQgdHJhZmZpYyBlbmFibGVtZW50IgogIGZpCgogIEVYUE9SVEVEPSIkKGdyZXAgLVJobyAnYW5kcm9pZDpleHBvcnRlZD0idHJ1ZSInICIkUFJPSkVDVCIgLS1pbmNsdWRlPSdBbmRyb2lkTWFuaWZlc3QueG1sJyAyPi9kZXYvbnVsbCB8IHdjIC1sIHwgdHIgLWQgJyAnKSIKICBlY2hvICJJTkZPIGV4cG9ydGVkPXRydWUgY29tcG9uZW50czogJHtFWFBPUlRFRDotMH0iCgogIGlmIFsgLWYgIiRPVVRQVVQvYXBwLXJlbGVhc2Utc2lnbmVkLmFwayIgXTsgdGhlbgogICAgZWNobyAiUEFTUyBzaWduZWQgQVBLIHByZXNlbnQiCiAgZWxzZQogICAgZWNobyAiQkxPQ0tFUiBzaWduZWQgQVBLIG1pc3NpbmciCiAgZmkKCiAgaWYgWyAtZiAiJE9VVFBVVC9hcHAtcmVsZWFzZS1zaWduZWQuYWFiIiBdOyB0aGVuCiAgICBlY2hvICJQQVNTIHNpZ25lZCBBQUIgcHJlc2VudCIKICBlbHNlCiAgICBlY2hvICJCTE9DS0VSIHNpZ25lZCBBQUIgbWlzc2luZyIKICBmaQoKICBlY2hvCiAgZWNobyAiUE9MSUNZIE5PVEUiCiAgZWNobyAiU3RhcnRpbmcgMjAyNi0wOC0zMSwgbmV3IG1vYmlsZSBhcHBzIGFuZCBhcHAgdXBkYXRlcyBzdWJtaXR0ZWQgdG8gR29vZ2xlIFBsYXkgbXVzdCB0YXJnZXQgQW5kcm9pZCAxNiAvIEFQSSAzNiBvciBoaWdoZXIuIgogIGVjaG8gIlRoaXMgaXMgYSB0ZWNobmljYWwgcHJlZmxpZ2h0LCBub3QgYSBndWFyYW50ZWUgb2YgR29vZ2xlIFBsYXkgYXBwcm92YWwuIgp9ID4gIiRSIgoKY2F0ICIkUiIK");
                files.put(".repo2play/package.sh","IyEvdXNyL2Jpbi9lbnYgYmFzaApzZXQgLWV1byBwaXBlZmFpbApPVVRQVVQ9IiR7MTo/fSIKClsgLWYgIiRPVVRQVVQvYXBwLXJlbGVhc2Utc2lnbmVkLmFwayIgXSB8fCB7IGVjaG8gIlNpZ25lZCBBUEsgbWlzc2luZyI7IGV4aXQgMTsgfQpbIC1mICIkT1VUUFVUL2FwcC1yZWxlYXNlLXNpZ25lZC5hYWIiIF0gfHwgeyBlY2hvICJTaWduZWQgQUFCIG1pc3NpbmciOyBleGl0IDE7IH0KWyAtZiAiJE9VVFBVVC9TSUdOSU5HLUlORk8udHh0IiBdIHx8IHsgZWNobyAiU2lnbmluZyByZXBvcnQgbWlzc2luZyI7IGV4aXQgMTsgfQoKcm0gLWYgIiRPVVRQVVQvYXBwLXJlbGVhc2UtdW5zaWduZWQuYXBrIiAiJE9VVFBVVC9hcHAtcmVsZWFzZS1hbGlnbmVkLmFwayIgIiRPVVRQVVQvYXBwLXJlbGVhc2UtdW5zaWduZWQuYWFiIiAiJE9VVFBVVC9kZXRlY3QuZW52IiAiJE9VVFBVVC9ncmFkbGUuZW52IgoKKAogIGNkICIkT1VUUFVUIgogIHNoYTI1NnN1bSBhcHAtcmVsZWFzZS1zaWduZWQuYXBrIGFwcC1yZWxlYXNlLXNpZ25lZC5hYWIgPiBTSEEyNTZTVU1TLnR4dAopCgplY2hvICJQQVNTIEZpbmFsIHNpZ25lZCBBUEsvQUFCIHBhY2thZ2UgcHJlcGFyZWQiCg==");
                files.put(".repo2play/version.py","IyEvdXNyL2Jpbi9lbnYgcHl0aG9uMwppbXBvcnQgcmUsIHN5cywganNvbgpmcm9tIHBhdGhsaWIgaW1wb3J0IFBhdGgKCnByb2plY3QgPSBQYXRoKHN5cy5hcmd2WzFdKS5yZXNvbHZlKCkKbW9kdWxlID0gc3lzLmFyZ3ZbMl0gaWYgbGVuKHN5cy5hcmd2KSA+IDIgZWxzZSAiIgptb2RlID0gKHN5cy5hcmd2WzNdIGlmIGxlbihzeXMuYXJndikgPiAzIGVsc2UgIk5FVyIpLnVwcGVyKCkKb3V0ID0gUGF0aChzeXMuYXJndls0XSkucmVzb2x2ZSgpCgptb2R1bGVfZGlyID0gcHJvamVjdCBpZiBub3QgbW9kdWxlIGVsc2UgcHJvamVjdC5qb2lucGF0aCgqbW9kdWxlLnNwbGl0KCI6IikpCmNhbmRpZGF0ZXMgPSBbbW9kdWxlX2RpciAvICJidWlsZC5ncmFkbGUua3RzIiwgbW9kdWxlX2RpciAvICJidWlsZC5ncmFkbGUiXQpidWlsZF9maWxlID0gbmV4dCgocCBmb3IgcCBpbiBjYW5kaWRhdGVzIGlmIHAuZXhpc3RzKCkpLCBOb25lKQppZiBub3QgYnVpbGRfZmlsZToKICAgIHByaW50KCJWRVJTSU9OIEVSUk9SOiBhcHAgYnVpbGQuZ3JhZGxlKC5rdHMpIG5vdCBmb3VuZCIpCiAgICBzeXMuZXhpdCgxKQoKdGV4dCA9IGJ1aWxkX2ZpbGUucmVhZF90ZXh0KGVuY29kaW5nPSJ1dGYtOCIpCm9yaWcgPSB0ZXh0Cgp2Y19wYXR0ZXJucyA9IFsKICAgIHInKFxidmVyc2lvbkNvZGVccyo9XHMqKShcZCspJywKICAgIHInKFxidmVyc2lvbkNvZGVccyspKFxkKyknLApdCnZuX3BhdHRlcm5zID0gWwogICAgcicoXGJ2ZXJzaW9uTmFtZVxzKj1ccypbIlwnXSkoW14iXCddKykoWyJcJ10pJywKICAgIHInKFxidmVyc2lvbk5hbWVccytbIlwnXSkoW14iXCddKykoWyJcJ10pJywKXQoKdmVyc2lvbl9jb2RlID0gTm9uZQpmb3IgcGF0IGluIHZjX3BhdHRlcm5zOgogICAgbSA9IHJlLnNlYXJjaChwYXQsIHRleHQpCiAgICBpZiBtOgogICAgICAgIHZlcnNpb25fY29kZSA9IGludChtLmdyb3VwKDIpKQogICAgICAgIHZjX3BhdCA9IHBhdAogICAgICAgIGJyZWFrCgp2ZXJzaW9uX25hbWUgPSBOb25lCmZvciBwYXQgaW4gdm5fcGF0dGVybnM6CiAgICBtID0gcmUuc2VhcmNoKHBhdCwgdGV4dCkKICAgIGlmIG06CiAgICAgICAgdmVyc2lvbl9uYW1lID0gbS5ncm91cCgyKQogICAgICAgIHZuX3BhdCA9IHBhdAogICAgICAgIGJyZWFrCgpyZXN1bHQgPSB7CiAgICAiZmlsZSI6IHN0cihidWlsZF9maWxlKSwKICAgICJtb2RlIjogbW9kZSwKICAgICJvbGRfdmVyc2lvbkNvZGUiOiB2ZXJzaW9uX2NvZGUsCiAgICAib2xkX3ZlcnNpb25OYW1lIjogdmVyc2lvbl9uYW1lLAogICAgImNoYW5nZWQiOiBGYWxzZSwKfQoKaWYgbW9kZSA9PSAiVVBEQVRFIjoKICAgIGlmIHZlcnNpb25fY29kZSBpcyBOb25lOgogICAgICAgIHByaW50KCJWRVJTSU9OIEVSUk9SOiBVUERBVEUgbW9kZSByZXF1aXJlcyBkZXRlY3RhYmxlIG51bWVyaWMgdmVyc2lvbkNvZGUiKQogICAgICAgIHN5cy5leGl0KDEpCgogICAgbmV3X2NvZGUgPSB2ZXJzaW9uX2NvZGUgKyAxCiAgICB0ZXh0ID0gcmUuc3ViKHZjX3BhdCwgbGFtYmRhIG06IG0uZ3JvdXAoMSkgKyBzdHIobmV3X2NvZGUpLCB0ZXh0LCBjb3VudD0xKQogICAgcmVzdWx0WyJuZXdfdmVyc2lvbkNvZGUiXSA9IG5ld19jb2RlCgogICAgaWYgdmVyc2lvbl9uYW1lOgogICAgICAgIHBhcnRzID0gdmVyc2lvbl9uYW1lLnNwbGl0KCIuIikKICAgICAgICBpZiBhbGwocC5pc2RpZ2l0KCkgZm9yIHAgaW4gcGFydHMpIGFuZCBsZW4ocGFydHMpID49IDI6CiAgICAgICAgICAgIG51bXMgPSBsaXN0KG1hcChpbnQsIHBhcnRzKSkKICAgICAgICAgICAgbnVtc1stMV0gKz0gMQogICAgICAgICAgICBuZXdfbmFtZSA9ICIuIi5qb2luKG1hcChzdHIsIG51bXMpKQogICAgICAgIGVsc2U6CiAgICAgICAgICAgIG5ld19uYW1lID0gdmVyc2lvbl9uYW1lICsgIi4xIgogICAgICAgIHRleHQgPSByZS5zdWIodm5fcGF0LCBsYW1iZGEgbTogbS5ncm91cCgxKSArIG5ld19uYW1lICsgbS5ncm91cCgzKSwgdGV4dCwgY291bnQ9MSkKICAgICAgICByZXN1bHRbIm5ld192ZXJzaW9uTmFtZSJdID0gbmV3X25hbWUKICAgIGVsc2U6CiAgICAgICAgcmVzdWx0WyJuZXdfdmVyc2lvbk5hbWUiXSA9IE5vbmUKCiAgICBpZiB0ZXh0ICE9IG9yaWc6CiAgICAgICAgYnVpbGRfZmlsZS53cml0ZV90ZXh0KHRleHQsIGVuY29kaW5nPSJ1dGYtOCIpCiAgICAgICAgcmVzdWx0WyJjaGFuZ2VkIl0gPSBUcnVlCmVsc2U6CiAgICByZXN1bHRbIm5ld192ZXJzaW9uQ29kZSJdID0gdmVyc2lvbl9jb2RlCiAgICByZXN1bHRbIm5ld192ZXJzaW9uTmFtZSJdID0gdmVyc2lvbl9uYW1lCgpvdXQud3JpdGVfdGV4dChqc29uLmR1bXBzKHJlc3VsdCwgaW5kZW50PTIpLCBlbmNvZGluZz0idXRmLTgiKQpwcmludCgiVkVSU0lPTiIpCnByaW50KCI9PT09PT09IikKcHJpbnQoZiJCdWlsZCBmaWxlOiB7YnVpbGRfZmlsZX0iKQpwcmludChmIk1vZGU6IHttb2RlfSIpCnByaW50KGYiT2xkIHZlcnNpb25Db2RlOiB7dmVyc2lvbl9jb2RlfSIpCnByaW50KGYiTmV3IHZlcnNpb25Db2RlOiB7cmVzdWx0LmdldCgnbmV3X3ZlcnNpb25Db2RlJyl9IikKcHJpbnQoZiJPbGQgdmVyc2lvbk5hbWU6IHt2ZXJzaW9uX25hbWV9IikKcHJpbnQoZiJOZXcgdmVyc2lvbk5hbWU6IHtyZXN1bHQuZ2V0KCduZXdfdmVyc2lvbk5hbWUnKX0iKQpwcmludChmIkNoYW5nZWQ6IHtyZXN1bHRbJ2NoYW5nZWQnXX0iKQo=");
                files.put(".github/workflows/repo2play-build.yml","bmFtZTogUmVwbzJQbGF5IEJ1aWxkCgpvbjoKICB3b3JrZmxvd19kaXNwYXRjaDoKICAgIGlucHV0czoKICAgICAgYnVpbGRfbW9kZToKICAgICAgICBkZXNjcmlwdGlvbjogIk5FVyBvciBVUERBVEUiCiAgICAgICAgcmVxdWlyZWQ6IHRydWUKICAgICAgICBkZWZhdWx0OiAiTkVXIgogICAgICAgIHR5cGU6IGNob2ljZQogICAgICAgIG9wdGlvbnM6CiAgICAgICAgICAtIE5FVwogICAgICAgICAgLSBVUERBVEUKICAgICAga2V5c3RvcmVfYmFzZTY0OgogICAgICAgIGRlc2NyaXB0aW9uOiAiRXhpc3RpbmcgUmVwbzJQbGF5IEpLUyBmb3IgVVBEQVRFIgogICAgICAgIHJlcXVpcmVkOiBmYWxzZQogICAgICAgIGRlZmF1bHQ6ICIiCiAgICAgICAgdHlwZTogc3RyaW5nCgpwZXJtaXNzaW9uczoKICBjb250ZW50czogcmVhZAoKam9iczoKICBidWlsZDoKICAgIG5hbWU6IEFuYWx5emUgQnVpbGQgU2lnbiBQYWNrYWdlCiAgICBydW5zLW9uOiB1YnVudHUtbGF0ZXN0CiAgICB0aW1lb3V0LW1pbnV0ZXM6IDQ1CgogICAgc3RlcHM6CiAgICAgIC0gbmFtZTogQ2hlY2tvdXQgcHJvamVjdAogICAgICAgIHVzZXM6IGFjdGlvbnMvY2hlY2tvdXRAdjQKCiAgICAgIC0gbmFtZTogU2V0dXAgSmF2YSAxNwogICAgICAgIHVzZXM6IGFjdGlvbnMvc2V0dXAtamF2YUB2NQogICAgICAgIHdpdGg6CiAgICAgICAgICBkaXN0cmlidXRpb246IHRlbXVyaW4KICAgICAgICAgIGphdmEtdmVyc2lvbjogIjE3IgoKICAgICAgLSBuYW1lOiBSdW4gUmVwbzJQbGF5IGVuZ2luZQogICAgICAgIHNoZWxsOiBiYXNoCiAgICAgICAgZW52OgogICAgICAgICAgVEFSR0VUX1JFUE9TSVRPUlk6ICR7eyBnaXRodWIucmVwb3NpdG9yeSB9fQogICAgICAgICAgVEFSR0VUX0JSQU5DSDogJHt7IGdpdGh1Yi5yZWZfbmFtZSB9fQogICAgICAgICAgQlVJTERfTU9ERTogJHt7IGlucHV0cy5idWlsZF9tb2RlIH19CiAgICAgICAgICBBUFBfS0VZU1RPUkVfQkFTRTY0OiAke3sgaW5wdXRzLmtleXN0b3JlX2Jhc2U2NCB9fQogICAgICAgIHJ1bjogfAogICAgICAgICAgc2V0IC1ldW8gcGlwZWZhaWwKICAgICAgICAgIGNobW9kICt4IC5yZXBvMnBsYXkvKi5zaAogICAgICAgICAgLnJlcG8ycGxheS9ydW4uc2ggIiRHSVRIVUJfV09SS1NQQUNFIiAiJEdJVEhVQl9XT1JLU1BBQ0Uvb3V0cHV0IgoKICAgICAgLSBuYW1lOiBSZXF1aXJlIHJlbGVhc2UgZmlsZXMKICAgICAgICBpZjogc3VjY2VzcygpCiAgICAgICAgc2hlbGw6IGJhc2gKICAgICAgICBydW46IHwKICAgICAgICAgIHNldCAtZXVvIHBpcGVmYWlsCiAgICAgICAgICB0ZXN0IC1zIG91dHB1dC9hcHAtcmVsZWFzZS1zaWduZWQuYXBrCiAgICAgICAgICB0ZXN0IC1zIG91dHB1dC9hcHAtcmVsZWFzZS1zaWduZWQuYWFiCiAgICAgICAgICB0ZXN0IC1zIG91dHB1dC9yZXBvMnBsYXktdXBsb2FkLmprcwoKICAgICAgLSBuYW1lOiBVcGxvYWQgUmVwbzJQbGF5IHJlbGVhc2UKICAgICAgICBpZjogYWx3YXlzKCkKICAgICAgICB1c2VzOiBhY3Rpb25zL3VwbG9hZC1hcnRpZmFjdEB2NAogICAgICAgIHdpdGg6CiAgICAgICAgICBuYW1lOiBSZXBvMlBsYXktJHt7IGlucHV0cy5idWlsZF9tb2RlIH19LVJlc3VsdAogICAgICAgICAgcGF0aDogb3V0cHV0LwogICAgICAgICAgaWYtbm8tZmlsZXMtZm91bmQ6IGVycm9yCiAgICAgICAgICByZXRlbnRpb24tZGF5czogNwo=");

                int total=files.size();
                int done=0;

                for(Map.Entry<String,String> entry:
                        files.entrySet()){

                    putRepoFile(
                        finalTok,
                        finalRepo,
                        finalBranch,
                        entry.getKey(),
                        entry.getValue()
                    );

                    done++;

                    final int progress=done;
                    final String current=entry.getKey();

                    runOnUiThread(()->setStatus(
                        "Installing Repo2Play engine...\n\n"+
                        progress+" / "+total+
                        "\n"+current
                    ));
                }

                getPreferences(MODE_PRIVATE)
                    .edit()
                    .putBoolean(
                        "engine_installed_"+finalRepo+
                        "_"+finalBranch,
                        true
                    )
                    .apply();

                runOnUiThread(()->{
                    setStatus(
                        "✓ FULL BUILD ENGINE INSTALLED\n\n"+
                        finalRepo+
                        "\nBranch: "+finalBranch+
                        "\n\nWorkflow + verified build/sign "+
                        "engine installed."
                    );

                    Toast.makeText(
                        this,
                        "Full Build Engine installed",
                        Toast.LENGTH_LONG
                    ).show();
                });

            }catch(Exception e){

                final String msg=
                    e.getMessage()==null
                    ? e.getClass().getSimpleName()
                    : e.getMessage();

                runOnUiThread(()->setStatus(
                    "BUILD ENGINE INSTALL FAILED\n\n"+
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
