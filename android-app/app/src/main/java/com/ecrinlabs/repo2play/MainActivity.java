
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
                body.put("ref",ENGINE_REF);
                JSONObject in=new JSONObject();
                in.put("repository",repo);
                in.put("branch",br);
                in.put("build_mode",mode);
                in.put("keystore_base64",finalKey);
                body.put("inputs",in);

                // Important: same API version behavior used by the previously working V11 Android client.
                JSONObject res=post(
                        "https://api.github.com/repos/"+ENGINE_REPO+"/actions/workflows/"+WORKFLOW+"/dispatches",
                        finalTok,
                        body.toString()
                );

                long id=res.optLong("workflow_run_id",0);
                if(id==0){
                    // Compatibility fallback: find the newest manually-dispatched matching workflow run.
                    id=findNewestWorkflowRun(finalTok);
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

    private long findNewestWorkflowRun(String tok)throws Exception{
        Thread.sleep(1500);
        JSONObject j=get("https://api.github.com/repos/"+ENGINE_REPO+
                "/actions/workflows/"+WORKFLOW+"/runs?event=workflow_dispatch&branch="+URLEncoder.encode(ENGINE_REF,"UTF-8")+"&per_page=5",tok);
        JSONArray a=j.optJSONArray("workflow_runs");
        if(a==null||a.length()==0)return 0;
        for(int i=0;i<a.length();i++){
            JSONObject r=a.getJSONObject(i);
            if("workflow_dispatch".equals(r.optString("event"))) return r.optLong("id",0);
        }
        return 0;
    }

    private void poll(String tok,long id,String repo)throws Exception{
        String url="https://api.github.com/repos/"+ENGINE_REPO+"/actions/runs/"+id;
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

        JSONObject a=get("https://api.github.com/repos/"+ENGINE_REPO+"/actions/runs/"+id+"/artifacts",tok);
        JSONArray arr=a.optJSONArray("artifacts");
        if(arr==null||arr.length()==0)
            throw new Exception("Build completed, but no release package was produced.");

        JSONObject artifact=arr.getJSONObject(0);
        currentArtifactName=artifact.optString("name","Repo2Play-Result");
        long aid=artifact.getLong("id");
        currentArtifactUrl="https://api.github.com/repos/"+ENGINE_REPO+"/actions/artifacts/"+aid+"/zip";

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
