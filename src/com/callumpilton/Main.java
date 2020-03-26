package com.callumpilton;
import java.io.*;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {
    static List<Date> dates = new ArrayList<Date>();
    static String prevDirectory;
    static int retry = 0;
    static int count = 0;
    static int dirCount = 0;
    static int updates = 0;
    static int files = 0;
    static List<String> updateLinks = new ArrayList<String>();
    static List<String> failed = new ArrayList<String>();

    public static void main(String[] args) {
        String nam = System.getProperty("user.dir");
        File dir = new File(nam);

        CountFolders(dir);
        Process(dir);

        System.out.println("\nFinished! :)\n");
    }

    static void CountFolders(File aFile) {
        if (aFile.isFile()) {
            files++;
        }
        else if (aFile.isDirectory()) {
            if (files > 0 && aFile.getName().matches(".*\\d.*")) {
                dirCount++;
                files = 0;
            }
            File[] listOfFiles = aFile.listFiles();
            if(listOfFiles!=null) {
                for (int i = 0; i < listOfFiles.length; i++)
                    CountFolders(listOfFiles[i]);
            }
        }
    }

    static void Process(File aFile) {
        if (aFile.isFile()) {
            dates.add(new Date(aFile.lastModified()));
        }
        else if (aFile.isDirectory()) {
            if (!dates.isEmpty()) {
                Date newestDate = getNewestFile(dates);
                dates.clear();
                if(prevDirectory != null && prevDirectory.matches(".*\\d.*")) {
                    String[] intID = prevDirectory.split(" ");
                    intID = intID[0].split("_");
                    try {
                        Date latestVersion = getLatestVersion(Integer.parseInt(intID[0]));

                        if (latestVersion.after(newestDate)) {
                            updates++;
                            updateLinks.add(intID[0]);
                        }
                    } catch (NumberFormatException e) {
                        //wrong format
                    }

                }
            }

            File[] listOfFiles = aFile.listFiles();
            if (listOfFiles != null) {
                for (int i = 0; i < listOfFiles.length; i++)
                    Process(listOfFiles[i]);
            }

            prevDirectory = aFile.getName();
            if (prevDirectory.matches(".*\\d.*")) {
            count++;
                try {
                    new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("Checked " + count + "/" + dirCount + " workshop items. Found " + updates + " updates.");

                System.out.println("\nUpdate Links:");
                if (updateLinks.isEmpty()) {
                    System.out.println("None");
                }
                for (String link : updateLinks) {
                    System.out.println("https://steamcommunity.com/sharedfiles/filedetails/?id=" + link);
                }
                System.out.println("\nFailed to Check:");
                if (failed.isEmpty()) {
                    System.out.println("None");
                }
                for (String link : failed) {
                    System.out.println("https://steamcommunity.com/sharedfiles/filedetails/?id=" + link);
                }
            }
        }
    }

    static Date getNewestFile(List<Date> dates) {
        Date newestDate = dates.get(0);
        for(Date newDate : dates) {
            if(newDate.after(newestDate)) {
                newestDate = newDate;
            }
        }

        return newestDate;
    }

    static Date getLatestVersion(int id) {

        Date latestVersionDate = new Date();
        String dateString;

        String stringID = String.valueOf(id);

        String webpage = getURLSource(stringID);

        if (webpage != null) {

            String[] webSplit = webpage.split("detailsStatRight\">");
            webSplit = webSplit[webSplit.length - 1].split("</div>");
            if (webSplit[0].length() < 23) {
                dateString = webSplit[0];

                if (dateString.length() < 18) {
                    int year = Calendar.getInstance().get(Calendar.YEAR);

                    String[] dateSplit = dateString.split("@");
                    dateString = dateSplit[0].trim() + ", " + year + " @" + dateSplit[1];
                }

                SimpleDateFormat formatter = new SimpleDateFormat("d MMM, yyyy @ h:mmaa");

                try {
                    latestVersionDate = formatter.parse(dateString);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                return latestVersionDate;
            }
        }
        failed.add(stringID);
        return new Date(0);
    }

    public static String getURLSource(String url)
    {
        URL urlObject = null;
        try {
            urlObject = new URL("https://steamcommunity.com/sharedfiles/filedetails/?id=" + url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        URLConnection urlConnection = null;
        try {
            urlConnection = urlObject.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");

        try {
            return toString(urlConnection.getInputStream());
        } catch (IOException e) {
            if (retry < 2) {
                retry++;
                getURLSource(url);
            } else {
                return null;
            }
        }
        return null;
    }

    private static String toString(InputStream inputStream) throws IOException
    {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8")))
        {
            String inputLine;
            StringBuilder stringBuilder = new StringBuilder();
            while ((inputLine = bufferedReader.readLine()) != null)
            {
                stringBuilder.append(inputLine);
            }

            return stringBuilder.toString();
        }
    }

}

