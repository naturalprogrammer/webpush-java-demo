package com.naturalprogrammer.demo.webpush;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;

@SpringBootApplication
@RestController
public class WebPushDemoApplication {
	
	public static class WebPushSubscription {
		
		private String notificationEndPoint;
		private String publicKey;
		private String auth;
		
	    public String getNotificationEndPoint() {
			return notificationEndPoint;
		}
		public void setNotificationEndPoint(String notificationEndPoint) {
			this.notificationEndPoint = notificationEndPoint;
		}
		public String getPublicKey() {
			return publicKey;
		}
		public void setPublicKey(String publicKey) {
			this.publicKey = publicKey;
		}
		public String getAuth() {
			return auth;
		}
		public void setAuth(String auth) {
			this.auth = auth;
		}		
	}
	
	public static class WebPushMessage {
		
		public String title;
		public String clickTarget;
		public String message;
	}

	private static PushService pushService = new PushService();
	
	private Map<String, WebPushSubscription> subscriptions = new ConcurrentHashMap<>();
	
	@Autowired
	private ObjectMapper objectMapper;

	public static void main(String[] args) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
		
		  // Add BouncyCastle as an algorithm provider
		  if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
		      Security.addProvider(new BouncyCastleProvider());
		  }
		
		pushService.setPublicKey("BBYCxwATP2vVgw7mMPHJfT6bZrJP2iUV7OP_oxHzEcNFenrX66D8G34CdEmVULNg4WJXfjkeyT0AT9LwavpN8M4=");
		pushService.setPrivateKey("AKYLHgp-aV3kOys9Oy6QgxNI6OGIlOB3G6kjGvhl57j_");
		
		SpringApplication.run(WebPushDemoApplication.class, args);
	}
	
	@PostMapping("/subscribe")
	public void subscribe(WebPushSubscription subscription) {
		
		subscriptions.put(subscription.notificationEndPoint, subscription);
	}
	
	@PostMapping("/unsubscribe")
	public void unsubscribe(WebPushSubscription subscription) {
		
		subscriptions.remove(subscription.notificationEndPoint);
	}
	
	@PostMapping("/notify-all")
	public WebPushMessage notifyAll(@RequestBody WebPushMessage message) throws GeneralSecurityException, IOException, JoseException, ExecutionException, InterruptedException {
		
		for (WebPushSubscription subscription: subscriptions.values()) {
			
			Notification notification = new Notification(
					subscription.getNotificationEndPoint(),
					subscription.getPublicKey(),
					subscription.getAuth(),
					objectMapper.writeValueAsBytes(message));
			
			pushService.send(notification);			
		}
		
		return message;		
	}
}
