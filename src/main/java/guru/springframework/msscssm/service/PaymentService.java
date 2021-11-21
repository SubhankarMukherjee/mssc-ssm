package guru.springframework.msscssm.service;

import guru.springframework.msscssm.domain.Payment;
import guru.springframework.msscssm.domain.PaymentEvent;
import guru.springframework.msscssm.domain.PaymentState;
import org.springframework.statemachine.StateMachine;

public interface PaymentService {

    Payment newPayment(Payment payment);
    StateMachine<PaymentState, PaymentEvent> preAuth(long paymentId);
    StateMachine<PaymentState, PaymentEvent> authorizePayment(long paymentId);
    StateMachine<PaymentState, PaymentEvent> declineAuth(long paymentId);
}
