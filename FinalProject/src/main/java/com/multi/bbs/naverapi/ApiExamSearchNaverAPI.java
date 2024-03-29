package com.multi.bbs.naverapi;

// 네이버 검색 API 예제 - 블로그 검색
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.parser.ParseException;



public class ApiExamSearchNaverAPI {

	 private static final String CLIENT_ID = "DqVJC04hOajaoktTIeYA"; //애플리케이션 클라이언트 아이디
	    private static final String CLIENT_SECRET = "0MvJnDIbBm"; //애플리케이션 클라이언트 시크릿

	    public static String searchFirstImageUrl(String query) throws IOException, ParseException {
	        String apiURL = "https://openapi.naver.com/v1/search/image"; // 이미지 검색 API URL
	        String display = "1"; // 첫 번째 결과만 반환
	        
	        query = URLEncoder.encode(query, "UTF-8");
	        String url = apiURL + "?query=" + query + "&display=" + display;

	        Map<String, String> requestHeaders = new HashMap<>();
	        requestHeaders.put("X-Naver-Client-Id", CLIENT_ID);
	        requestHeaders.put("X-Naver-Client-Secret", CLIENT_SECRET);

	        String responseBody = get(url, requestHeaders);
	        // 첫 번째 이미지 URL 추출
	        String firstImageUrl = NaverApiParser.parseFirstImageUrl(responseBody);
	        return firstImageUrl;
	    }
  

    private static String get(String apiUrl, Map<String, String> requestHeaders){
        HttpURLConnection con = connect(apiUrl);
        try {
            con.setRequestMethod("GET");
            for(Map.Entry<String, String> header :requestHeaders.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }
            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // 정상 호출
                return readBody(con.getInputStream());
            } else { // 오류 발생
                return readBody(con.getErrorStream());
            }
        } catch (IOException e) {
            throw new RuntimeException("API 요청과 응답 실패", e);
        } finally {
            con.disconnect();
        }
    }


    private static HttpURLConnection connect(String apiUrl){
        try {
            URL url = new URL(apiUrl);
            return (HttpURLConnection)url.openConnection();
        } catch (MalformedURLException e) {
            throw new RuntimeException("API URL이 잘못되었습니다. : " + apiUrl, e);
        } catch (IOException e) {
            throw new RuntimeException("연결이 실패했습니다. : " + apiUrl, e);
        }
    }

    private static String readBody(InputStream body){
        InputStreamReader streamReader = new InputStreamReader(body);
        try (BufferedReader lineReader = new BufferedReader(streamReader)) {
            StringBuilder responseBody = new StringBuilder();
            String line;
            while ((line = lineReader.readLine()) != null) {
                responseBody.append(line);
            }
            return responseBody.toString();
        } catch (IOException e) {
            throw new RuntimeException("API 응답을 읽는 데 실패했습니다.", e);
        }
    }
}