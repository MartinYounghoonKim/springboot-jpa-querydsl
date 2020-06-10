package com.example.demo.service;

import com.example.demo.entity.HelloEntity;
import com.example.demo.entity.QHelloEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

@Service
@RequiredArgsConstructor
public class HelloService {
	@Autowired
	EntityManager em;

	@Transactional
	public void hello () {
		HelloEntity helloEntity = new HelloEntity();
		em.persist(helloEntity);

		JPAQueryFactory queryFactory =  new JPAQueryFactory(em);
		QHelloEntity qHelloEntity = QHelloEntity.helloEntity;

		HelloEntity result = queryFactory
			.selectFrom(qHelloEntity)
			.fetchOne();

		System.out.println(result);
	}
}
