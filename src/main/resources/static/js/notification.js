
var notifyPollingId = undefined;

var notifyLatestTimestamp = undefined;

var notifications = [];

var notificationsContainerElement;

function updateNotificationsContainerElement(){
    if(!notificationsContainerElement) return;

    // ToDo - Clear the element


    // ToDo - Add new elements per notification
}

function processNewNotifications(newNotes, callback){

    for(let note of newNotes){
        let timeStr = note.post.time.toString();
        note.post.time = new Date(timeStr);
    }

    notifications = newNotes.concat(notifications );
    if(newNotes.length){
        notifyLatestTimestamp = newNotes[0].post.time;
    }
    let count = 0;
    for(let note of notifications){
        if(note.status == 'UNSEEN'){
            count++;
        }
    }
    updateNotificationsContainerElement();
    callback(count);
}

function prepareNotificationPolling(url, callback){

    notifyPollingId = setInterval(() -> {

        const params = new URLSearchParams();
        params.append("appId", "falsehoods-management");
        if(notifyLatestTimestamp){
            params.append("time", notifyLatestTimestamp.toISOString());
        } else {
            params.append("page", 0);
            params.append("size", 10);
        }
        let useUrl = notifyLatestTimestamp ? `${url}/After?${params}` : `${url}?${params}`;
        fetch(useUrl, {
            method: 'GET'
        }).then((response) => {
            if(response.status != 200){
                console.error("Failed to Receive Notifications!");
                return;
            }
            processNewNotifications(await response.json(), callback);
        }).catch(() -> {
            console.error("Failed to send request for Notifications")
        });
    }, 45000);
}