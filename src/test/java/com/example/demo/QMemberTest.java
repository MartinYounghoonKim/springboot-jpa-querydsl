package com.example.demo;

import com.example.demo.entity.Member;
import com.example.demo.entity.QMember;
import com.example.demo.entity.QTeam;
import com.example.demo.entity.Team;
import com.querydsl.core.QueryModifiers;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assert;
import org.assertj.core.api.Assertions;
import org.hibernate.annotations.common.reflection.XMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.example.demo.entity.QMember.member;
import static com.example.demo.entity.QTeam.team;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QMemberTest {
	@Autowired
	EntityManager entityManager;

	JPAQueryFactory queryFactory;

	@BeforeEach
	public void before () {
		queryFactory = new JPAQueryFactory(entityManager);
		Team teamA = new Team("teamA");
		Team teamB = new Team("teamB");

		entityManager.persist(teamA);
		entityManager.persist(teamB);

		Member member1 = new Member("member1", 10, teamA);
		Member member2 = new Member("member2", 20, teamA);

		Member member3 = new Member("member3", 30, teamB);
		Member member4 = new Member("member4", 40, teamB);

		entityManager.persist(member1);
		entityManager.persist(member2);
		entityManager.persist(member3);
		entityManager.persist(member4);
	}

	@Test
	public void startJPQL () {
		String qlString =
			"select m from Member m " +
				"where m.username = :username";
		Member findMember = entityManager.createQuery(qlString, Member.class)
			.setParameter("username", "member1")
			.getSingleResult();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void startQuerydsl () {
		Member findMember = queryFactory
			.select(member)
			.from(member)
			.where(member.username.eq("member1"))
			.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void search () {
		Member findMember = queryFactory
			.selectFrom(member)
			.where(member.username.eq("member1")
				.and(member.age.between(10, 30)))
			.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void searchAndParam () {
		Member findMember = queryFactory
			.selectFrom(member)
			.where(
				member.username.eq("member1"), // 쉼표로 작성하면 알아서 and 로 인식한다.
				member.age.eq(10),
				null // null 이 들어가면 알아서 무시(동적 쿼리 작성 시 유용함)
			)
			.fetchOne();

		queryFactory
			.selectFrom(member)
			.where(
				member.username.eq("member1"), // 쉼표로 작성하면 알아서 and 로 인식한다.
				member.age.eq(10),
				null // null 이 들어가면 알아서 무시(동적 쿼리 작성 시 유용함)
			)
			.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void resultFetch () {
		List<Member> fetch = queryFactory
			.selectFrom(member)
			.fetch();
		Member fetchOne = queryFactory
			.selectFrom(member)
			.fetchOne();

		Member fetchFirst = queryFactory.selectFrom(QMember.member)
			.fetchFirst();

		QueryResults<Member> results = queryFactory.selectFrom(member).fetchResults();

		results.getLimit();
		results.getTotal();
		List<Member> members = results.getResults();
	}

	@Test
	public void sort () {
		entityManager.persist(new Member(null, 100));
		entityManager.persist(new Member("member5", 100));
		entityManager.persist(new Member("member6", 100));

		List<Member> results = queryFactory
			.selectFrom(member)
			.where(member.age.eq(100))
			.orderBy(
				member.age.desc(),
				member.username.asc().nullsLast()
			)
			.fetch();

		Member member5 = results.get(0);
		Member member6 = results.get(1);
		Member memberNull = results.get(2);
		assertThat(member5.getUsername()).isEqualTo("member5");
		assertThat(member6.getUsername()).isEqualTo("member6");
		assertThat(memberNull.getUsername()).isEqualTo(null);
	}

	@Test
	public void paging1 () {
		List<Member> result = queryFactory
			.selectFrom(member)
			.orderBy(member.username.desc())
			.offset(1)
			.limit(2)
			.fetch();

		assertThat(result.size()).isEqualTo(2);
	}

	@Test
	public void paging2 () {
		QueryResults<Member> results = queryFactory
			.selectFrom(member)
			.orderBy(member.username.desc())
			.offset(1)
			.limit(2)
			.fetchResults();

		assertThat(results.getTotal()).isEqualTo(4);
		assertThat(results.getLimit()).isEqualTo(2);
		assertThat(results.getOffset()).isEqualTo(1);
		assertThat(results.getResults().size()).isEqualTo(2);
	}
	@Test
	public void paging3 () {
		QueryModifiers queryModifiers = new QueryModifiers(2L, 1L); // limit, offset
		QueryResults<Member> results1 = queryFactory.selectFrom(member)
			.orderBy(member.username.desc())
			.restrict(queryModifiers).fetchResults();



		QueryResults<Member> results = queryFactory
			.selectFrom(member)
			.orderBy(member.username.desc())
			.offset(1)
			.limit(2)
			.fetchResults();

		assertThat(results1.getTotal()).isEqualTo(4);
		assertThat(results1.getLimit()).isEqualTo(2);
		assertThat(results1.getOffset()).isEqualTo(1);
		assertThat(results1.getResults().size()).isEqualTo(2);
	}

	@Test
	public void aggregation () {
		List<Tuple> list = queryFactory
			.select(
				member.count(),
				member.age.sum(),
				member.age.avg(), // 평균
				member.age.max(),
				member.age.min()
			)
			.from(member)
			.fetch();
		Tuple tuple = list.get(0); // 하나의 컬럼을 tuple이라 부르기도 함
		assertThat(tuple.get(member.count())).isEqualTo(4);
		assertThat(tuple.get(member.age.sum())).isEqualTo(100);
	}

	@Test
	public void group () {
		List<Tuple> result = queryFactory
			.select(team.name, member.age.avg())
			.from(member)
			.join(member.team, team)
			.groupBy(team.name)
			.fetch();
		Tuple teamA = result.get(0);
		Tuple teamB = result.get(1);

		assertThat(teamA.get(team.name)).isEqualTo("teamA");
		assertThat(teamA.get(member.age.avg())).isEqualTo(15);

		assertThat(teamB.get(team.name)).isEqualTo("teamB");
		assertThat(teamB.get(member.age.avg())).isEqualTo(35);
	}

	@Test
	public void join () {
		List<Member> result = queryFactory
			.selectFrom(member)
			.join(member.team, team)
			.where(team.name.eq("teamA"))
			.fetch();
		assertThat(result)
			.extracting("username")
			.containsExactly("member1", "member2");
	}

	@Test
	public void theta_join () {
		entityManager.persist(new Member("teamA"));
		entityManager.persist(new Member("teamB"));
		entityManager.persist(new Member("teamC"));

		List<Member> result = queryFactory
			.select(member)
			.from(member, team)
			.where(member.username.eq(team.name))
			.fetch();

		assertThat(result)
			.extracting("username")
			.containsExactly("teamA", "teamB");
	}

	@Test
	public void join_on_filtering () {
		List<Tuple> result = queryFactory
			.select(member, team)
			.from(member)
			.join(member.team, team)
			.on(team.name.eq("teamA"))
			.where(team.name.eq("teamA"))
			.fetch();
		for (Tuple tuple: result) {
			System.out.println(tuple);
		}
	}

	@Test
	public void join_on_no_relation () {
		entityManager.persist(new Member("teamA"));
		entityManager.persist(new Member("teamB"));
		entityManager.persist(new Member("teamC"));

		List<Tuple> result = queryFactory
			.select(member, team)
			.from(member)
			.join(team)
			.on(member.username.eq(team.name))
			.fetch();
		for (Tuple tuple : result) {
			System.out.println("tuple = " + tuple);
		}
	}

	@PersistenceUnit
	EntityManagerFactory entityManagerFactory;

	@Test
	public void fetchJoinNo () {
		entityManager.flush();
		entityManager.clear();

		Member findMember = queryFactory
			.selectFrom(member)
			.where(member.username.eq("member1"))
			.fetchOne();
		boolean isLoaded = entityManagerFactory.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		assertThat(isLoaded).as("패치 조인 미적용").isFalse();
	}

	@Test
	public void fetchJoinUse () {
		entityManager.flush();
		entityManager.clear();

		Member findMember = queryFactory
			.selectFrom(member)
			.join(member.team, team).fetchJoin()
			.where(member.username.eq("member1"))
			.fetchOne();
		boolean isLoaded = entityManagerFactory.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		assertThat(isLoaded).as("패치 조인 미적용").isTrue();
	}
}
