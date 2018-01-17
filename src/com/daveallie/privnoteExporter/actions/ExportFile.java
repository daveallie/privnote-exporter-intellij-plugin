package com.daveallie.privnoteExporter.actions;

import com.daveallie.privnoteExporter.helpers.AES;
import com.daveallie.privnoteExporter.helpers.Version;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.HttpsURLConnection;
import java.awt.datatransfer.StringSelection;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class ExportFile extends AnAction {
    private static String PRIVNOTE_URL = "https://privnote.com/";
    private static int AUTO_PASS_LENGTH = 9;
    private static String AUTO_PASS_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        SelectionModel editorSelection = editor.getSelectionModel();
        String content = editorSelection.hasSelection() ? editorSelection.getSelectedText() : editor.getDocument().getText();

        if (content == null) {
            Messages.showErrorDialog("No content to send to Privnote.", "Privnote Upload Failed");
            return;
        }

        String password = makePassword();

        String privnoteLink;
        try {
            privnoteLink = sendPrivnote(AES.encrypt(content, password), password);
        } catch (NoSuchAlgorithmException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException | IOException | IllegalBlockSizeException exception) {
            Messages.showErrorDialog(exception.getMessage(), "Privnote Upload Failed");
            return;
        }

        CopyPasteManager.getInstance().setContents(new StringSelection(privnoteLink));
        Messages.showMessageDialog(project, "Privnote link copied to clipboard!", "Privnote Uploaded", Messages.getInformationIcon());
    }

    private String sendPrivnote(String encrpytedText, String password) throws IOException {
        String body = "data=" + encrpytedText +
                "&has_manual_pass=false" +
                "&duration_hours=0" +
                "&data_type=T" +
                "&notify_email=" +
                "&notify_ref=";

        URL myurl = new URL(PRIVNOTE_URL);
        HttpsURLConnection con = (HttpsURLConnection) myurl.openConnection();
        con.setRequestMethod("POST");

        con.setRequestProperty("Content-length", String.valueOf(body.length()));
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setRequestProperty("Accept", "application/json, text/javascript, */*");
        con.setRequestProperty("Origin", "https://privnote.com");
        con.setRequestProperty("Referer", "https://privnote.com");
        con.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        con.setRequestProperty("Connection", "keep-alive");
        con.setRequestProperty("DNT", "1");
        con.setRequestProperty("User-Agent", "privnote-exporter/" + Version.CURRENT_VERSION + " (https://github.com/daveallie/privnote-exporter-intellij-plugin)");
        con.setDoOutput(true);
        con.setDoInput(true);

        DataOutputStream output = new DataOutputStream(con.getOutputStream());
        output.writeBytes(body);
        output.close();

        StringBuilder responseBody = new StringBuilder();
        DataInputStream input = new DataInputStream(con.getInputStream());
        for (int c = input.read(); c != -1; c = input.read()) {
            responseBody.append((char) c);
        }
        input.close();

        JsonObject responseBodyObject = new JsonParser().parse(responseBody.toString()).getAsJsonObject();
        return responseBodyObject.get("note_link").getAsString() + "#" + password;
    }

    private String makePassword() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < AUTO_PASS_LENGTH; i++) {
            int pos = (int) Math.floor(Math.random() * AUTO_PASS_CHARS.length());
            sb.append(AUTO_PASS_CHARS.charAt(pos));
        }

        return sb.toString();
    }
}
