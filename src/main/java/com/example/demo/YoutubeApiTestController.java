package com.example.demo;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

@Controller
@RequestMapping("/test")
public class YoutubeApiTestController {
	
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

	private static final JsonFactory JSON_FACTORY = new JacksonFactory();
	
	private static final String API_KEY = "AIzaSyCQZDMrQ9cWy_JdS1vX3DDoii2vBWIVckQ";

	@RequestMapping(value="/", method=RequestMethod.GET)
	public String display(Model model) throws IOException {
		
		YouTube youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpRequestInitializer() {
		    public void initialize(HttpRequest request) throws IOException {
		    }
		}).setApplicationName("youtube-cmdline-search-sample").build();
		
		YouTube.Search.List search = youtube.search().list("id,snippet");
		
		search.setKey(API_KEY);
		
		// 日付を形式変換
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime sixMonthsAgo = now.minusMonths(6);
		DateTimeFormatter formatter = DateTimeFormatter
	            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
	            .withZone(ZoneId.of("UTC"));
		String timeStr = formatter.format(sixMonthsAgo);
		DateTime dateTime = DateTime.parseRfc3339(timeStr);
		
		// 検索する条件をセット
		search.setQ("エンジニア");
		search.setType("video");
		search.setOrder("viewCount");
		search.setPublishedAfter(dateTime);
		search.setMaxResults((long) 10);
		
		// 取得する値を指定する
		search.setFields("items");
		
		SearchListResponse searchResponse = search.execute();
		
		List<SearchResult> searchResultList = searchResponse.getItems();
		List<Map<String, Object>> searchResultMaps = new ArrayList();
		for(int index=0; index < searchResultList.size(); index++) {
			SearchResult searchResult = searchResultList.get(index);
			Map<String, Object> searchResultMap = new HashMap();
			
			try {
				// すでに取得した値を格納
				searchResultMap.put("channelId", searchResult.getSnippet().getChannelId());
				searchResultMap.put("videoId", searchResult.getId().getVideoId());
				searchResultMap.put("title", searchResult.getSnippet().getTitle());
				searchResultMap.put("publishTime", searchResult.getSnippet().getPublishedAt());
				searchResultMap.put("channelTitle", searchResult.getSnippet().getChannelTitle());
				
				// 動画の再生回数を取得して格納
				YouTube.Videos.List video = youtube.videos().list("statistics,contentDetails");
				video.setKey(API_KEY);
				video.setId(searchResult.getId().getVideoId().toString());
				VideoListResponse videoResponse = video.execute();
				
				List<Video> videoResultList = videoResponse.getItems();
				searchResultMap.put("viewCount", videoResultList.get(0).getStatistics().getViewCount());
				
				// チャンネルの情報を取得して格納
				YouTube.Channels.List channels = youtube.channels().list("statistics,contentDetails");
				channels.setKey(API_KEY);
				channels.setId(searchResult.getSnippet().getChannelId());
				ChannelListResponse channelResponse = channels.execute();
				
				List<Channel> channelResultList = channelResponse.getItems();
				searchResultMap.put("subscriberCount", channelResultList.get(0).getStatistics().getSubscriberCount());
				
				searchResultMaps.add(searchResultMap);
			} catch(Exception e) {
				System.out.println(searchResult);
				e.printStackTrace();
			}
		
		}
		model.addAttribute("searchResultMaps", searchResultMaps);
		
		return "test/index";
		
	}
}
