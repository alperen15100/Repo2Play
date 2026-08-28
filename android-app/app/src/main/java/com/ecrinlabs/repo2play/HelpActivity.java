package com.ecrinlabs.repo2play;

import android.app.*;
import android.os.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

public class HelpActivity extends Activity {

    int BG=Color.rgb(10,12,16);
    int SURFACE=Color.rgb(18,21,27);
    int TXT=Color.rgb(244,241,234);
    int MUT=Color.rgb(160,165,174);
    int GOLD=Color.rgb(178,151,96);

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        ScrollView sv=new ScrollView(this);
        sv.setBackgroundColor(BG);

        LinearLayout page=new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(d(20),d(24),d(20),d(40));
        sv.addView(page);

        TextView back=t("‹ BACK",14,GOLD,true);
        back.setOnClickListener(v->finish());
        back.setPadding(0,0,0,d(18));
        page.addView(back);

        page.addView(t("HOW TO USE",30,TXT,true));

        TextView sub=t(
            "Complete step-by-step Repo2Play guide",
            14,MUT,false
        );
        sub.setPadding(0,d(6),0,d(20));
        page.addView(sub);

        add(page,"WHAT REPO2PLAY DOES","Repo2Play builds, signs and packages an EXISTING Android project from GitHub.\n\nA successful build can produce:\n• APK — install and test on an Android phone.\n• AAB — upload to Google Play Console.\n• JKS — signing identity for future updates.\n\nRepo2Play does not create an application from scratch and it cannot automatically turn any random GitHub repository into an Android app.");
        add(page,"BEFORE YOU START","Open the repository on GitHub and make sure it is really an Android project.\n\nTypical Android projects contain files such as:\n• app/\n• gradlew\n• settings.gradle or settings.gradle.kts\n• build.gradle or build.gradle.kts\n\nIf the original source code is broken, has missing dependencies or cannot compile, Repo2Play may also fail to build it.");
        add(page,"CONNECT GITHUB","Create a GitHub Personal Access Token from:\n\nGitHub → Settings → Developer settings → Personal access tokens\n\nGive the token the repository and GitHub Actions access required for the projects you intend to build.\n\nPaste the token into Repo2Play and tap CONNECT SECURELY.\n\nNever publish or share your token.");
        add(page,"YOUR OWN REPOSITORY","If the Android project is already inside YOUR GitHub account, normally you do not need to fork it.\n\nYou can enter:\n\nusername/project\n\nor:\n\nhttps://github.com/username/project");
        add(page,"SOMEONE ELSE'S REPOSITORY — FORK","If the open-source project belongs to another GitHub user, you may need to fork it first.\n\nOn GitHub:\n1. Open the original repository.\n2. Tap FORK.\n3. Select your GitHub account.\n4. Tap CREATE FORK.\n5. Wait for GitHub to create your copy.\n6. Use YOUR forked repository in Repo2Play.\n\nExample:\n\nOriginal:\noriginal-owner/sample-android-app\n\nAfter fork:\nyourusername/sample-android-app\n\nOnly build or redistribute source code when its license and your rights allow it.");
        add(page,"CHECK THE BRANCH","This is one of the most important steps.\n\nNot every repository uses main.\n\nSome repositories use:\n• main\n• master\n• another branch name\n\nOpen the repository on GitHub and check the branch selector above the file list.\n\nEnter EXACTLY the same branch name in Repo2Play.\n\nWrong branch = repository checkout can fail before Android compilation even starts.");
        add(page,"REAL BRANCH EXAMPLE","For example, a repository may be entered with the main branch and fail during checkout.\n\nIf that repository actually uses master, change the branch field from main to master and start the build again.\n\nAlways use the exact branch name shown on GitHub. If a build fails very early, checking the branch should be one of your first troubleshooting steps.");
        add(page,"INSTALL BUILD ENGINE","Before the first Repo2Play build for a repository, tap INSTALL BUILD ENGINE.\\n\\nRepo2Play installs one self-contained workflow file into YOUR repository:\\n\\n.github/workflows/repo2play-build.yml\\n\\nYou normally need to do this only once for that repository.\\n\\nThe build engine runs inside GitHub Actions when BUILD RELEASE is started. Repo2Play does not use an Ecrin Labs server to compile your application.");
        add(page,"YOUR OWN GITHUB ACTIONS","Repo2Play builds are designed to run in YOUR repository and YOUR GitHub Actions environment.\\n\\nFlow:\\n\\nYour repository or fork\\n→ Repo2Play Build workflow\\n→ Your GitHub Actions\\n→ Signed APK + AAB + JKS\\n\\nThis means the build job, run history and release artifact appear in the GitHub Actions section of your repository.");
        add(page,"NEW BUILD","Choose NEW for the FIRST Repo2Play release of an application.\n\nTypical flow:\n\nGitHub Android Project\n→ NEW\n→ BUILD RELEASE\n→ APK + AAB + signing key\n\nNEW can create and secure the signing identity used by the project.");
        add(page,"JKS AND SIGNING","A JKS contains Android signing credentials.\n\nThe same application's later updates must preserve the correct signing identity.\n\nKeep your original signing material safe.\n\nRepo2Play's Signing Vault stores project signing information locally so it can be reused for UPDATE builds.");
        add(page,"IMPORTANT — KEEP YOUR JKS","After your first successful NEW build, keep the repo2play-upload.jks file in a safe place.\n\nThis file represents the signing identity of your application. Future updates to the same application must use the same signing identity.\n\nDo not lose or replace the original JKS. A different JKS may prevent the new APK from installing over the existing application.\n\nEven when Signing Vault is available, keeping your original JKS backup is strongly recommended.");

