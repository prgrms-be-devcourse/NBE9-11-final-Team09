package com.back.team9.moyeota.domain.payment.service;

import com.back.team9.moyeota.domain.payment.client.TossConfirmResponse;
import com.back.team9.moyeota.domain.payment.client.TossPaymentClient;
import com.back.team9.moyeota.domain.payment.dto.PaymentConfirmRequest;
import com.back.team9.moyeota.domain.payment.dto.PaymentResponse;
import com.back.team9.moyeota.domain.payment.entity.Payment;
import com.back.team9.moyeota.domain.payment.entity.PaymentStatus;
import com.back.team9.moyeota.domain.payment.entity.PaymentType;
import com.back.team9.moyeota.domain.payment.repository.PaymentRepository;
import com.back.team9.moyeota.global.error.ErrorCode;
import com.back.team9.moyeota.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final PaymentWriter paymentWriter;

    public PaymentResponse confirmDeposit(PaymentConfirmRequest request) {
        return confirmPayment(request, PaymentType.DEPOSIT);
    }

    public PaymentResponse confirmBalance(PaymentConfirmRequest request) {
        return confirmPayment(request, PaymentType.BALANCE);
    }

    private PaymentResponse confirmPayment(PaymentConfirmRequest request, PaymentType paymentType) {

        if (paymentRepository.findByOrderId(request.orderId()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_PAYMENT);
        }

        //Todo: participation repository 업데이트 후 수정 필요
//        Participation participation = participationRepository.findById(request.participationId())
//                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND));
//
//        if (!request.amount().equals(participation.getFinalAmount())) {
//            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
//        }

        TossConfirmResponse tossResponse = tossPaymentClient.confirm(
                request.paymentKey(),
                request.orderId(),
                request.amount()
        );

        //Todo: participation repository 업데이트 후 수정 필요
        Payment payment = request.toEntity(null, tossResponse.paymentKey(), PaymentStatus.PAID, paymentType);
        Payment savePayment = paymentWriter.save(payment);

        return PaymentResponse.from(savePayment);
    }
}
