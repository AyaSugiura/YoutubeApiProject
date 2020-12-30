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
		}).setApplicationName("youtube-api").build();
		
		YouTube.Search.List search = youtube.search().list("id, snippet");
		search.setKey(API_KEY);
		
		// 日付を形式変換
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime sixMonthsAgo = now.minusMonths(6);
		DateTimeFormatter formatter = DateTimeFormatter
			.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
			.withZone(ZoneId.of("UTC"));
		String timeStr = formatter.format(sixMonthsAgo);
		DateTime dateTime = DateTime.parseRfc3339(timeStr);
		
		search.setQ("エンジニア");
		search.setType("video");
		search.setOrder("viewCount");
		search.setPublishedAfter(dateTime);
		search.setMaxResults((long) 10);
		search.setFields("items");
		
		SearchListResponse searchResponse = search.execute();
		List<SearchResult> searchResultList = searchResponse.getItems();
		
		List<Map<String, Object>> searchResultMaps = new ArrayList();
		for (SearchResult searchResult : searchResultList) {
			
			// すでに取得済みの値をセット
			Map<String, Object> searchResultMap = new HashMap();
			searchResultMap.put("title", searchResult.getSnippet().getTitle());
			searchResultMap.put("channelTitle", searchResult.getSnippet().getChannelTitle());
			searchResultMap.put("publishedAt", searchResult.getSnippet().getPublishedAt());
			
			String videoId = searchResult.getId().getVideoId();
			String channelId = searchResult.getSnippet().getChannelId();
			
			// 動画の再生回数を取得してセット
			YouTube.Videos.List video = youtube.videos().list("statistics, contentDetails");
			video.setKey(API_KEY);
			video.setId(videoId);
			VideoListResponse videoListResponse = video.execute();
			
			List<Video> videoResultList = videoListResponse.getItems();
			searchResultMap.put("videoViewCount", videoResultList.get(0).getStatistics().getViewCount());
			
			// チャンネル登録者数を取得してセット
			YouTube.Channels.List channels = youtube.channels().list("statistics, contentDetails");
			channels.setKey(API_KEY);
			channels.setId(channelId);
			ChannelListResponse channelListResponse = channels.execute();
			
			List<Channel> channelResultList = channelListResponse.getItems();
			searchResultMap.put("subscriberCount", channelResultList.get(0).getStatistics().getSubscriberCount());
			
			searchResultMaps.add(searchResultMap);
		}
		
		model.addAttribute("searchResultMaps", searchResultMaps);
		
		return "test/index";
		
	}
}
