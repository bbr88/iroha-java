package jp.co.soramitsu.iroha.java.health_check

import jp.co.soramitsu.iroha.testcontainers.IrohaContainer
import jp.co.soramitsu.iroha.testcontainers.PeerConfig
import jp.co.soramitsu.iroha.testcontainers.detail.IrohaConfig
import spock.lang.Retry
import spock.lang.Specification

class HealthCheckTest extends Specification {

    // Change the container's version to 1.4 as soon as it's released
    static def irohaDockerImage = "hyperledger/iroha:1.4.0-rc.2"
    static def irohaWithDefaultHealthCheckPort = new IrohaContainer()
            .withIrohaDockerImage(irohaDockerImage)

    static def customHealthCheckPort = HealthCheckClient.DEFAULT_HEALTH_CHECK_PORT + 1
    static def irohaWithCustomHealthCheckPort = new IrohaContainer()
            .withPeerConfig(
                    PeerConfig.builder()
                            .irohaConfig(
                                    IrohaConfig.builder()
                                            .healthcheck_port(customHealthCheckPort)
                                            .build())
                            .build())
            .withIrohaDockerImage(irohaDockerImage)

    def setupSpec() {
        irohaWithDefaultHealthCheckPort.start()
        irohaWithCustomHealthCheckPort.start()
    }

    def cleanupSpec() {
        irohaWithDefaultHealthCheckPort.stop()
        irohaWithCustomHealthCheckPort.stop()
    }

    @Retry(delay = 3000)
    def "health check of a healthy node with a default health check port"() {
        given:
        final def healthCheckClient = new HealthCheckClient(
                irohaWithDefaultHealthCheckPort.getIrohaDockerContainer().getContainerIpAddress(),
                irohaWithDefaultHealthCheckPort.getHealthCheckPort())

        when: "call the health check endpoint of a healthy node"
        final def response = healthCheckClient.check()

        then:
        !response.syncing
        response.status
    }

    @Retry(delay = 3000)
    def "health check of a healthy node with a custom health check port"() {
        given:
        final def healthCheckClient = new HealthCheckClient(
                irohaWithCustomHealthCheckPort.getIrohaDockerContainer().getContainerIpAddress(),
                irohaWithDefaultHealthCheckPort.getHealthCheckPort())

        when: "call the health check endpoint of a healthy node"
        final def response = healthCheckClient.check()

        then:
        !response.syncing
        response.status
    }

    def "health check of an unhealthy node"() {
        given:
        final def healthCheckClient = new HealthCheckClient(
                irohaWithDefaultHealthCheckPort.getIrohaDockerContainer().getContainerIpAddress())

        when: "call the health check endpoint of an unhealthy node"
        final def response = healthCheckClient.check()

        then:
        response.status == HealthResponse.unhealthy().status
    }

}
