package org.example;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
//                System.out.println(nameRelease);
//                System.out.println(dateRelease);
//                System.out.println("+++++++++++++++++++++++++++++++");
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

    public static Map<String, List<String>> getIssueIdOfVersion(String projName, String version) throws IOException, JSONException{

        List<String> issuesId  = new ArrayList<>();
        List<String> fixVersions = new ArrayList<>();
        Map<String, List<String>> issuesIdMap = new HashMap<>();
        int len, fixLen;

        Integer j = 0, i = 0, total = 1;
        //Get JSON API for closed bugs w/ AV in the project
        do {
            //Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
            j = i + 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project%20%3D%20" + projName +
                    "%20AND%20issuetype%20%3D%20Bug%20AND%20(%22status%22%20%3D%22resolved%22%20OR%20%22status" +
                    "%22%20%3D%20%22closed%22)%20AND%20%20%22resolution%22%20%3D%20%22fixed%22%20AND%20affectedVersion%20%3D%20" +
                    version + "&fields=key,resolutiondate,versions,created,fixVersions&startAt="+i.toString()+ "&maxResults="+ j.toString();

            JSONObject json = readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");
            int k;
            for (; i < total && i < j; i++) {
                //Iterate through each bug

                String key = issues.getJSONObject(i%1000).get("key").toString();
                String versionJsonString = issues.getJSONObject(i%1000).getJSONObject("fields").get("versions").toString();
                String fixVersionsJsonString = issues.getJSONObject(i%1000).getJSONObject("fields").get("fixVersions").toString();

                JSONArray versionJsonArray = new JSONArray(versionJsonString);
                JSONArray fixVersionJsonArray = new JSONArray(fixVersionsJsonString);

                System.out.println("[" + key + "]");

                //STAMPO TUTTE LE AFFECTED VERSION PER UN ISSUE
                System.out.print("--> affected versions: ");
                len = versionJsonArray.length();
                for(k = 0; k < len; k++){
                    System.out.print(versionJsonArray.getJSONObject(k).get("name"));
                    if(k != len-1) System.out.print("; ");
                    if(k == len-1) System.out.print("\n");
                }


                //STAMPO TUTTE LE FIX VERSION PER UN ISSUE
                System.out.print("--> fix versions: ");
                fixLen = fixVersionJsonArray.length();
                for(k = 0; k < fixLen; k++){
                    String curr = fixVersionJsonArray.getJSONObject(k).get("name").toString();
                    System.out.print(curr);
                    if(!fixVersions.contains(curr))
                        fixVersions.add(curr);
                    if(k != fixLen-1) System.out.print("; ");
                    if(k == fixLen-1) System.out.print("\n");
                }
                System.out.println("");

                issuesId.add(key);
            }
        } while (i < total);

        issuesIdMap.put("issues", issuesId);
        issuesIdMap.put("fixVersions", fixVersions);

        return issuesIdMap;
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

                Issue issue = new Issue(key, fixVersions, affectedVersions, resolutionDate);
                bugInfo.add(issue);
            }
        } while (i < total);

        System.out.println("ho trovato #bug="+bugInfo.size());
        return bugInfo;
    }

}
