package org.springframework.cloud.function.discovery.aws

import com.amazonaws.regions.Regions
import com.amazonaws.services.apigateway.AmazonApiGateway
import com.amazonaws.services.apigateway.model.*
import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.model.GetFunctionRequest
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties
import org.springframework.util.Assert
import java.net.URI
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * A {@link DiscoveryClient} implementation that provides URLs for
 * functions that have been registered in AWS Lambda and then exposed
 * through an AWS API Gateway trigger. This implementation returns the URL
 * for the AWS API Gateway.
 *
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 *
 */
open class LambdaDiscoveryClient(private val region: Regions,
                                 private val amazonApiGateway: AmazonApiGateway,
                                 private val lambda: AWSLambda) : DiscoveryClient {


	/**
	 * Returns a list of the logical names for AWS Lambda functions
	 */
	override fun getServices(): MutableList<String> =
			lambda
					.listFunctions()
					.functions
					.map { it.functionName }
					.toMutableList()

	/**
	 * For any given service {@code foo} there's only one addressable URL in
	 * AWS (behind the gateway), so this returns that single URL.
	 */
	override fun getInstances(serviceId: String): List<ServiceInstance> {
		val split = serviceId.split(':')
		val serviceName = split[0]
		val verbs: Array<String> = if (split.size > 1) {
			val lastPart: String = split[split.size - 1]
			if (lastPart.contains(',')) lastPart.split(',').toTypedArray() else arrayOf(lastPart)
		} else {
			arrayOf("GET", "POST", "DELETE", "OPTIONS", "ANY", "PUT")
		}
		val uri = URI.create(urlByFunctionName(serviceName, methods = verbs))
		return arrayListOf(SimpleDiscoveryProperties.SimpleServiceInstance(uri) as ServiceInstance)
	}


	override fun description(): String = ("A discovery client that returns URIs " +
			"for AWS Lambda functions mapped to API Gateway endpoints")
			.trim()

	/**
	 * Finds a function by its logical name, then finds any REST APIs
	 * that have an integration with that function.
	 */
	private fun urlByFunctionName(functionName: String, methods: Array<String> = arrayOf("GET", "POST")): String? {

		data class PathContext(val resource: Resource,
		                       val integrationResult: GetIntegrationResult,
		                       val restApi: RestApi)

		val fnArn = lambda.getFunction(GetFunctionRequest()
				.withFunctionName(functionName))
				.configuration
				.functionArn

		return amazonApiGateway
				.getRestApis(GetRestApisRequest())
				.items
				.flatMap { restApi ->

					val pathContexts: List<PathContext> = amazonApiGateway
							.getResources(GetResourcesRequest().withRestApiId(restApi.id))
							.items
							.flatMap { resource ->

								fun forMethod(method: String): PathContext? {
									val integration: GetIntegrationResult? =
											try {
												val integrationRequest = GetIntegrationRequest()
														.withHttpMethod(method)
														.withRestApiId(restApi.id)
														.withResourceId(resource.id)

												amazonApiGateway.getIntegration(integrationRequest)
											} catch (e: Exception) {
												null
											}

									return if (null != integration) PathContext(resource, integration, restApi) else null
								}

								methods.flatMap {
									val pc = forMethod(it)
									if (pc == null) emptyList<PathContext>() else arrayListOf(pc)
								}
							}
					pathContexts
				}
				.map { ctx ->
					if (ctx.integrationResult.uri.contains(fnArn)) {
						"https://${ctx.restApi.id}.execute-api.${region.getName()}.amazonaws.com/prod/${ctx.resource.pathPart}"
					} else
						null
				}
				.filter { it != null }
				.toSet()
				.first { it != null }
	}
}