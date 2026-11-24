package com.techpp.glass;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Your own keys and weather area, in the app's folder on the TV (files/tmdb_key.txt, location.txt):
 * the same files the set-*.sh scripts push over adb, and what Control Center → Keys and location writes. Each reader
 * (Trending, Weather) notices a new file on its next refresh. A codermert build may also ship a TMDB key in the APK
 * (tmdb_key.txt next to the Gradle project, never in git).
 */
final class Keys {
    static final String TMDB = "tmdb_key.txt", LOCATION = "location.txt";
    /** A key check's answer: the service accepted it, turned it down, or couldn't be reached. */
    static final int OK = 0, REJECTED = 1, OFFLINE = 2;

    private Keys() { }

    private static File file(Context c, String name) { return new File(c.getExternalFilesDir(null), name); }

    static boolean hasBuiltInTmdb() {
        String k = BuildConfig.TMDB_KEY;
        return k != null && !k.isEmpty();
    }

    /** Copies a build-time TMDB key into the TV's files folder when the file is missing. */
    static void ensureEmbedded(Context c) {
        if (!hasBuiltInTmdb() || read(c, TMDB) != null) return;
        write(c, TMDB, BuildConfig.TMDB_KEY);
    }

    /** The file's text, trimmed, or null when it's missing or empty. */
    static String read(Context c, String name) {
        try (InputStream in = new FileInputStream(file(c, name))) {
            byte[] b = new byte[4096];
            int n = in.read(b);
            String s = n > 0 ? new String(b, 0, n, StandardCharsets.UTF_8).trim() : "";
            if (!s.isEmpty()) return s;
        } catch (IOException ignored) { }
        if (TMDB.equals(name) && hasBuiltInTmdb()) return BuildConfig.TMDB_KEY;
        return null;
    }

    /** Saves the text, or removes the file when it's null or blank. Written whole, then renamed into place. */
    static boolean write(Context c, String name, String text) {
        File f = file(c, name);
        if (text == null || text.trim().isEmpty()) return !f.exists() || f.delete();
        File tmp = new File(f.getPath() + ".tmp");
        try (OutputStream out = new FileOutputStream(tmp)) {
            out.write(text.trim().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            return false;
        }
        return tmp.renameTo(f);
    }

    /** "…3fb6": enough to tell keys apart, never the key. */
    static String masked(String key) {
        return key.length() <= 8 ? "set" : "…" + key.substring(key.length() - 4);
    }
}
