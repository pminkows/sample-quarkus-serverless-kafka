package pl.piomin.samples.quarkus.serverless.customer.function;

import io.quarkus.funqy.Funq;
import org.jboss.logging.Logger;
import pl.piomin.samples.quarkus.serverless.customer.client.OrderSender;
import pl.piomin.samples.quarkus.serverless.customer.exception.NotFoundException;
import pl.piomin.samples.quarkus.serverless.customer.message.Order;
import pl.piomin.samples.quarkus.serverless.customer.message.OrderStatus;
import pl.piomin.samples.quarkus.serverless.customer.model.Customer;
import pl.piomin.samples.quarkus.serverless.customer.repository.CustomerRepository;

import javax.inject.Inject;

public class OrderReserveFunction {

    private static final String SOURCE = "payment";

    @Inject
    Logger log;
    @Inject
    CustomerRepository repository;
    @Inject
    OrderSender sender;

    @Funq
    public void reserve(Order order) {
        log.infof("Received order: %s", order);
        if (order.getStatus() == OrderStatus.NEW)
            doReserve(order);
        else
            doConfirm(order);
    }

    private void doReserve(Order order) {
        Customer c = repository.findById(order.getCustomerId());
        if (c == null)
            throw new NotFoundException();
        log.infof("Customer: %s", c);
        if (order.getAmount() < c.getAmountAvailable()) {
            order.setStatus(OrderStatus.IN_PROGRESS);
            c.setAmountReserved(c.getAmountReserved() + order.getAmount());
            c.setAmountAvailable(c.getAmountAvailable() - order.getAmount());
            repository.persist(c);
        } else {
            order.setStatus(OrderStatus.REJECTED);
        }
        order.setSource(SOURCE);
        log.infof("Order reserved: %s", order);
        sender.send(order);
    }

    private void doConfirm(Order order) {
        Customer c = repository.findById(order.getCustomerId());
        if (c == null)
            throw new NotFoundException();
        log.infof("Customer found: %s", c);
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            c.setAmountReserved(c.getAmountReserved() - order.getAmount());
            repository.persist(c);
        } else if (order.getStatus() == OrderStatus.ROLLBACK && !order.getSource().equals(SOURCE)) {
            c.setAmountAvailable(c.getAmountAvailable() - order.getAmount());
            c.setAmountReserved(c.getAmountReserved() + order.getAmount());
            repository.persist(c);
        }
    }

}
