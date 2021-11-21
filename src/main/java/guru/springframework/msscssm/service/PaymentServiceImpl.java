package guru.springframework.msscssm.service;

import guru.springframework.msscssm.domain.Payment;
import guru.springframework.msscssm.domain.PaymentEvent;
import guru.springframework.msscssm.domain.PaymentState;
import guru.springframework.msscssm.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final StateMachineFactory<PaymentState,PaymentEvent> stateMachineFactory;
    public static final String PAYMENT_HEADER="payment_id";
    private final PaymentStateChangeInterceptor paymentStateChangeInterceptor;

    @Override
    @Transactional
    public Payment newPayment(Payment payment) {
        payment.setState(PaymentState.NEW);
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public StateMachine<PaymentState, PaymentEvent> preAuth(long paymentId) {

        //Use statemachine to rehidrate from database current state
        StateMachine<PaymentState,PaymentEvent> sm=build(paymentId);
        System.out.println("return from build");
        System.out.println("Sending event");
        sendEvent(paymentId,sm,PaymentEvent.PRE_AUTH_APPROVED);
        System.out.println("Event sent");
        System.out.println("sm state:"+ sm.getState().getId());
        return sm;
    }

    @Override
    @Transactional
    public StateMachine<PaymentState, PaymentEvent> authorizePayment(long paymentId) {
        //Use statemachine to rehidrate from database current state
        StateMachine<PaymentState,PaymentEvent> sm=build(paymentId);
        sendEvent(paymentId,sm,PaymentEvent.AUTH_APPROVED);
        return sm;
    }

    @Override
    @Transactional
    public StateMachine<PaymentState, PaymentEvent> declineAuth(long paymentId) {
        //Use statemachine to rehidrate from database current state
        StateMachine<PaymentState,PaymentEvent> sm=build(paymentId);
        //send event
        sendEvent(paymentId,sm,PaymentEvent.AUTH_DECLINED);
        return sm;
    }

    //use case when authroize amount and use it has time inbetween


    private StateMachine<PaymentState,PaymentEvent> build(Long paymentId)
    {
        //get the paymnet from id from DB
        Payment payment = paymentRepository.getById(paymentId);

        StateMachine<PaymentState,PaymentEvent> sm= stateMachineFactory
                .getStateMachine(Long.toString(payment.getId()));

        sm.stop();
        sm.getStateMachineAccessor()
                .doWithAllRegions(sma->{
                    sma.addStateMachineInterceptor(paymentStateChangeInterceptor);
                    sma.resetStateMachine(new DefaultStateMachineContext<>(payment.getState(),null,null,null));


                });
        sm.start();
        return sm;
    }

    private void sendEvent(Long paymentId, StateMachine<PaymentState,PaymentEvent> sm, PaymentEvent event)
    {
        System.out.println("Inside Send event");
        System.out.println("from Event:"+sm.getState().getId());
        Message message= MessageBuilder.withPayload(event)
                .setHeader(PAYMENT_HEADER,paymentId)
                .build();
        System.out.println("message is :"+ message.toString());
       // boolean b = sm.sendEvent(message);
       // System.out.println(b);
        sm.sendEvent(event);
    }
}
