package org.example;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import static java.lang.Math.floor;

public class Jira {
    private static String readAll(Reader rd) throws IOException {
	    StringBuilder sb = new StringBuilder();
        int cp;
	    while ((cp = rd.read()) != -1) {
	        sb.append((char) cp);
        }
	    return sb.toString();
    }

    public static JSONArray readJsonArrayFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONArray json = new JSONArray(jsonText);
            return json;
        } finally {
            is.close();
        }
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            return json;
        } finally {
            is.close();
        }
    }

    public static ArrayList<Release> getAllRelease(String projName) throws IOException {

        ArrayList<Release> releases = new ArrayList<>();

        String url = "https://issues.apache.org/jira/rest/api/2/project/"+projName+"/versions";
        JSONArray json = readJsonArrayFromUrl(url);
        int tot =json.length();
        int i;
        for (i=0; i<tot; i++){
            String nameRelease = json.getJSONObject(i).get("name").toString();
            String released = json.getJSONObject(i).get("released").toString();
            if(released.equalsIgnoreCase("true")){
                String dateRelease = json.getJSONObject(i).get("releaseDate").toString();

                Release element = new Release(nameRelease, dateRelease);
                releases.add(element);
                System.out.println(nameRelease);
                System.out.println(dateRelease);
                System.out.println("+++++++++++++++++++++++++++++++");
            }
        }

        return releases;
    }

    public static ArrayList<Release> getHalfReleases(ArrayList<Release> allRelease){
        int size, halfSize, i;
        size = allRelease.size();
        halfSize = (int) floor(size/2);
        ArrayList<Release> halfRelease = new ArrayList<>();

        i=0;
        for(Release element: allRelease){
            halfRelease.add(element);
            i++;
            if(i==halfSize) break;
        }
        return halfRelease;
    }


    public static ArrayList<Issue> getIssueIdOfProject(String projName) throws IOException, JSONException{

        ArrayList<Issue> bugInfo = new ArrayList<>();
        int len, fixLen;

        Integer j = 0, i = 0, total = 1;

        do {
            j = i + 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project%20%3D%20" + projName +
                    "%20AND%20issuetype%20%3D%20Bug%20AND%20(%22status%22%20%3D%22resolved%22%20OR%20%22status" +
                    "%22%20%3D%20%22closed%22)%20AND%20%20%22resolution%22%20%3D%20%22fixed%22%20&fields=key," +
                    "resolutiondate,versions,created,fixVersions&startAt="+i.toString()+ "&maxResults="+ j.toString();

            JSONObject json = readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");
            int k;
            for (; i < total && i < j; i++) {

                JSONObject currentJson = issues.getJSONObject(i%1000);
                String key = currentJson.get("key").toString();
                String versionJsonString = currentJson.getJSONObject("fields").get("versions").toString();
                String fixVersionsJsonString = currentJson.getJSONObject("fields").get("fixVersions").toString();
                String resolutionDate = currentJson.getJSONObject("fields").get("resolutiondate").toString();
                String creationDate = currentJson.getJSONObject("fields").get("created").toString();

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

                Issue issue = new Issue(key, fixVersions, affectedVersions, resolutionDate, creationDate);
                int ivIndex, ovIndex, fvIndex;
                if (issue.injectedVersion != null)
                    ivIndex = issue.injectedVersion.index;
                else
                    ivIndex = -1;
                ovIndex = issue.openingVersion.index;
                fvIndex = issue.fixVersion.index;

                // scarto l'issue se non è post release (ha IV=OV=FV) e se presenta un'inconsistenza tra ov ed fv (OV>FV)

                if((ovIndex <= fvIndex) && !((ivIndex==ovIndex)&&(ovIndex==fvIndex)))
                    bugInfo.add(issue);
            }
        } while (i < total);

        System.out.println("ho trovato #bug="+bugInfo.size());
        return bugInfo;
    }

}
