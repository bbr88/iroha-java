package jp.co.soramitsu.iroha.java.health_check

import jp.co.soramitsu.iroha.testcontainers.IrohaContainer
import jp.co.soramitsu.iroha.testcontainers.PeerConfig
import jp.co.soramitsu.iroha.testcontainers.detail.IrohaConfig
import jp.co.soramitsu.iroha.testcontainers.network.IrohaNetwork
import spock.lang.Specification

class HealthCheckTest extends Specification {

    // Change the container's version to 1.4 as soon as it's released
    static final IROHA_DOCKER_IMAGE = "hyperledger/iroha:1.4.0-rc.2"
    static final CUSTOM_HEALTH_CHECK_PORT = HealthCheckClient.DEFAULT_HEALTH_CHECK_PORT + 1

    static def irohaWithDefaultHealthCheckPort = new IrohaContainer()
            .withIrohaDockerImage(IROHA_DOCKER_IMAGE)

    static def irohaWithCustomHealthCheckPort = new IrohaContainer()
            .withPeerConfig(
                    PeerConfig.builder()
                            .irohaConfig(
                                    IrohaConfig.builder()
                                            .healthcheck_port(CUSTOM_HEALTH_CHECK_PORT)
                                            .build())
                            .build())
            .withIrohaDockerImage(IROHA_DOCKER_IMAGE)

    static def irohaNetwork = new IrohaNetwork(3, IROHA_DOCKER_IMAGE)

    def setupSpec() {
        irohaWithDefaultHealthCheckPort.start()
        irohaWithCustomHealthCheckPort.start()
        irohaNetwork.start()

        sleep(5000) // It takes a few seconds for a node to become healthy
    }

    def cleanupSpec() {
        irohaWithDefaultHealthCheckPort.stop()
        irohaWithCustomHealthCheckPort.stop()
        irohaNetwork.stop()
    }

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

    def "health check of a healthy Iroha network"() {
        given:
        final def healthCheckResponses = new ArrayList<HealthResponse>(irohaNetwork.peers.size());
        healthCheckResponses.isEmpty()

        and: "the network is running"
        irohaNetwork.getPeers()
                .stream()
                .map({ p -> p.getContainer() })
                .allMatch({ c ->
                    final def ir = c.getIrohaDockerContainer()

                    return ir.isRunning()
                })

        when: "call the health check endpoint of each network's node"
        irohaNetwork.getPeers().each {
            final def healthCheckClient = new HealthCheckClient(
                    it.container.getIrohaDockerContainer().getContainerIpAddress(),
                    it.container.getHealthCheckPort())

            final def response = healthCheckClient.check()
            healthCheckResponses.add(response)
        }

        then:
        healthCheckResponses.size() == irohaNetwork.peers.size()
        healthCheckResponses.each {
            assert !it.syncing
            assert it.status
        }
    }

}
