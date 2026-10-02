
var notifyPollingId = undefined;

var notifyLatestTimestamp = undefined;

var notifications = [];

var notificationsContainerElement;

var notifyUrl;

function markNotification(notifications1, callback, read = false){
    let postBody = {
      notifications1,
      status: read ? "READ" : "UNREAD"
    }
    const params = new URLSearchParams();
    params.append("appId", "falsehoods-management");
    fetch(`${notifyUrl}/mark?${params}`, {
        method: "POST",
        body: JSON.stringify(postBody),
        headers: {
            "Content-Type": "application/json"
        }
    }).then(async (response) => {
        if(response.status == 200){
            callback(await response.json());
        }
    })
}

function hoverOnNotification(notification, onClick = false){
    if((!onClick && notification.status == "UNSEEN") || (onClick && notification.status == "UNREAD")) {
        markNotification([notification.notificationId], onClick, (obj) => {
            if(!obj.id || !obj.id.includes(notification.notificationId))return;

            if(notification.status == "UNSEEN") {
                notification.status = "UNREAD";
            } else if(onClick && notification.status == "UNREAD"){
                notification.status = "READ";
            }
            updateNotificationsContainerElement();
        })
    }
}

function updateNotificationsContainerElement(){
    if(!notificationsContainerElement) return;

    notificationsContainerElement.replaceChildren();

    for(let notification of notifications){
        let notificationItem = document.createElement("div");
        notificationItem.classList.add("element-item");
        notificationItem.classList.add(elementItemSetting);
        notificationItem.addEventListener("mouseover", () => {
            hoverOnNotification(notification, false);
        });
        notificationItem.addEventListener("click", () => {
            hoverOnNotification(notification, true);
            window.location = `${falsehoodServiceUrl}/Falsehood/${notification.post.relevantId}`;
        });

        notificationsContainerElement.appendChild(notificationItem);

        let notifyMessage = document.createElement("div");
        notifyMessage.classList.add("notification");
        notificationItem.appendChild(notifyMessage);

        let notifyP = document.createElement("p");
        notifyP.textContent = notification.post.message;
        notifyMessage.appendChild(notifyP);
    }

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

function notifyPollingFunc(url, callback){
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
        }).then(async (response) => {
            if(response.status != 200){
                console.error("Failed to Receive Notifications!");
                return;
            }
            processNewNotifications(await response.json(), callback);
        }).catch(() => {
            console.error("Failed to send request for Notifications")
        });
}

function prepareNotificationPolling(url, callback){

    notifyUrl = url;

    notifyPollingFunc(notifyUrl, callback);

    notifyPollingId = setInterval(() => {


        notifyPollingFunc(notifyUrl, callback);
    }, 45000);
}

