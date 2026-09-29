package com.trecapps.falsehoods.notify;

import reactor.core.publisher.Mono;

public interface IMessageProducer {

    Mono<Boolean> sendNotification(NotificationPost post);

}
