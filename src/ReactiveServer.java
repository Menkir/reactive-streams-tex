package prototype.async.server;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.server.TcpServerTransport;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Scanner;

public class CarServer {
	private InetSocketAddress socketAddress;
	private Disposable channel;

	/**
	 * Inintialize Host Information like IP and Port.
	 * @param socketAddress Contains Information about IP and Port.
	 */
	private CarServer(InetSocketAddress socketAddress){
		this.socketAddress = socketAddress;
	}

	/**
	 * Instantiate Server Socket in channel Variable. The receive Method returns a ServerRSocketFactory whcih is configured down below.
	 * The acceptor takes a Lambda for a SocketAcceptor Class which returns an AbstractSocket Instance. The AbstractSocket Instance override's one of the four Interaction Models for RSocket: channel.
	 * The requestChannel takes a Publisher, transforms it to a Flux, configure a delay for each Measurement and return it to the Caller respective Car Instance.
	 * The transport Methode configures the Protocol TCP, because it is a more stable than UDP.
	 * The start Method returns a Mono &lt;Disposable&gt; of a successful established hosted Server in a blocking manner because we only have one Server.
	 */
	public void receive() {
		channel = RSocketFactory.receive()
				.acceptor((setupPayload, reactiveSocket) ->
						Mono.just(new AbstractRSocket() {
							@Override
							public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
								return Flux.from(payloads)
										.flatMapSequential(
												payload -> Flux.just(payload).delaySequence(Duration.ofMillis(10)));
							}
						}))
				.transport(TcpServerTransport.create(socketAddress.getHostName(), socketAddress.getPort()))
				.start()
				.block();
	}

	/**
	 * Dispose from incoming Connections.
	 */
	public void close(){
		channel.dispose();
	}

	public static void main(final String... args){
		Scanner sc = new Scanner(System.in);
		CarServer carServer = new CarServer(new InetSocketAddress("127.0.0.1", 1337));
	    carServer.receive();

		System.out.println("Type 'close' to terminate the CarServer:");
		while(true){
			String input = sc.nextLine();
			switch(input){
				case "close": carServer.close();
					return;
				default:
					System.err.println("Try again...");
			}
		}

    }
}
