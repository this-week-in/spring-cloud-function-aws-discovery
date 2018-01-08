package org.springframework.cloud.function.discovery.aws

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.apigateway.AmazonApiGateway
import com.amazonaws.services.apigateway.AmazonApiGatewayClientBuilder
import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.AWSLambdaClientBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

/**
 * Configures AWS connectivity and the {@link LambdaDiscoveryClient}.
 *
 * @author <a href="mailto:josh@JoshLong.com">Josh Long</a>
 */
@Configuration
class AwsAutoConfiguration(private val env: Environment) {

	private val region = Regions.valueOf(
			env.getProperty("cloud.aws.region", Regions.US_EAST_1.name))

	@Bean
	@ConditionalOnMissingBean
	fun region() = this.region

	@Bean
	@ConditionalOnMissingBean
	fun awsLambda(): AWSLambda = AWSLambdaClientBuilder.standard()
			.withCredentials(awsCredentialsProvider())
			.withRegion(region)
			.build()

	@Bean
	@ConditionalOnMissingBean
	fun awsCredentialsProvider() = AWSStaticCredentialsProvider(
			BasicAWSCredentials(env.getProperty("cloud.aws.credentials.accessKey", System.getenv("AWS_ACCESS_KEY_ID")),
					env.getProperty("cloud.aws.credentials.secretKey", System.getenv("AWS_SECRET_ACCESS_KEY"))))

	@Bean
	@ConditionalOnMissingBean
	fun amazonApiGateway(): AmazonApiGateway = AmazonApiGatewayClientBuilder.standard()
			.withRegion(region)
			.withCredentials(awsCredentialsProvider())
			.build()

	@Bean
	@ConditionalOnMissingBean
	fun lambdaDiscoveryClient(): DiscoveryClient = LambdaDiscoveryClient(region(), amazonApiGateway(), awsLambda())
}