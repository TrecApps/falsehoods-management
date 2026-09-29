package com.trecapps.falsehoods.notify;

import com.trecapps.falsehoods.models.FalsehoodDocument;
import com.trecapps.falsehoods.models.RecordEvent;
import com.trecauth.common.model.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
public class NotifyService {

    private static final String APP_NAME = "falsehoods-management";

    IMessageProducer messageProducer;
    @Autowired
    NotifyService(@Autowired(required = false) IMessageProducer producer){
        this.messageProducer = producer;
    }

    private void sendNotification(NotificationPost post){
        messageProducer.sendNotification(post).subscribe();
    }

    public void notifyOnReview(FalsehoodDocument document, RecordEvent event){
        String message = RecordEvent.SUGGEST.equals(event) ?
                String.format("You Falsehood Submission '%s' has a suggestion", document.getTitle()) :
                String.format("Your Falsehood Submission '%s' has been %s", document.getTitle(), event);

        NotificationPost post = new NotificationPost();
        post.setAccountId(document.getCreator());
        post.setAppId(APP_NAME);
        post.setMessage(message);
        post.setRelevantId(document.getId().toString());
        post.setCategory("Review");

        if(RecordEvent.SUGGEST.equals(event)){
            post.setRelevantIdSecondary(document.getSuggestId().toString());
        }
        sendNotification(post);
    }

    public void notifyOnEdit(Collection<Record> records, FalsehoodDocument document){
        List<NotificationPost> posts = records.stream()
                .map((Record record) -> {
                    NotificationPost post = new NotificationPost();

                    String message =
                    switch(record.getType()){
                        case "ACCEPT_OUT" -> String.format("Submission '%s' that you Accepted has been edited!", document.getTitle());
                        case "REJECT_OUT" -> String.format("Submission '%s' that you Rejected has been edited!", document.getTitle());
                        case "SUGGEST" -> String.format("Submission '%s' has been edited since you left a suggestion", document.getTitle());
                        default -> null;
                    };
                    if(message == null)
                        return null;

                    post.setMessage(message);
                    post.setRelevantId(document.getId().toString());
                    post.setAppId(APP_NAME);
                    post.setAccountId(record.getCreator());
                    post.setCategory("Edit");
                    return post;
                })
                .filter(Objects::nonNull)
                .toList();
        Flux.fromIterable(posts)
                .flatMap((NotificationPost post) -> {
                    return this.messageProducer.sendNotification(post);
                }).subscribe();
    }
}
