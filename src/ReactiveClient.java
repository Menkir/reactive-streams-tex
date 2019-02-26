package prototype.async.client;

import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.DefaultPayload;
import prototype.utility.Serializer;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import prototype.routing.RoutingFactory;
import java.net.InetSocketAddress;
import java.time.Duration;

public class Car {
	private final InetSocketAddress socketAddress;
	private final RoutingFactory routingFactory = new RoutingFactory();
    private RSocket client;
    private Disposable serverEndpoint;
    private final CarConfiguration carConfiguration;

	/**
	 * The Flowrate define how many Requests are processed successfully.
	 */
	private int flowrate = 0;

	/**
	 * Initialize Car Instance with default CarConfiguration
	 * @param socketAddress Contains the IP and Port for corresponding Server
	 */
    public Car(InetSocketAddress socketAddress) {
		this.carConfiguration = new CarConfiguration();
		this.socketAddress =  socketAddress;
    }

	/**
	 *
	 * @param socketAddress Contains the IP and Port for corresponding Server
	 * @param configuration The Custom Configuration for a Car Instance
	 */
	public Car(InetSocketAddress socketAddress, CarConfiguration configuration) {
	    this.socketAddress = socketAddress;
	    this.carConfiguration = configuration;
    }

	/**
	 * This Method connect to the reactive Server.
	 * The keep-alive is set to 30 Minutes in case the Connection stays longer (Benchmark).
	 * The transport protocol is TCP, the connection safety is guaranteed.
	 * Because each connection is only once, the return value of start(...) can be get in a blocking manner.
	 */
	public void connect() {
		this.client = RSocketFactory
				.connect()
				.keepAliveAckTimeout(Duration.ofMinutes(30))
				.transport(TcpClientTransport.create(socketAddress.getHostName(), socketAddress.getPort()))
				.start()
				.block();
	}

	/**
	 *
	 * @return Integer of current Flowrate
	 */
	public int getFlowrate() {
		return flowrate;
	}

	/**
	 * This Method request a Channel between Server and Client by giving a Stream as an Argument. The Stream is the datasource.
	 * The Flux (Stream) emit elements from Iterable of a List of Measurements (Coordinate and Signal Strength). It will be repeated about 100.000 times
	 * to simulate an endless emission. Each Measure Signal is delayed by particular delay in ms. Each emitted Measure is mapped to a Payload Type from RSocket.
	 * Because this datasource is able to be subscribed multiple times the Stream is hot by calling the share() function.
	 */
    public void send() {
        serverEndpoint = client.requestChannel(
                Flux.fromIterable(routingFactory.getRoutingType(carConfiguration.ROUTETYPE).getRouteAsList())
                        .repeat(100_000)
                        .delayElements(carConfiguration.DELAY)
                        .doOnNext(measurements -> measurements.setSignalStrength(((int) (Math.random() * 10))))
                        .map(measurements -> DefaultPayload.create(Serializer.serialize(measurements)))
                        .share()
        ).subscribe(payload -> ++flowrate);
    }

	/**
	 * This Method disposes from the Response Stream from the Server.
	 */
	public void close() {
        serverEndpoint.dispose();
    }
}