        add(page,"UPDATE BUILD","Choose UPDATE when building a newer version of the SAME application.\n\nBefore BUILD RELEASE:\n\n1. Use the same repository.\n2. Check the correct branch.\n3. Select UPDATE.\n4. Check Signing Vault.\n5. If the original project key is not available, tap IMPORT ORIGINAL JKS.\n6. Select repo2play-upload.jks from the original successful NEW release.\n7. Confirm that the original key is secured/imported.\n8. Tap BUILD RELEASE.\n\nThe UPDATE build must use the same signing identity as the original application.\n\nNever use a JKS from another project. A different signing identity may prevent Android from installing the APK over the existing application.");
        add(page,"IMPORT ORIGINAL JKS","Use IMPORT ORIGINAL JKS only when the application was originally signed elsewhere, Repo2Play was reinstalled or cleared, or the correct original signing key is not already stored in the Signing Vault.\n\nImport ONLY the original key belonging to that application.\n\nDo not use a random JKS or a key from another app.");
        add(page,"BEFORE BUILD RELEASE","Check:\n\n✓ GitHub connected\n✓ Correct repository\n✓ Repository accessible by your account\n✓ Correct main/master/other branch\n✓ Android project\n✓ NEW for first release\n✓ UPDATE only for the same existing application\n✓ Correct signing key available for UPDATE");
        add(page,"WHAT HAPPENS DURING BUILD","After BUILD RELEASE:\n\n1. Repo2Play starts GitHub Actions.\n2. GitHub accesses the target repository.\n3. The selected branch is checked out.\n4. Android/Gradle environment is prepared.\n5. Source code is compiled.\n6. Release packages are signed.\n7. APK/AAB deliverables are created.\n8. Results are packaged as a GitHub Actions artifact.\n\nBuild time varies by project and GitHub Actions availability.");
        add(page,"BUILD STATUS","QUEUED — GitHub received the job.\n\nBUILDING — GitHub Actions is compiling the project.\n\nRELEASE READY — output was produced successfully.\n\nFAILED / BUILD ERROR — the build could not complete.\n\nA BUILD ERROR does not automatically mean Repo2Play itself is broken.");
        add(page,"IF BUILD FAILS","Check these in order:\n\n1. Repository URL correct?\n2. Repository accessible?\n3. Need to fork it first?\n4. Branch really main or master?\n5. Is it an Android project?\n6. Gradle files present?\n7. Source code has compile errors?\n8. Missing API keys or secret configuration?\n9. Old or unavailable dependencies?\n10. Project requires a special build environment?\n\nKeep the GitHub Actions run number. It helps identify the exact failed stage.");
        add(page,"APK VS AAB","APK:\nInstall directly on an Android device for testing.\n\nAAB:\nAndroid App Bundle normally used for Google Play release uploads.\n\nSimple rule:\n\nAPK → test on phone\nAAB → Play Console");
        add(page,"GOOGLE PLAY","A successful AAB build does NOT guarantee Google Play approval.\n\nYour application may also require:\n\n• Current target API compliance\n• Privacy Policy\n• Data Safety declaration\n• App Access instructions when required\n• Content rating\n• Correct permission usage\n• Store listing\n• Testing requirements\n• Rights to all code and assets\n\nThe publishing developer is responsible for the application's content, rights and compliance.");
        add(page,"COMMON MISTAKES","• Using someone else's repository without required access\n• Forgetting to fork when necessary\n• Entering main when the repo uses master\n• Wrong repository URL\n• Trying to build a non-Android repository\n• Choosing UPDATE for the first build\n• Using the wrong signing key\n• Confusing APK with AAB\n• Assuming every GitHub project is automatically buildable\n• Assuming every build failure is a Repo2Play failure");
        add(page,"QUICK START — YOUR PROJECT","1. Put the Android project on GitHub.\n2. Connect GitHub in Repo2Play.\n3. Paste the repository address.\n4. Check the exact branch on GitHub.\n5. Select NEW for the first release.\n6. Tap BUILD RELEASE.\n7. Wait for RELEASE READY.\n8. Download the package.\n9. Install and test the APK.\n10. Use the AAB for Play Console when ready.");

