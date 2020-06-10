package com.example.demo.controller;

import com.example.demo.entity.HelloEntity;
import com.example.demo.entity.QHelloEntity;
import com.example.demo.service.HelloService;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;

@RestController
@RequiredArgsConstructor
public class HelloController {
	@Autowired
	EntityManager em;
	private final HelloService helloService;

	@GetMapping
	public String hello () {
		helloService.hello();
		return "Hello";
	}
}
