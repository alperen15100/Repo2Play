package com.ecrinlabs.repo2play;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class HistoryStore {
    private final SharedPreferences prefs;
    public HistoryStore(Context c) { prefs = c.getSharedPreferences("history", Context.MODE_PRIVATE); }

    public synchronized void add(String repo, String mode, String result, long runId) {
        try {
            JSONArray old = new JSONArray(prefs.getString("items", "[]"));
            JSONArray fresh = new JSONArray();
            JSONObject item = new JSONObject();
            item.put("repo", repo);
            item.put("mode", mode);
            item.put("result", result);
            item.put("runId", runId);
            item.put("time", new SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(new Date()));
            fresh.put(item);
            for (int i = 0; i < old.length() && i < 9; i++) fresh.put(old.getJSONObject(i));
            prefs.edit().putString("items", fresh.toString()).apply();
        } catch (Exception ignored) {}
    }

    public String render() {
        try {
            JSONArray a = new JSONArray(prefs.getString("items", "[]"));
            if (a.length() == 0) return "No builds yet.";
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                s.append(o.optString("result").equals("SUCCESS") ? "✓ " : "× ")
                 .append(o.optString("mode")).append("  ")
                 .append(o.optString("repo")).append("\n")
                 .append("   ").append(o.optString("time"))
                 .append("  • Run #").append(o.optLong("runId")).append("\n");
            }
            return s.toString().trim();
        } catch (Exception e) {
            return "No builds yet.";
        }
    }
}
