package com.naturalprogrammer.demo.webpush;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.jose4j.lang.JoseException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;

@SpringBootApplication
@RestController
public class WebPushDemoApplication {
	
	public static class MySubscription {
		
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
	
	public static class Message {
		
		public String title;
		public String clickTarget;
		public String message;
	}
	
	private static PushService pushService = new PushService();
	
	private Map<String, MySubscription> subscriptions = new ConcurrentHashMap<>();

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
	public void subscribe(MySubscription subscription) {
		
		subscriptions.put(subscription.notificationEndPoint, subscription);
	}
	
	@PostMapping("/unsubscribe")
	public void unsubscribe(MySubscription subscription) {
		
		subscriptions.remove(subscription.notificationEndPoint);
	}
	
	@PostMapping("/notify-all")
	public String notifyAll(@RequestBody String message) throws GeneralSecurityException, IOException, JoseException, ExecutionException, InterruptedException {
		
		for (MySubscription subscription: subscriptions.values()) {
			
			Notification notification = new Notification(
					subscription.getNotificationEndPoint(),
					subscription.getPublicKey(),
					subscription.getAuth(),
					message);
			
			pushService.send(notification);			
		}
		
		return message;		
	}
}