        add(page,"COMPLETE NEW → UPDATE FLOW","FIRST RELEASE\\n\\nFork or use your own repository\\n→ Check exact branch\\n→ Connect GitHub\\n→ INSTALL BUILD ENGINE\\n→ NEW\\n→ BUILD RELEASE\\n→ Your GitHub Actions runs\\n→ Download signed APK + AAB + JKS\\n→ Install and test APK\\n\\nNEXT RELEASE\\n\\nOpen the same project\\n→ Use the correct branch\\n→ Select UPDATE\\n→ Confirm the original signing key is in Signing Vault or import the original JKS\\n→ BUILD RELEASE\\n→ Your GitHub Actions runs again\\n→ Download the new signed APK + AAB\\n→ Install the new APK over the previous application\\n\\nIf Android accepts the new APK as an update to the installed application, the signing continuity is correct.");

        TextView foot=t(
            "Repo2Play v13.2 • by Ecrin Labs",
            11,MUT,false
        );
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0,d(20),0,0);
        page.addView(foot);

        setContentView(sv);
    }

    void add(LinearLayout page,String title,String body){
        LinearLayout c=new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(d(18),d(18),d(18),d(18));

        GradientDrawable g=new GradientDrawable();
        g.setColor(SURFACE);
        g.setCornerRadius(d(18));
        c.setBackground(g);

        LinearLayout.LayoutParams p=
            new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,0,0,d(14));
        page.addView(c,p);

        c.addView(t(title,14,GOLD,true));

        TextView b=t(body,13,TXT,false);
        b.setPadding(0,d(10),0,0);
        b.setLineSpacing(d(4),1f);
        c.addView(b);
    }

    TextView t(String s,int z,int color,boolean bold){
        TextView v=new TextView(this);
        v.setText(s);
        v.setTextSize(z);
        v.setTextColor(color);
        if(bold)v.setTypeface(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.BOLD
        );
        return v;
    }

    int d(int x){
        return (int)(
            x*getResources().getDisplayMetrics().density+.5f
        );
    }
}
