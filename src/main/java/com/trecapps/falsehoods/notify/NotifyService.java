package com.trecapps.falsehoods.notify;

import com.trecapps.falsehoods.models.FalsehoodDocument;
import com.trecapps.falsehoods.models.RecordEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class NotifyService {

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
        post.setAppId("falsehoods-management");
        post.setMessage(message);
        post.setRelevantId(document.getId().toString());

        if(RecordEvent.SUGGEST.equals(event)){
            post.setRelevantIdSecondary(document.getSuggestId().toString());
        }
        sendNotification(post);
    }
}
