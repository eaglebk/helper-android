package ru.bsuirhelper.android.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.widget.Toast;
import ru.bsuirhelper.android.core.schedule.Lesson;
import ru.bsuirhelper.android.core.schedule.ScheduleManager;
import ru.bsuirhelper.android.core.schedule.ScheduleParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Влад on 18.02.14.
 */
public class DownloadScheduleTask extends AsyncTask<String, Integer, String> {
    private ProgressDialog mPogressDialog;
    private Fragment fragment;
    private Context context;
    private String message = "Обновление расписания";
    private ScheduleManager scheduleManager;

    public static interface CallBack {
        public void onPostExecute();
    }

    public DownloadScheduleTask(Fragment fragment) {
        super();
        this.fragment = fragment;
        this.context = fragment.getActivity();
        scheduleManager = ScheduleManager.getInstance(fragment.getActivity());
    }

    private File downloadScheduleFromInternet(String groupId) {
        final String LIST_URL = "http://www.bsuir.by/psched/rest/";
        final String TEMP_FILE_NAME = "schedule.xml";
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(LIST_URL + groupId);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            // download the file
            input = connection.getInputStream();
            output = context.openFileOutput(TEMP_FILE_NAME, Context.MODE_PRIVATE);

            byte data[] = new byte[4096];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (connection != null)
                connection.disconnect();
        }
        return new File(context.getApplicationContext().getFilesDir() + "/" + TEMP_FILE_NAME);
    }


    @Override
    protected String doInBackground(String... urls) {
        String groupId = urls[0];
        File xmlFile = downloadScheduleFromInternet(groupId);
        if (xmlFile == null) {
            return "Error";
        }
        ArrayList<Lesson> lessons = ScheduleParser.parseXmlSchedule(xmlFile);
        scheduleManager.addSchedule(groupId, lessons);
        return "Success";
    }

    @Override
    public void onPreExecute() {
        showProgressDialog();
    }

    @Override
    public void onPostExecute(String result) {
        mPogressDialog.cancel();
        if (result.equals("Error")) {
            Toast.makeText(context.getApplicationContext(), "Произошла ошибка", Toast.LENGTH_LONG).show();
        } else {
            try {
                CallBack callBack = (CallBack) fragment;
                callBack.onPostExecute();
            } catch (ClassCastException e) {
                System.out.print("Fragment must implement CallBack Interface" + e.getMessage());
            }
        }
    }

    private void showProgressDialog() {
        mPogressDialog = new ProgressDialog(context);
        mPogressDialog.setMessage(message);
        mPogressDialog.setCancelable(false);
        mPogressDialog.show();
    }

    public void setPogressDialogMessage(String message) {
        this.message = message;
    }
}