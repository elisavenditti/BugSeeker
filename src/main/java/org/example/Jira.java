package org.example;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Math.floor;

public class Jira {
    private static final String FIELD = "field";
    private Jira() {

    }

    private static String readAll(Reader rd) throws IOException {
	    StringBuilder sb = new StringBuilder();
        int cp;
	    while ((cp = rd.read()) != -1) {
	        sb.append((char) cp);
        }
	    return sb.toString();
    }

    public static JSONArray readJsonArrayFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            return new JSONArray(jsonText);
        }
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        }
    }

    public static List<Release> getAllRelease(String projName) throws IOException {

        ArrayList<Release> releases = new ArrayList<>();

        String url = "https://issues.apache.org/jira/rest/api/2/project/"+projName+"/versions";
        JSONArray json = readJsonArrayFromUrl(url);
        int tot =json.length();
        int i;
        for (i=0; i<tot; i++){
            String nameRelease = json.getJSONObject(i).get("name").toString();
            String released = json.getJSONObject(i).get("released").toString();
            if(released.equalsIgnoreCase("true")){
                try{
                    String dateRelease = json.getJSONObject(i).get("releaseDate").toString();
                    Release element = new Release(nameRelease, dateRelease);
                    releases.add(element);
                }catch (JSONException e){
                    Logger logger = Logger.getLogger(Issue.class.getName());
                    logger.log(Level.INFO, "["+projName+"] - una release non possiede la data di rilascio. Release saltata.");
                }
            }
        }

        return releases;
    }

    public static List<Release> getHalfReleases(List<Release> allRelease){
        int size;
        int halfSize;
        int i;
        size = allRelease.size();
        halfSize = (int) floor(size/(double)2);
        ArrayList<Release> halfRelease = new ArrayList<>();

        i=0;
        for(Release element: allRelease){
            halfRelease.add(element);
            i++;
            if(i == halfSize) break;
        }
        return halfRelease;
    }


    public static List<Issue> getIssueIdOfProject(String projName) throws IOException, JSONException{

        ArrayList<Issue> bugInfo = new ArrayList<>();
        int len;
        int fixLen;

        int j;
        int i = 0;
        int total;

        do {
            j = i + 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project%20%3D%20" + projName +
                    "%20AND%20issuetype%20%3D%20Bug%20AND%20(%22status%22%20%3D%22resolved%22%20OR%20%22status" +
                    "%22%20%3D%20%22closed%22)%20AND%20%20%22resolution%22%20%3D%20%22fixed%22%20&fields=key," +
                    "resolutiondate,versions,created,fixVersions&startAt="+i+ "&maxResults="+ j;

            JSONObject json = readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");
            int k;
            for (; i < total && i < j; i++) {

                JSONObject currentJson = issues.getJSONObject(i%1000);
                String key = currentJson.get("key").toString();
                String versionJsonString = currentJson.getJSONObject(FIELD).get("versions").toString();
                String fixVersionsJsonString = currentJson.getJSONObject(FIELD).get("fixVersions").toString();
                String resolutionDate = currentJson.getJSONObject(FIELD).get("resolutiondate").toString();
                String creationDate = currentJson.getJSONObject(FIELD).get("created").toString();

                JSONArray versionJsonArray = new JSONArray(versionJsonString);
                JSONArray fixVersionJsonArray = new JSONArray(fixVersionsJsonString);
                len = versionJsonArray.length();
                fixLen = fixVersionJsonArray.length();

                ArrayList<String> affectedVersions = new ArrayList<>();
                for(k = 0; k < len; k++){
                    String curr = versionJsonArray.getJSONObject(k).get("name").toString();
                    if(!affectedVersions.contains(curr))
                        affectedVersions.add(curr);
                }
                ArrayList<String> fixVersions = new ArrayList<>();
                for(k = 0; k < fixLen; k++){
                    String curr = fixVersionJsonArray.getJSONObject(k).get("name").toString();
                    if(!fixVersions.contains(curr))
                        fixVersions.add(curr);
                }

                Issue issue = new Issue(key, affectedVersions, resolutionDate, creationDate);
                // discard issues without FV or OV (I can't claculate them in other ways)
                if(issue.getFixVersion() == null || issue.getOpeningVersion()==null) continue;

                int ivIndex;
                int ovIndex;
                int fvIndex;
                if (issue.getInjectedVersion() != null)
                    ivIndex = issue.getInjectedVersion().getIndex();
                else
                    ivIndex = -1;
                ovIndex = issue.getOpeningVersion().getIndex();
                fvIndex = issue.getFixVersion().getIndex();

                // discard issues with OV and FV inconsistent (OV>=FV) and issues pre-release (IV=OV=FV)
                if(!((ivIndex==ovIndex)&&(ovIndex==fvIndex)) && ovIndex < fvIndex)
                        bugInfo.add(issue);
            }
        } while (i < total);
        Logger logger = Logger.getLogger(Issue.class.getName());
        logger.log(Level.INFO, "ho trovato #bug="+bugInfo.size());
        return bugInfo;
    }

}
