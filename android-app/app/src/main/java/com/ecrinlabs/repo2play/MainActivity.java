package com.ecrinlabs.repo2play;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.text.InputType;
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

    private final int BG=Color.rgb(12,14,18), CARD=Color.rgb(20,23,29), CARD2=Color.rgb(27,31,39);
    private final int TXT=Color.rgb(242,239,232), MUT=Color.rgb(146,152,163), GOLD=Color.rgb(199,169,107);
    private EditText token,target,branch;
    private Button newBtn,updateBtn,actionBtn,connectBtn,importBtn;
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
        ScrollView sv=new ScrollView(this);sv.setBackgroundColor(BG);
        LinearLayout page=col();page.setPadding(d(22),d(26),d(22),d(40));sv.addView(page);

        LinearLayout hero=new LinearLayout(this);hero.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left=col();
        left.addView(t("REPO2PLAY",31,TXT,true));
        TextView by=t("by Ecrin Labs",12,GOLD,true);by.setPadding(0,d(3),0,0);left.addView(by);
        TextView sub=t("GitHub → Android → Play Store",14,MUT,false);sub.setPadding(0,d(8),0,0);left.addView(sub);
        hero.addView(left,new LinearLayout.LayoutParams(0,-2,1));
        TextView mark=t("R",23,GOLD,true);mark.setGravity(Gravity.CENTER);mark.setBackground(round(CARD,18));
        hero.addView(mark,new LinearLayout.LayoutParams(d(58),d(58)));
        page.addView(hero);

        TextView intro=t("Build signed Android releases without leaving your phone.",15,TXT,false);
        intro.setPadding(0,d(22),0,d(16));page.addView(intro);

        LinearLayout auth=card();
        page.addView(auth);
        auth.addView(section("GITHUB CONNECTION"));
        accountLabel=t("Not connected",13,MUT,false);auth.addView(accountLabel);
        token=input("Personal access token");token.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        token.setText(secure.get("github_token"));
        LinearLayout.LayoutParams tp=field();tp.setMargins(0,d(12),0,0);auth.addView(token,tp);
        connectBtn=secondary("CONNECT GITHUB");auth.addView(connectBtn,buttonParams());
        connectBtn.setOnClickListener(v->connect());

        LinearLayout build=card();LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,d(14),0,0);page.addView(build,cp);
        build.addView(section("ANDROID RELEASE"));
        build.addView(label("TARGET REPOSITORY"));
        target=input("owner/repository or GitHub URL");target.setText(getPreferences(MODE_PRIVATE).getString("last_repo",""));build.addView(target,field());
        build.addView(space(12));
        build.addView(label("BRANCH"));
        branch=input("main");branch.setText(getPreferences(MODE_PRIVATE).getString("last_branch","main"));build.addView(branch,field());

        build.addView(space(18));build.addView(label("RELEASE TYPE"));
        LinearLayout modes=new LinearLayout(this);modes.setOrientation(LinearLayout.HORIZONTAL);
        newBtn=modeButton("NEW",true);updateBtn=modeButton("UPDATE",false);
        modes.addView(newBtn,new LinearLayout.LayoutParams(0,d(50),1));
        LinearLayout.LayoutParams up=new LinearLayout.LayoutParams(0,d(50),1);up.setMargins(d(10),0,0,0);modes.addView(updateBtn,up);build.addView(modes);
        newBtn.setOnClickListener(v->setMode("NEW"));updateBtn.setOnClickListener(v->setMode("UPDATE"));

        vaultLabel=t("Signing vault: NEW builds create and save a project key automatically.",12,MUT,false);
        vaultLabel.setPadding(0,d(14),0,0);build.addView(vaultLabel);
        importBtn=secondary("IMPORT SIGNING KEY");build.addView(importBtn,smallButtonParams());
        importBtn.setOnClickListener(v->pickKey());

        actionBtn=primary("BUILD RELEASE");build.addView(actionBtn,buttonParams());
        actionBtn.setOnClickListener(v->dispatch());

        LinearLayout st=card();LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.setMargins(0,d(14),0,0);page.addView(st,sp);
        st.addView(section("BUILD STATUS"));
        status=t("Ready",14,TXT,false);status.setPadding(0,d(8),0,0);status.setTextIsSelectable(true);st.addView(status);

        LinearLayout hist=card();LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.setMargins(0,d(14),0,0);page.addView(hist,hp);
        hist.addView(section("BUILD HISTORY"));
        history=t("",13,TXT,false);history.setPadding(0,d(8),0,0);history.setLineSpacing(d(3),1f);hist.addView(history);

        TextView privacy=t("Privacy • Token and signing vault stay encrypted on this device. Build requests go directly to GitHub.",11,MUT,false);
        privacy.setPadding(0,d(18),0,0);privacy.setGravity(Gravity.CENTER);page.addView(privacy);

        setContentView(sv);
    }

    private void connect(){
        String tok=token.getText().toString().trim();
        if(tok.length()<10){token.setError("GitHub token required");return;}
        setStatus("Connecting securely to GitHub…");
        new Thread(()->{
            try{
                JSONObject me=get("https://api.github.com/user",tok);
                secure.put("github_token",tok);
                String login=me.optString("login","GitHub user");
                runOnUiThread(()->{
                    accountLabel.setText("Connected as "+login);
                    setStatus("✓ GitHub connected");
                });
            }catch(Exception e){showError(e);}
        }).start();
    }

    private void dispatch(){
        hideKeyboard();
        String tok=secure.get("github_token");
        if(tok.isEmpty()) tok=token.getText().toString().trim();
        String repo=normalizeRepo(target.getText().toString());
        String br=branch.getText().toString().trim();
        if(tok.length()<10){token.setError("Connect GitHub first");return;}
        if(repo==null){target.setError("Use owner/repository or a GitHub URL");return;}
        if(br.isEmpty()){branch.setError("Branch required");return;}

        String keystore="";
        if("UPDATE".equals(mode)){
            keystore=secure.get(vaultName(repo));
            if(keystore.isEmpty()){
                setStatus("UPDATE BLOCKED\n\nNo signing key is saved for this repository.\nImport the JKS from the original NEW build first.");
                return;
            }
        }

        currentRepo=repo;
        getPreferences(MODE_PRIVATE).edit().putString("last_repo",repo).putString("last_branch",br).apply();
        actionBtn.setEnabled(false);actionBtn.setText("STARTING…");
        setStatus("Preparing "+mode+" release…");

        final String finalTok=tok, finalKey=keystore;
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

                JSONObject res=post("https://api.github.com/repos/"+ENGINE_REPO+"/actions/workflows/"+WORKFLOW+"/dispatches",finalTok,body.toString());
                long id=res.optLong("workflow_run_id",0);
                if(id==0) throw new Exception("GitHub started no identifiable workflow run.");
                currentRunId=id;
                runOnUiThread(()->{actionBtn.setText("BUILDING…");setStatus("Run #"+id+"\nQueued on GitHub Actions…");});
                poll(finalTok,id,repo);
            }catch(Exception e){showError(e);runOnUiThread(()->resetAction());}
        }).start();
    }

    private void poll(String tok,long id,String repo)throws Exception{
        String url="https://api.github.com/repos/"+ENGINE_REPO+"/actions/runs/"+id;
        while(true){
            JSONObject j=get(url,tok);
            String st=j.optString("status"), con=j.optString("conclusion");
            runOnUiThread(()->setStatus("Run #"+id+"\nStatus: "+friendlyStatus(st)+"\n"+("completed".equals(st)?"Result: "+friendlyConclusion(con):"GitHub Actions is building your release.")));
            if("completed".equals(st)){
                if(!"success".equals(con)){
                    historyStore.add(repo,mode,"FAILED",id);runOnUiThread(this::refreshHistory);
                    throw new Exception("Build failed on GitHub Actions. Run #"+id);
                }
                break;
            }
            Thread.sleep(5000);
        }

        JSONObject a=get("https://api.github.com/repos/"+ENGINE_REPO+"/actions/runs/"+id+"/artifacts",tok);
        JSONArray arr=a.optJSONArray("artifacts");
        if(arr==null||arr.length()==0) throw new Exception("Build completed, but no release artifact was produced.");
        JSONObject artifact=arr.getJSONObject(0);
        currentArtifactName=artifact.optString("name","Repo2Play-Result");
        long aid=artifact.getLong("id");
        currentArtifactUrl="https://api.github.com/repos/"+ENGINE_REPO+"/actions/artifacts/"+aid+"/zip";
        historyStore.add(repo,mode,"SUCCESS",id);
        runOnUiThread(()->{
            refreshHistory();
            setStatus("✓ RELEASE READY\n"+currentArtifactName+"\n\nAPK • AAB • signing key • reports");
            actionBtn.setText("DOWNLOAD RELEASE");
            actionBtn.setEnabled(true);
            actionBtn.setOnClickListener(v->downloadResult());
        });
    }

    private void downloadResult(){
        String tok=secure.get("github_token");
        if(currentArtifactUrl.isEmpty()||tok.isEmpty())return;
        actionBtn.setEnabled(false);actionBtn.setText("DOWNLOADING…");
        setStatus("Downloading release package…");
        new Thread(()->{
            try{
                HttpURLConnection first=req(currentArtifactUrl,tok);first.setInstanceFollowRedirects(false);
                int code=first.getResponseCode();String loc=first.getHeaderField("Location");first.disconnect();
                if(code/100!=3||loc==null)throw new Exception("GitHub artifact download could not start.");

                HttpURLConnection dl=(HttpURLConnection)new URL(loc).openConnection();
                dl.setConnectTimeout(20000);dl.setReadTimeout(60000);
                File tmp=new File(getCacheDir(),"release-"+System.currentTimeMillis()+".zip");
                try(InputStream in=dl.getInputStream();OutputStream out=new FileOutputStream(tmp)){
                    byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);
                }
                dl.disconnect();

                if("NEW".equals(mode)){
                    String key=extractKeystoreBase64(tmp);
                    if(!key.isEmpty())secure.put(vaultName(currentRepo),key);
                }
                saveToDownloads(tmp,currentArtifactName+".zip");
                runOnUiThread(()->{
                    vaultLabel.setText("Signing vault: project key secured for "+currentRepo);
                    setStatus("✓ DOWNLOADED\nDownloads/"+currentArtifactName+".zip\n\nSigning key backed up inside the ZIP and encrypted in this app.");
                    resetAction();
                });
            }catch(Exception e){showError(e);runOnUiThread(this::resetAction);}
        }).start();
    }

    private void pickKey(){
        String repo=normalizeRepo(target.getText().toString());
        if(repo==null){target.setError("Enter the target repository first");return;}
        currentRepo=repo;
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i,PICK_JKS);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==PICK_JKS&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri uri=data.getData();
            try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);
                String enc=Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);
                secure.put(vaultName(currentRepo),enc);
                vaultLabel.setText("Signing vault: key imported for "+currentRepo);
                setStatus("✓ Signing key imported securely");
            }catch(Exception e){showError(e);}
        }
    }

    private String extractKeystoreBase64(File zip)throws Exception{
        try(ZipInputStream zin=new ZipInputStream(new FileInputStream(zip))){
            ZipEntry e;
            while((e=zin.getNextEntry())!=null){
                if(!e.isDirectory()&&e.getName().endsWith(".jks")){
                    ByteArrayOutputStream out=new ByteArrayOutputStream();
                    byte[] b=new byte[4096];int n;while((n=zin.read(b))>0)out.write(b,0,n);
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
            if(u==null)throw new IOException("Cannot create Downloads file.");
            try(InputStream in=new FileInputStream(src);OutputStream out=getContentResolver().openOutputStream(u)){
                byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);
            }
        }else{
            File dir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File dst=new File(dir,name);
            try(InputStream in=new FileInputStream(src);OutputStream out=new FileOutputStream(dst)){
                byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);
            }
        }
    }

    private String vaultName(String repo){return "jks_"+repo.toLowerCase(Locale.US).replace('/','_');}
    private void refreshHistory(){history.setText(historyStore.render());}
    private void resetAction(){actionBtn.setText("BUILD RELEASE");actionBtn.setEnabled(true);actionBtn.setOnClickListener(v->dispatch());}
    private void setMode(String m){
        mode=m;boolean n="NEW".equals(m);
        newBtn.setTextColor(n?BG:TXT);newBtn.setBackground(round(n?GOLD:CARD2,13));
        updateBtn.setTextColor(!n?BG:TXT);updateBtn.setBackground(round(!n?GOLD:CARD2,13));
        String repo=normalizeRepo(target.getText().toString());
        if("UPDATE".equals(m)&&repo!=null&&!secure.get(vaultName(repo)).isEmpty())
            vaultLabel.setText("Signing vault: matching key found for "+repo);
        else if("UPDATE".equals(m)) vaultLabel.setText("Signing vault: UPDATE requires the original project key.");
        else vaultLabel.setText("Signing vault: NEW builds create and save a project key automatically.");
    }

    private String friendlyStatus(String s){
        if("queued".equals(s))return"Queued";
        if("in_progress".equals(s))return"Building";
        if("completed".equals(s))return"Completed";
        return s;
    }
    private String friendlyConclusion(String s){return"success".equals(s)?"Success":s;}
    private void showError(Exception e){
        String m=e.getMessage()==null?"Unknown error":e.getMessage();
        if(m.contains("401"))m="GitHub rejected the token. Reconnect with a valid token.";
        else if(m.contains("403"))m="GitHub permission denied. The token needs Actions access to the Repo2Play engine.";
        else if(m.contains("404"))m="Repository or workflow not found, or the token cannot access it.";
        final String msg=m;
        runOnUiThread(()->setStatus("× BUILD ERROR\n\n"+msg+"\n\nRun #"+(currentRunId==0?"—":currentRunId)));
    }

    private JSONObject get(String u,String tok)throws Exception{HttpURLConnection c=req(u,tok);return parse(c);}
    private JSONObject post(String u,String tok,String body)throws Exception{
        HttpURLConnection c=req(u,tok);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");
        try(OutputStream o=c.getOutputStream()){o.write(body.getBytes(StandardCharsets.UTF_8));}
        return parse(c);
    }
    private HttpURLConnection req(String u,String tok)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();
        c.setConnectTimeout(15000);c.setReadTimeout(30000);
        c.setRequestProperty("Accept","application/vnd.github+json");
        c.setRequestProperty("Authorization","Bearer "+tok);
        c.setRequestProperty("X-GitHub-Api-Version","2022-11-28");
        c.setRequestProperty("User-Agent","Repo2Play-Android/12");
        return c;
    }
    private JSONObject parse(HttpURLConnection c)throws Exception{
        int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();
        String x=read(in);c.disconnect();
        if(code<200||code>=300)throw new Exception("GitHub HTTP "+code+(x.isEmpty()?"":"\n"+x));
        return x.trim().isEmpty()?new JSONObject():new JSONObject(x);
    }
    private String read(InputStream in)throws Exception{
        if(in==null)return"";BufferedReader b=new BufferedReader(new InputStreamReader(in));
        StringBuilder s=new StringBuilder();String l;while((l=b.readLine())!=null)s.append(l);return s.toString();
    }
    private String normalizeRepo(String x){
        if(x==null)return null;x=x.trim().replaceFirst("^https?://github\\.com/","").replaceFirst("\\.git$","").replaceAll("/+$","");
        return x.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")?x:null;
    }
    private void hideKeyboard(){try{((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(target.getWindowToken(),0);}catch(Exception ignored){}}
    private void setStatus(String x){status.setText(x);}

    private LinearLayout card(){LinearLayout l=col();l.setPadding(d(18),d(18),d(18),d(18));l.setBackground(round(CARD,18));return l;}
    private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private TextView section(String s){TextView v=t(s,12,GOLD,true);v.setLetterSpacing(.08f);return v;}
    private TextView label(String s){TextView v=t(s,11,MUT,true);v.setPadding(0,0,0,d(7));return v;}
    private TextView t(String s,int sp,int c,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(c);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private EditText input(String h){EditText e=new EditText(this);e.setHint(h);e.setHintTextColor(Color.rgb(95,101,112));e.setTextColor(TXT);e.setTextSize(15);e.setSingleLine(true);e.setPadding(d(14),0,d(14),0);e.setBackground(round(CARD2,13));return e;}
    private Button primary(String s){Button b=new Button(this);b.setText(s);b.setTextSize(14);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(BG);b.setBackground(round(GOLD,14));return b;}
    private Button secondary(String s){Button b=new Button(this);b.setText(s);b.setTextSize(13);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(TXT);b.setBackground(round(CARD2,13));return b;}
    private Button modeButton(String s,boolean on){Button b=on?primary(s):secondary(s);return b;}
    private LinearLayout.LayoutParams field(){return new LinearLayout.LayoutParams(-1,d(54));}
    private LinearLayout.LayoutParams buttonParams(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,d(56));p.setMargins(0,d(14),0,0);return p;}
    private LinearLayout.LayoutParams smallButtonParams(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,d(48));p.setMargins(0,d(10),0,0);return p;}
    private View space(int h){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(1,d(h)));return v;}
    private GradientDrawable round(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(d(r));return g;}
    private int d(int x){return(int)(x*getResources().getDisplayMetrics().density+.5f);}
}
