package com.noahalvandi.dbbserver.listener;

import com.noahalvandi.dbbserver.configuration.RabbitMQConfig;
import com.noahalvandi.dbbserver.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReservationExpirationListener {

    private final ReservationService reservationService;

    @RabbitListener(queues = RabbitMQConfig.RESERVATION_QUEUE)
    public void handleReservationExpiration(String reservationIdString) {
        UUID reservationId = UUID.fromString(reservationIdString);
        reservationService.expireAndNotifyNext(reservationId);
    }
}
