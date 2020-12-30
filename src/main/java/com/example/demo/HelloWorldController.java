package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/")
public class HelloWorldController {

	@RequestMapping(method=RequestMethod.GET)
	public String display(Model model) {
		
		String message = "チャンネル登録よろしくお願いします！";
		model.addAttribute("message", message);
		return "/helloWorld/index";
	}
}
