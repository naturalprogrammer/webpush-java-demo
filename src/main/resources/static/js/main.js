var url = 'http://localhost:8080/subscribe';

//Vapid public key.
var applicationServerPublicKey = 'BBYCxwATP2vVgw7mMPHJfT6bZrJP2iUV7OP_oxHzEcNFenrX66D8G34CdEmVULNg4WJXfjkeyT0AT9LwavpN8M4=';

var serviceWorkerName = 'sw.js';

var isSubscribed = false;
var swRegistration = null;

$(document).ready(function () {
    $('#btnPushNotifications').click(function (event) {
        if(isSubscribed){
            console.log("Unsubscribing...");
            unsubscribe();
        }else{
            subscribe();
        }
    });
    
    Notification.requestPermission().then(function (status) {
        if (status === 'denied') {
            console.log('[Notification.requestPermission] The user has blocked notifications.');
            disableAndSetBtnMessage('Notification permission denied');
        } else if (status === 'granted') {
            console.log('[Notification.requestPermission] Initializing service worker.');
            initialiseServiceWorker();
        }
    });
});

function initialiseServiceWorker() {
    if ('serviceWorker' in navigator) {
        navigator.serviceWorker.register(serviceWorkerName).then(handleSWRegistration);
    } else {
        console.log('Service workers aren\'t supported in this browser.');
        disableAndSetBtnMessage('Service workers unsupported');
    }
};

function handleSWRegistration(reg) {
    if (reg.installing) {
        console.log('Service worker installing');
    } else if (reg.waiting) {
        console.log('Service worker installed');
    } else if (reg.active) {
        console.log('Service worker active');
    }
    
    swRegistration = reg;
    initialiseState(reg);
}

// Once the service worker is registered set the initial state
function initialiseState(reg) {
    // Are Notifications supported in the service worker?
    if (!(reg.showNotification)) {
        console.log('Notifications aren\'t supported on service workers.');
        disableAndSetBtnMessage('Notifications unsupported');
        return;
    }

    // Check if push messaging is supported
    if (!('PushManager' in window)) {
        console.log('Push messaging isn\'t supported.');
        disableAndSetBtnMessage('Push messaging unsupported');
        return;
    }

    // We need the service worker registration to check for a subscription
    navigator.serviceWorker.ready.then(function (reg) {
        // Do we already have a push message subscription?
        reg.pushManager.getSubscription()
            .then(function (subscription) {
                if (!subscription) {
                    console.log('Not yet subscribed to Push');

                    isSubscribed = false;
                    makeButtonSubscribable();
                } else {
                    // initialize status, which includes setting UI elements for subscribed status
                    // and updating Subscribers list via push
                    isSubscribed = true;
                    makeButtonUnsubscribable();
                }
            })
            .catch(function (err) {
                console.log('Error during getSubscription()', err);
            });
    });
}

function subscribe() {
    navigator.serviceWorker.ready.then(function (reg) {
        var subscribeParams = {userVisibleOnly: true};
        
        //Setting the public key of our VAPID key pair.
        var applicationServerKey = urlB64ToUint8Array(applicationServerPublicKey);
        subscribeParams.applicationServerKey = applicationServerKey;

        reg.pushManager.subscribe(subscribeParams)
            .then(function (subscription) {

                // Update status to subscribe current user on server, and to let
                // other users know this user has subscribed
                var endpoint = subscription.endpoint;
                var key = subscription.getKey('p256dh');
                var auth = subscription.getKey('auth');
                sendSubscriptionToServer(endpoint, key, auth);
                isSubscribed = true;
                makeButtonUnsubscribable();
            })
            .catch(function (e) {
                // A problem occurred with the subscription.
                console.log('Unable to subscribe to push.', e);
            });
    });
}

function unsubscribe() {
    var endpoint = null;
    swRegistration.pushManager.getSubscription()
        .then(function(subscription) {
            if (subscription) {
                endpoint = subscription.endpoint;
                return subscription.unsubscribe();
            }
        })
        .catch(function(error) {
            console.log('Error unsubscribing', error);
        })
        .then(function() {
            removeSubscriptionFromServer(endpoint);

            console.log('User is unsubscribed.');
            isSubscribed = false;

            makeButtonSubscribable(endpoint);
        });
}

function sendSubscriptionToServer(endpoint, key, auth) {
    var encodedKey = btoa(String.fromCharCode.apply(null, new Uint8Array(key)));
    var encodedAuth = btoa(String.fromCharCode.apply(null, new Uint8Array(auth)));
    $.ajax({
        type: 'POST',
        url: url,
        data: {publicKey: encodedKey, auth: encodedAuth, notificationEndPoint: endpoint},
        success: function (response) {
            console.log('Subscribed successfully! ' + JSON.stringify(response));
        },
        dataType: 'json'
    });
}

function removeSubscriptionFromServer(endpoint) {
    $.ajax({
        type: 'POST',
        url: '/unsubscribe',
        data: {notificationEndPoint: endpoint},
        success: function (response) {
            console.log('Unsubscribed successfully! ' + JSON.stringify(response));
        },
        dataType: 'json'
    });
}

function disableAndSetBtnMessage(message) {
    setBtnMessage(message);
    $('#btnPushNotifications').attr('disabled','disabled');
}

function enableAndSetBtnMessage(message) {
    setBtnMessage(message);
    $('#btnPushNotifications').removeAttr('disabled');
}

function makeButtonSubscribable() {
    enableAndSetBtnMessage('Subscribe to push notifications');
    $('#btnPushNotifications').addClass('btn-primary').removeClass('btn-danger');
}

function makeButtonUnsubscribable() {
    enableAndSetBtnMessage('Unsubscribe from push notifications');
    $('#btnPushNotifications').addClass('btn-danger').removeClass('btn-primary');
}

function setBtnMessage(message) {
    $('#btnPushNotifications').text(message);
}

function urlB64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding)
        .replace(/\-/g, '+')
        .replace(/_/g, '/');

    const rawData = window.atob(base64);
    const outputArray = new Uint8Array(rawData.length);

    for (var i = 0; i < rawData.length; ++i) {
        outputArray[i] = rawData.charCodeAt(i);
    }
    return outputArray;
}