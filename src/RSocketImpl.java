package prototype.async.server;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.util.DefaultPayload;
import org.reactivestreams.Publisher;
import prototype.model.Coordinate;
import prototype.utility.Serializer;
import reactor.core.publisher.Flux;
import java.time.Duration;

public class RSocketImpl extends AbstractRSocket {
	@Override
	public Flux<Payload> requestChannel(final Publisher<Payload> payloads) {
	    return Flux.from(payloads)
                .flatMapSequential(
                        payload -> Flux.just(payload).delaySequence(Duration.ofMillis(10)));
	}
}
