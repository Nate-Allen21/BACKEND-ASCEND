package br.com.ascend.the_system;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TheSystemApplicationTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoads() {
	}

	@Test
	void usesADeploySafeConnectionPool() {
		assertThat(dataSource).isInstanceOf(HikariDataSource.class);
		HikariDataSource hikari = (HikariDataSource) dataSource;
		assertThat(hikari.getMaximumPoolSize()).isEqualTo(3);
		assertThat(hikari.getMinimumIdle()).isZero();
	}

}
