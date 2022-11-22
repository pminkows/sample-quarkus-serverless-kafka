package pl.piomin.samples;

import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import pl.piomin.samples.model.Order;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class SimpleListener {

    @Inject
    Logger log;

    @Incoming("order-events")
    @Acknowledgment(Acknowledgment.Strategy.POST_PROCESSING)
    public void onEvent(Order order) {
        log.infof("In: %s", order);
    }
}
