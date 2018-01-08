package org.springframework.cloud.function.discovery.aws

import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.test.context.junit4.SpringRunner

@SpringBootTest
@RunWith(SpringRunner::class)
class LambdaDiscoveryClientTest {

	@SpringBootApplication
	class MyApp

	@Autowired
	val ldc:  DiscoveryClient? = null

	@Test
	fun getServices() {
		Assertions.assertThat(this.ldc).isNotNull()
		ldc!!.services.forEach {
			println("the service ${it} is available.")
		}
	}

	@Test
	fun getInstances() {
		Assertions.assertThat(this.ldc).isNotNull()
		val dc = ldc!!
		dc.getInstances("uppercase").forEach {
			println("found: ${it.uri}")
		}
	}

	@Test
	fun description() {
		Assertions.assertThat(this.ldc).isNotNull()
		println(ldc!!.description())
	}
}