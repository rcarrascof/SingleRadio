package com.app.AlofokeFm.services.parser;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ParserM3UToURL {

    private ParserM3UToURL() {
        throw new IllegalStateException("Utility class");
    }

    @Nullable
    public static String parse(String urlM3u, @NonNull String type) {
        String line = null;
        try {
            URL urlPage = new URL(urlM3u);
            HttpURLConnection connection = (HttpURLConnection) urlPage.openConnection();
            InputStream inputStream = connection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuffer = new StringBuilder();
            if (type.equals("m3u")) {
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.contains("http")) {
                        connection.disconnect();
                        bufferedReader.close();
                        inputStream.close();
                        return line;
                    }
                    stringBuffer.append(line);
                }
            } else {
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.contains("http")) {
                        connection.disconnect();
                        bufferedReader.close();
                        line = line.split("http")[1];
                        line = "http" + line;
                        Log.e("line", line);
                        return line;
                    }
                    stringBuffer.append(line);
                }
            }
            connection.disconnect();
            bufferedReader.close();
            inputStream.close();
        } catch (IOException e) {
            return null;
        }
        return null;
    }
}